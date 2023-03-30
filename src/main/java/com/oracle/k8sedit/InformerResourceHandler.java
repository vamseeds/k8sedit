package com.oracle.k8sedit;

import com.google.common.flogger.FluentLogger;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerEventListener;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.kubernetes.client.informers.cache.Lister;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A resource handler for Kubernetes resources, based on shared informers. If you need a support for custom resources, use
 * {@link InformerCustomResourceHandler} instead.
 *
 * @param <T> the binding class of the resource. Needs to only implement {@link HasMetadata}
 * @param <L> the binding list class for the resource
 */
public class InformerResourceHandler<T extends HasMetadata, L extends KubernetesResourceList<T>, R extends Resource<T>> {
    protected static final long SYNC_WAIT_TIME_SEC = 15;
    protected static final long SYNC_POLLING_MILLIS = 100;
    private static final ExecutorService SHARED_THREAD_POOL_EXECUTOR = Executors.newCachedThreadPool();
    private static final FluentLogger log = FluentLogger.forEnclosingClass();
    private static Executor timeoutExecutor = CompletableFuture.delayedExecutor(SYNC_WAIT_TIME_SEC, TimeUnit.SECONDS);
    private static final Executor NEXT_TICK_EXECUTOR = CompletableFuture.delayedExecutor(SYNC_POLLING_MILLIS,
                                                                                         TimeUnit.MILLISECONDS,
                                                                                         SHARED_THREAD_POOL_EXECUTOR);

    /**
     * Action to be performed if the informer fails to sync or fails to establish watch
     */
    private static Runnable informerFailureAction = new DefaultInformerFailureAction();

    protected static SharedInformerFactory sharedInformerFactory;
    protected Supplier<MixedOperation<T, L, R>> resourceClientSupplier = null;

    protected static final long INFORMER_RE_SYNC_PERIOD_MILLIS = 60 * 60 * 1000L;

    private final Class<T> clazz;
    protected final SharedIndexInformer<T> resourceInformer;

    /**
     * New constructor taking the resource {@link Class} and a {@link Function} to create the {@link SharedIndexInformer}
     *
     * @param clazz                 the resource class
     * @param indexInformerSupplier the {@link Function} that derives a {@link SharedIndexInformer} from a
     * {@link SharedInformerFactory}.
     */
    public InformerResourceHandler(final Class<T> clazz, final Function<SharedInformerFactory,
            SharedIndexInformer<T>> indexInformerSupplier) {
        this.clazz = clazz;
        if (sharedInformerFactory == null) {
            throw new IllegalStateException(
                    "SharedInformerFactory is null. Set InformerResourceHandler#sharedInformerFactory before creating handlers");
        }
        resourceInformer = indexInformerSupplier.apply(sharedInformerFactory);
    }

    /**
     * Returns the resource client as {@link MixedOperation} instance. Required for direct access to resources, especially for 
     * modifications
     *
     * @return the resource client.
     * @throws NullPointerException in case the resource client isn't set
     */
    public MixedOperation<T, L, R> getResourceClient() {
        if (resourceClientSupplier == null) {
            throw new NullPointerException(String.format("resourceClientSupplier on %s was null when it shouldn't",
                                                         this.getClass().getCanonicalName()));
        }
        return resourceClientSupplier.get();
    }

    /**
     * Registers an ADDED/MODIFIED/DELETED event handler.
     *
     * @param handler the handler instance
     */
    public void addEventHandler(final ResourceEventHandler<T> handler) {
        log.atInfo().log("Adding event handler for type %s", clazz.getCanonicalName());
        resourceInformer.addEventHandler(handler);
    }

    /**
     * Registers an exception handler
     *
     * @param event the exception handler
     */
    public static void addSharedInformerEventListener(final SharedInformerEventListener event) {
        sharedInformerFactory.addSharedInformerEventListener(event);
    }

    public static void setSharedInformerFactory(final SharedInformerFactory newSharedInformerFactory) {
        sharedInformerFactory = newSharedInformerFactory;
    }

    /**
     * Get all resources of the particular kind in all namespaces
     *
     * @return the List of resources
     */
    public List<T> getResources() {
        preCheck();

        final var indexer = resourceInformer.getIndexer();
        final var lister = new Lister<>(indexer);
        return lister.list();
    }

