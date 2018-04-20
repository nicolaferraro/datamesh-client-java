package me.nicolaferraro.datamesh.test.server;

import me.nicolaferraro.datamesh.client.api.DataMeshAbstractClientFactory;
import me.nicolaferraro.datamesh.client.api.DataMeshClient;

public interface DataMeshTestServer {

    static DataMeshClient newTestServerConnection(DataMeshAbstractClientFactory factory) {
        return DataMeshTestServer.newTestServerConnection(new DataMeshTestServerConfiguration(), factory);
    }

    static DataMeshClient newTestServerConnection(DataMeshTestServerConfiguration configuration, DataMeshAbstractClientFactory factory) {
        return new DefaultDataMeshTestServer(configuration, factory).newServerConnection();
    }

}
