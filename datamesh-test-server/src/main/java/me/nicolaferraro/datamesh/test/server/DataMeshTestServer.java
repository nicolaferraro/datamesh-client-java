package me.nicolaferraro.datamesh.test.server;

import java.io.Closeable;

public interface DataMeshTestServer {

    static DataMeshTestServer newTestServer() {
        return DataMeshTestServer.newTestServer(new DataMeshTestServerConfiguration());
    }

    static DataMeshTestServer newTestServer(DataMeshTestServerConfiguration configuration) {
        return new DefaultDataMeshTestServer(configuration).newTestServer();
    }

    void stop();

    String getHost();

    Integer getPort();

}
