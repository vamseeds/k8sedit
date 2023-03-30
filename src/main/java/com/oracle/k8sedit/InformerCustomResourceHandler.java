package com.oracle.k8sedit;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

import java.util.List;

/**
 * Resource handler for custom resources using {@link io.fabric8.kubernetes.client.informers.SharedInformer}
 *
 * @param <T> the binding class of the custom resource
 * @param <L> the binding list class of the custom resource
 */
public class InformerCustomResourceHandler<
        T extends CustomResource<?, ?>,
        L extends CustomResourceList<T>> extends InformerResourceHandler<T, L, Resource<T>> {

    /**
     * Builds a new {@link InformerResourceHandler}
     *
     * @param k8sClient the {@link KubernetesClient} to use
     * @param kind      the custom resource's kind
     * @param crdClass  the binding class of the custom resource
     * @param listClass the binding list class of the custom resource
     */
    public InformerCustomResourceHandler(final KubernetesClient k8sClient,
                                         final String kind,
                                         final Class<T> crdClass,
                                         final Class<L> listClass) {
        super(crdClass,
              sharedInformerFactory -> sharedInformerFactory.sharedIndexInformerFor(crdClass, INFORMER_RE_SYNC_PERIOD_MILLIS));

        KubernetesDeserializer.registerCustomKind(kind, crdClass);

        resourceClientSupplier = () -> k8sClient.resources(crdClass, listClass);
    }

    /**
     * Returns all resources of the resource handler's managed type
     *
     * @return the found resources or an empty List
     * @deprecated Use {@link InformerResourceHandler#getResources(String)} instead
     */
    @Deprecated(forRemoval = true)
    public List<T> getCustomResources() {
        return super.getResources();
    }

    /**
     * Returns all resources of the resource handler's managed type in the provided namespace
     *
     * @param namespace the name of the namespace
     * @return the found resources or an empty List
     * @deprecated Use {@link InformerResourceHandler#getResources(String)} instead
     */
    @Deprecated(forRemoval = true)
    public List<T> getCustomResources(final String namespace) {
        return getResources(namespace);
    }

    /**
     * Returns the named resource
     *
     * @param name the name of the resource
     * @return the resource, if found
     * @deprecated Use {@link InformerResourceHandler#getResourceByName(String)} instead
     */
    @Deprecated(forRemoval = true)
    public T getCustomResourceByName(final String name) {
        return super.getResourceByName(name);
    }

    /**
     * Returns the named resource from the provided namespace
     *
     * @param name      the name of the resource
     * @param namespace the namespace of the resource
     * @return the resource, if found
     * @deprecated Use {@link InformerResourceHandler#getResourceByName(String, String)} instead
     */
    @Deprecated(forRemoval = true)
    public T getCustomResourceByName(final String name, final String namespace) {
        return super.getResourceByName(name, namespace);
    }
}
