package me.nicolaferraro.datamesh.test.server;

public class DataMeshTestServerConfiguration {

    public static final String DATAMESH_REAL_TEST_SERVER_HOST_PROPERTY = "datamesh.real.test.server.host";
    public static final String DATAMESH_REAL_TEST_SERVER_PORT_PROPERTY = "datamesh.real.test.server.port";
    public static final String DEFAULT_DATA_DIR = "target/datamesh";

    private String realHost;

    private Integer realPort;

    private String dataDirectory;

    public DataMeshTestServerConfiguration() {
    }

    public String getRealHost() {
        if (realHost != null) {
            return realHost;
        }
        return System.getProperty(DATAMESH_REAL_TEST_SERVER_HOST_PROPERTY);
    }

    public void setRealHost(String realHost) {
        this.realHost = realHost;
    }

    public Integer getRealPort() {
        if (realPort != null) {
            return realPort;
        }
        String port = System.getProperty(DATAMESH_REAL_TEST_SERVER_PORT_PROPERTY);
        if (port != null) {
            return Integer.parseInt(port);
        }
        return null;
    }

    public void setRealPort(Integer realPort) {
        this.realPort = realPort;
    }

    public String getDataDirectory() {
        if (dataDirectory != null) {
            return dataDirectory;
        }
        return DEFAULT_DATA_DIR;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }
}
