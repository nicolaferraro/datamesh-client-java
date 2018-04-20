package me.nicolaferraro.datamesh.test.server;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static org.junit.Assert.assertEquals;

public class StartupTest {

    @Test(expected = IOException.class)
    public void testServerStartup() throws IOException {
        DataMeshTestServer server = DataMeshTestServer.newTestServer();
        assertEquals("localhost", server.getHost());
        try (ServerSocket ss = new ServerSocket(server.getPort())) {
            Assert.fail("The port should be busy");
        }
        server.stop();
    }

}
