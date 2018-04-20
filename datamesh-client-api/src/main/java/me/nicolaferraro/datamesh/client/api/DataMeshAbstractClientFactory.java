package me.nicolaferraro.datamesh.client.api;

@FunctionalInterface
public interface DataMeshAbstractClientFactory {

    DataMeshClient create(String host, Integer port);

}