    /**
     * Get all resources of the particular kind in a specific namespaces
     *
     * @param namespace the namespace to filter for
     * @return the List of resources
     */
    public List<T> getResources(final String namespace) {
        preCheck();

        final var indexer = resourceInformer.getIndexer();
        final var lister = new Lister<>(indexer, namespace);
        return lister.list();
    }

    /**
     * Gets a named resource from the namespace the client is connected to. Discouraged from being used.
     *
     * @param name the name of the resource
     * @return the found resource or null
     */
    public T getResourceByName(final String name) {
        preCheck();

        final var indexer = resourceInformer.getIndexer();
        final var lister = new Lister<>(indexer);
        return lister.get(name);
    }

    /**
     * Gets a named resource from the provided namespace.
     *
     * @param name      the name of the resource
     * @param namespace the namespace to look into
     * @return the found resource or null
     */
    public T getResourceByName(final String name, final String namespace) {
        preCheck();

        final var indexer = resourceInformer.getIndexer();
        final var lister = new Lister<>(indexer, namespace);
        return lister.get(name);
    }

    /**
     * Returns the {@link SharedIndexInformer}
     *
     * @return the {@link SharedIndexInformer}
     */
    public SharedIndexInformer<T> getResourceInformer() {
        return resourceInformer;
    }

    /**
     * Returns the {@link SharedInformerFactory}. Mainly to be used to determine whether there are informers registered
     *
     * @return the {@link SharedInformerFactory}
     */
    public SharedInformerFactory getSharedInformerFactory() {
        return sharedInformerFactory;
    }

    private void preCheck() {
        if (!resourceInformer.hasSynced()) {
            waitForSync();
        }

        if (!resourceInformer.isWatching()) {
            establishWatch();
        }
    }

    public void waitForSync() {
        final var resourceName = clazz.getName();

        final var timeout = CompletableFuture.runAsync(
                () -> {
                    // nothing to be done here
                }, timeoutExecutor);
        try {
            while (!resourceInformer.hasSynced()) {
                if (timeout.isDone()) {
                    throw new KubernetesClientException(String.format("Waited too long for %s informer to synchronize.",
                                                                      resourceName));
                }
                final var tick = CompletableFuture.runAsync(() -> {
                }, NEXT_TICK_EXECUTOR);
                tick.join();
            }
            log.atInfo().log(String.format("Sync for type %s completed", resourceName));
        } catch (final KubernetesClientException e) {
            log.atSevere().withCause(e).log(String.format("Could not sync the informer for type %s", resourceName));
            informerFailureAction.run();
            throw e;
        }
    }

    public void establishWatch() {
        final var resourceName = clazz.getName();

        final var timeout = CompletableFuture.runAsync(
                () -> {
                    // nothing to be done here
                }, timeoutExecutor);
        try {
            while (!resourceInformer.isWatching()) {
                if (timeout.isDone()) {
                    throw new KubernetesClientException(String.format("Waited too long for %s informer to establish watch.",
                                                                      resourceName));
                }
                final var tick = CompletableFuture.runAsync(() -> {
                }, NEXT_TICK_EXECUTOR);
                tick.join();
            }
            log.atInfo().log(String.format("Watch established for type %s.", resourceName));
        } catch (final KubernetesClientException e) {
            log.atSevere().withCause(e).log(String.format("Could not establish watch for informer of type %s", resourceName));
            informerFailureAction.run();
            throw e;
        }
    }

    /**
     * Setter for the timeoutExecutor to ensure we can unit test without to long waiting or to allow
     * tuning the max timeout with using projects
     *
     * @param timeoutExecutor the new delayed executor to use
     */
    public static void setTimeoutExecutor(final Executor timeoutExecutor) {
        InformerResourceHandler.timeoutExecutor = timeoutExecutor;
    }

    /**
     * @param informerFailureAction action to be performed if the informer fails to sync or fails to establish watch
     */
    public static void setInformerFailureAction(Runnable informerFailureAction) {
        InformerResourceHandler.informerFailureAction = informerFailureAction;
    }
}
