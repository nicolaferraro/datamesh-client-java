package me.nicolaferraro.datamesh.client.api;

public class DataMeshConnectionInfo {

    public static final String DEFAULT_CONTEXT_NAME = "default";

    public static final long DEFAULT_CONTEXT_REVISION = 1L;

    public static final int DEFAULT_PORT = 6543;

    private String host;

    private Integer port = DEFAULT_PORT;

    private String contextName = DEFAULT_CONTEXT_NAME;

    private Long contextRevision = DEFAULT_CONTEXT_REVISION;

    public DataMeshConnectionInfo(String host) {
        this.host = host;
    }

    public DataMeshConnectionInfo(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public DataMeshConnectionInfo(String host, Integer port, String contextName, Long contextRevision) {
        this.host = host;
        this.port = port;
        this.contextName = contextName;
        this.contextRevision = contextRevision;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public Long getContextRevision() {
        return contextRevision;
    }

    public void setContextRevision(Long contextRevision) {
        this.contextRevision = contextRevision;
    }

    @Override
    public String toString() {
        return "DataMeshConnectionInfo{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", contextName='" + contextName + '\'' +
                ", contextRevision=" + contextRevision +
                '}';
    }
}
