package com.oracle.k8sedit;

import com.google.common.flogger.FluentLogger;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceList;


import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public abstract class AbstractCRDRepository<S, T extends  CustomResource<S, Void>,
        L extends  CustomResourceList<T>> {
    private static final FluentLogger log = FluentLogger.forEnclosingClass();

    protected InformerCustomResourceHandler<T, L> resourceHandler;
    protected final Class<T> resourceClass;

    private boolean syncStarted = false;

    protected AbstractCRDRepository(final Class<T> resourceClass,
                                    final Class<L> listClass,
                                    final String resourceKind) {
        final var factoryAndK8sClientSupplier =
                SharedInformerFactoryAndK8sClientSupplier.getInstance();

        // Set shared informer factory
        InformerResourceHandler.setSharedInformerFactory(factoryAndK8sClientSupplier.getSharedInformerFactory());

        resourceHandler = new InformerCustomResourceHandler<>(factoryAndK8sClientSupplier.getK8sClient(),
                                                                                                                         resourceKind,
                                                                                                                         resourceClass,
                                                                                                                         listClass);
        this.resourceClass = resourceClass;
    }

    public CompletableFuture<Void> startSync() {
        startSyncIfNeeded();

        return CompletableFuture.runAsync(resourceHandler::waitForSync,
                                          SharedInformerFactoryAndK8sClientSupplier.getInstance().getExecutorService());
    }

    protected void startSyncIfNeeded() {
        if (!syncStarted) {
            final var executorService = SharedInformerFactoryAndK8sClientSupplier.getInstance().getExecutorService();
            final var informer = resourceHandler.getResourceInformer();
            executorService.submit(() -> {
                try {
                    informer.run();
                } catch (Throwable t) {
                    final var msg = String.format("Error starting informer for %s", resourceClass.getSimpleName());
                    log.atSevere().withCause(t).log(msg);
                }
            });

            syncStarted = true;
            log.atInfo().log("%s sync started", resourceClass.getSimpleName());
        }
    }

    public Optional<T> getResourceInNamespace(final String resourceName, final String namespace) {
        final  T resourceByName = resourceHandler.getResourceByName(resourceName, namespace);
        if (null == resourceByName) {
            return Optional.empty();
        } else {
            return Optional.of(resourceByName);
        }
    }

    public List<T> getResourcesInNamespace(final String namespace) {
        return resourceHandler.getResources(namespace);
    }

    public List<T> getResourcesInAllNamespaces() {
        return resourceHandler.getResources();
    }

    public int getResourceCount() {
        return resourceHandler.getResources().size();
    }

    public String lastSync() {
        return resourceHandler.getResourceInformer().lastSyncResourceVersion();
    }

    public boolean isHealthy() {
        final var resourceInformer = resourceHandler.getResourceInformer();
        return resourceInformer.isRunning() && resourceInformer.hasSynced() && resourceInformer.isWatching();
    }

    public boolean isReady() {
        return isHealthy();
    }

    public String getSimpleResourceName() {
        return resourceClass.getSimpleName();
    }



    public T createResourceInNamespace(final T resource, final String namespace) {
        try {
            if(namespace!=null)
            {
                resource.getMetadata().setNamespace(namespace);
            }
            final var createResource = resourceHandler.getResourceClient()
                    .inNamespace(namespace)
                    .create(resource);
            log.atFine().log("Create Resource=%s", createResource);
            return createResource;
        }catch (Exception e)
        {
            log.atSevere().log("Exception Caught Updating : %s, printing StackTrace", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }



    @SuppressWarnings("unchecked")
    public T updateResourceInNamespace(final T resource, final String namespace) {
        try {
            var metadata = getMetadata(resource);
            metadata.setNamespace(namespace);

            final var updateResource = resourceHandler
                    .getResourceClient().inNamespace(namespace).createOrReplace(resource);
            log.atFine().log("Update Resource=%s", updateResource);
            return updateResource;
        }catch (Exception e)
        {
            log.atSevere().log("Exception Caught Updating : %s, printing StackTrace", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Replace the resource in the  namespace
     *
     * @param resource
     * @return
     */
    @SuppressWarnings("unchecked")
    public T replaceResourceInNamespace(final T resource, final String namespace) {
        try {
            var metadata = getMetadata(resource);
            metadata.setNamespace(namespace);

            final var updateResource = resourceHandler
                    .getResourceClient().inNamespace(namespace).replace(resource);
            log.atFine().log("Update Resource=%s", updateResource);
            return updateResource;
        }catch (Exception e)
        {
            log.atSevere().log("Exception Caught Updating : %s, printing StackTrace", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }










    /**
     * Delete the resource in the  namespace
     *
     * @param name
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean deleteResourceInNamespace(final String name, final String namespace ) {

        startSyncIfNeeded();
        final var resource = resourceHandler.getResourceByName(name, namespace);
        if (resource == null) {
            return false;
        } else {
            return deleteResourceInNamespace(resource,namespace);
        }
    }

    /**
     * Delete the resource in the  namespace
     *
     * @param resource resource to be Delete
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean deleteResourceInNamespace(final T resource, final String namespace) {
        try {
            return resourceHandler.getResourceClient().inNamespace(namespace).delete(resource);
        }catch (Exception e)
        {
            log.atSevere().log("Exception Caught Deleting : %s in namespace %s, printing StackTrace", e.getMessage(),namespace);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ObjectMeta getMetadata(final CustomResource<S, Void> resource) {
        var metadata = resource.getMetadata();
        if (metadata == null) {
            metadata = new ObjectMeta();
            resource.setMetadata(metadata);
        }
        return metadata;
    }

    public String convertStringToDNS1123(final String str, final Class<T> clazz) {
        var dns1123 = str.toLowerCase()
                .replace(" ", "-")
                .replaceAll("[^a-z0-9-.]", "");

        final String shortType = shortType(clazz);
        if (dns1123.isEmpty()) {
            return shortType;
        }

        if (isSeparator(dns1123.charAt(0))) {
            dns1123 = shortType + dns1123;
        }

        final var builder = new StringBuilder();
        var separatorInvalid = false;
        for (int i = 0; i < dns1123.length(); i++) {
            char c = dns1123.charAt(i);
            if (!separatorInvalid || !isSeparator(c)) {
                builder.append(c);
            }
            separatorInvalid = isSeparator(c);
        }

        if (separatorInvalid) {
            builder.append(shortType);
        }

        return builder.toString();
    }

    private String shortType(Class<T> clazz) {
        String name = clazz.getName();
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isAlphabetic(c) && Character.isUpperCase(c)) {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }

    private boolean isSeparator(char c) {
        return (c == '-' || c == '.');
    }

    // For unit testing
    public void setResourceHandler(InformerCustomResourceHandler<T, L> resourceHandler) {
        this.resourceHandler = resourceHandler;
    }
}
