package me.nicolaferraro.datamesh.client;

import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import me.nicolaferraro.datamesh.client.api.DataMeshConnectionInfo;

public interface DataMeshClientFactory {

    static DataMeshClient create(String host) {
        return create(new DataMeshConnectionInfo(host));
    }

    static DataMeshClient create(String host, int port) {
        return create(new DataMeshConnectionInfo(host, port));
    }

    static DataMeshClient create(DataMeshConnectionInfo info) {
        return new DefaultDataMeshClient(info);
    }

}
