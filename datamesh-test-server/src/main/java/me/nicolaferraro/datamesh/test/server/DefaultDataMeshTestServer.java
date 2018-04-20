package me.nicolaferraro.datamesh.test.server;

import me.nicolaferraro.datamesh.client.api.DataMeshAbstractClientFactory;
import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static me.nicolaferraro.datamesh.test.server.FunctionalUtils.ensure;
import static me.nicolaferraro.datamesh.test.server.FunctionalUtils.wrap;

class DefaultDataMeshTestServer {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDataMeshTestServer.class);

    private DataMeshTestServerConfiguration configuration;

    private DataMeshAbstractClientFactory factory;

    DefaultDataMeshTestServer(DataMeshTestServerConfiguration configuration, DataMeshAbstractClientFactory factory) {
        this.configuration = configuration;
        this.factory = factory;
    }

    public DataMeshClient newServerConnection() {
        DataMeshClient client = tryRealServer();
        if (client != null) {
            return client;
        }

        return wrap(this::connectTestProcess);
    }

    private DataMeshClient connectTestProcess() throws IOException {
        File dataDir = new File(configuration.getDataDirectory());
        if (!dataDir.exists()) {
            ensure(dataDir::mkdirs);
        }

        File logDir;
        do {
            long timestamp = System.currentTimeMillis();
            logDir = new File(dataDir, String.valueOf(timestamp));
            if (logDir.exists()) {
                wrap(() -> Thread.sleep(1));
            }
        } while (logDir.exists());
        ensure(logDir::mkdir);

        File binary = new File(dataDir, "datamesh");
        if (!binary.exists()) {
            ensure(binary::createNewFile);
            ensure(() -> binary.setExecutable(true));
            try (InputStream in = getClass().getResourceAsStream("/datamesh"); FileOutputStream out = new FileOutputStream(binary)) {
                IOUtils.copy(in, out);
            }
        }

        int port = getFreePort();

        ProcessBuilder builder = new ProcessBuilder(binary.getAbsolutePath(), "-dir", logDir.getAbsolutePath(), "-port", "" + port, "-logtostderr", "server");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopProcess(process)));
        startLoggerThread(process);

        wrap(() -> waitForServerStart(port));

        return factory.create("localhost", port);
    }

    private DataMeshClient tryRealServer() {
        if (configuration.getRealHost() != null) {
            Integer port = configuration.getRealPort();
            return factory.create(configuration.getRealHost(), port);
        }

        return null;
    }

    private int getFreePort() {
        try {
            ServerSocket ss = new ServerSocket(0);
            int port = ss.getLocalPort();

            ss.close();
            do {
                wrap(() -> Thread.sleep(1));
            } while (!ss.isClosed());

            return port;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot find available port", e);
        }
    }

    private void waitForServerStart(int port) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + 5000) {
            try (Socket socket = new Socket("localhost", port)) {
                if (socket.isConnected()) {
                    return;
                }
            } catch (IOException e) {
                // Sleep and retry
            }

            Thread.sleep(50);
        }
    }

    private void stopProcess(Process process) {
        process.destroy();
        try {
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    private void startLoggerThread(Process process) {
        Thread logger = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.info("[DataMesh Server] " + line);
                }
            } catch (Exception ex) {
                LOG.error("Error while reading the Data Mesh process log", ex);
            }
        });
        logger.setDaemon(true);
        logger.start();
    }



}
