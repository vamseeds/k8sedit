package com.oracle.k8sedit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

import io.helidon.common.context.Contexts;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;


public class SharedInformerFactoryAndK8sClientSupplier {
    public static final Function<ExecutorService, SharedInformerFactoryAndK8sClientSupplier> DEFAULT_SUPPLIER =
            SharedInformerFactoryAndK8sClientSupplier::new;
    private static Function<ExecutorService, SharedInformerFactoryAndK8sClientSupplier> supplierFunction;
    private static Supplier<KubernetesClient> clientSupplier;
    private static SharedInformerFactoryAndK8sClientSupplier theInstance;
    private static final AtomicLong NEXT_THREAD_ID;
    private final ExecutorService executorService;
    private final SharedInformerFactory sharedInformerFactory;
    private final KubernetesClient k8sClient;

    public static SharedInformerFactoryAndK8sClientSupplier getInstance(ExecutorService executorService) {
        if (theInstance == null) {
            theInstance = (SharedInformerFactoryAndK8sClientSupplier) supplierFunction.apply(executorService);
        }

        return theInstance;
    }

    public static SharedInformerFactoryAndK8sClientSupplier getInstance() {
        if (theInstance == null) {
            ExecutorService executorService = Contexts.wrap(Executors.newCachedThreadPool((r) -> {
                return new Thread(r, "common-utils-core-" + NEXT_THREAD_ID.getAndIncrement());
            }));
            theInstance = getInstance(executorService);
        }

        return theInstance;
    }

    private SharedInformerFactoryAndK8sClientSupplier(ExecutorService executorService) {
        this.executorService = executorService;
        KubernetesClient kubernetesClient = (KubernetesClient) clientSupplier.get();
        this.sharedInformerFactory = kubernetesClient.informers(executorService);
        this.k8sClient = kubernetesClient;
    }

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    public SharedInformerFactory getSharedInformerFactory() {
        return this.sharedInformerFactory;
    }

    public KubernetesClient getK8sClient() {
        return this.k8sClient;
    }

    public static void setSupplierFunction(Function<ExecutorService, SharedInformerFactoryAndK8sClientSupplier> supplierFunction) {
        theInstance = null;
        SharedInformerFactoryAndK8sClientSupplier.supplierFunction = supplierFunction;
    }

    public static void setClientSupplier(Supplier<KubernetesClient> clientSupplier) {
        SharedInformerFactoryAndK8sClientSupplier.clientSupplier = clientSupplier;
    }

    static {
        supplierFunction = DEFAULT_SUPPLIER;
        clientSupplier = DefaultKubernetesClient::new;
        theInstance = null;
        NEXT_THREAD_ID = new AtomicLong(0L);
    }
}
