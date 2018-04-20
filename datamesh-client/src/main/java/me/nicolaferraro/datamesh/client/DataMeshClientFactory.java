package me.nicolaferraro.datamesh.client;

import me.nicolaferraro.datamesh.client.api.DataMeshAbstractClientFactory;
import me.nicolaferraro.datamesh.client.api.DataMeshClient;

public interface DataMeshClientFactory {

    static DataMeshClient create(String host) {
        return new DefaultDataMeshClient(host);
    }

    static DataMeshClient create(String host, int port) {
        return new DefaultDataMeshClient(host, port);
    }

    static DataMeshAbstractClientFactory instance() {
        return new DataMeshAbstractClientFactory() {
            @Override
            public DataMeshClient create(String host, Integer port) {
                if (port != null) {
                    return DataMeshClientFactory.create(host, port);
                } else {
                    return DataMeshClientFactory.create(host);
                }
            }
        };
    }

}
