package me.nicolaferraro.datamesh.test.server;

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

    DefaultDataMeshTestServer(DataMeshTestServerConfiguration configuration) {
        this.configuration = configuration;
    }

    public DataMeshTestServer newTestServer() {
        DataMeshTestServer server = tryRealServer();
        if (server != null) {
            return server;
        }

        return wrap(this::createTestServer);
    }

    private DataMeshTestServer createTestServer() throws IOException {
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

        ProcessBuilder builder = new ProcessBuilder(binary.getAbsolutePath(), "-dir", logDir.getAbsolutePath(), "-port", "" + port, "-logtostderr", "-v", "1", "server");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopProcess(process)));
        startLoggerThread(process);

        wrap(() -> waitForServerStart(port));

        return new DataMeshTestServer() {
            @Override
            public String getHost() {
                return "localhost";
            }

            @Override
            public Integer getPort() {
                return port;
            }

            @Override
            public void stop() {
                stopProcess(process);
            }
        };
    }

    private DataMeshTestServer tryRealServer() {
        if (configuration.getRealHost() != null) {
            return new DataMeshTestServer() {
                @Override
                public String getHost() {
                    return configuration.getRealHost();
                }

                @Override
                public Integer getPort() {
                    return configuration.getRealPort();
                }

                @Override
                public void stop() {
                    // not managed
                }
            };
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
                if (process.isAlive()) {
                    LOG.warn("Error while reading the Data Mesh process log", ex);
                }
            }
        });
        logger.setDaemon(true);
        logger.start();
    }



}
