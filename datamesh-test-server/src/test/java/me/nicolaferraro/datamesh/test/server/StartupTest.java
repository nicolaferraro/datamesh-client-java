package me.nicolaferraro.datamesh.test.server;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;

public class StartupTest {

    @Test(expected = IOException.class)
    public void testServerStartup() throws IOException {
        MockDataMeshClient client = (MockDataMeshClient) DataMeshTestServer.newTestServerConnection(MockDataMeshClient::new);
        try (ServerSocket ss = new ServerSocket(client.getPort())) {
            Assert.fail("The port should be busy");
        }
    }

}
