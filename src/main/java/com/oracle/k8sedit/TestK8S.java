package com.oracle.k8sedit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.oracle.k8sedit.api.API;
import com.oracle.k8sedit.api.APISpec;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class TestK8S {
    public static void main(String[] args) throws IOException {
        File initialFile = new File("src/main/resources/api2.json");
        InputStream targetStream = new FileInputStream(initialFile);
        File initialFile2 = new File("src/main/resources/api.yml");
        InputStream targetStream2 = new FileInputStream(initialFile2);
        ObjectMeta objectMeta = new ObjectMetaBuilder()
                .withName("ola").build();
        APISpec apiSpec = new APISpec("hello", "hello-id", "v1");
        API api = new API(objectMeta, apiSpec);
        List<API> resourcesInAllNamespaces = APIs.getInstance()
                .getResourcesInAllNamespaces();
        DefaultKubernetesClient kubernetesClient = new DefaultKubernetesClient();
        resourcesInAllNamespaces.forEach(System.out::println);
        extracted(kubernetesClient, targetStream);
        extracted(kubernetesClient, targetStream2);
        File initialFile3 = new File("src/main/resources/api3.yml");
        InputStream targetStream3 = new FileInputStream(initialFile3);
        extracted(kubernetesClient, targetStream3);
        kubernetesClient.close();
        //hasMetadata.forEach(System.out::println);
    }

    private static void extracted(final DefaultKubernetesClient kubernetesClient, final InputStream targetStream) {
        final byte[] inputData;
        try {
            inputData = targetStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final var forClient = new ByteArrayInputStream(inputData);
        /*kubernetesClient.close();
        try {
            kubernetesClient.inNamespace("dx-prod").resourceList(List.of(api)).createOrReplace();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                    byteArrayOutputStream);

            objectOutputStream.writeObject(api);

            objectOutputStream.flush();
            objectOutputStream.close();

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                    byteArrayOutputStream.toByteArray());
            kubernetesClient.load(byteArrayInputStream).createOrReplace();

        } catch (Exception e) {
            e.printStackTrace();
        }*/
        List<HasMetadata> hasMetadata = kubernetesClient.load(forClient).get();
        for (HasMetadata metadata : hasMetadata) {
            metadata.getMetadata().setLabels(Map.of("Ola", "adiyeee"));
            List<HasMetadata> orReplace = kubernetesClient.inNamespace("dx-prod").resourceList(metadata).createOrReplace();
            orReplace.forEach(System.out::println);
        }
    }
}
