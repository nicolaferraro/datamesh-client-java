package me.nicolaferraro.datamesh.client.api;

import java.util.Objects;

public class DataMeshConnectionInfo {

    public static final String DEFAULT_CONTEXT_NAME = "default";

    public static final long DEFAULT_CONTEXT_REVISION = 1L;

    public static final int DEFAULT_PORT = 6543;

    private String host;

    private Integer port;

    private String contextName;

    private Long contextRevision;

    public DataMeshConnectionInfo(String host) {
        this(host, DEFAULT_PORT, DEFAULT_CONTEXT_NAME, DEFAULT_CONTEXT_REVISION);
    }

    public DataMeshConnectionInfo(String host, Integer port) {
        this(host, port, DEFAULT_CONTEXT_NAME, DEFAULT_CONTEXT_REVISION);
    }

    public DataMeshConnectionInfo(String host, Integer port, String contextName, Long contextRevision) {
        this.host = Objects.requireNonNull(host, "host cannot be null");
        this.port = port != null ? port : DEFAULT_PORT;
        this.contextName = contextName != null ? contextName : DEFAULT_CONTEXT_NAME;
        this.contextRevision = contextRevision != null ? contextRevision : DEFAULT_CONTEXT_REVISION;
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
        this.port = port != null ? port : DEFAULT_PORT;
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName != null ? contextName : DEFAULT_CONTEXT_NAME;
    }

    public Long getContextRevision() {
        return contextRevision;
    }

    public void setContextRevision(Long contextRevision) {
        this.contextRevision = contextRevision != null ? contextRevision : DEFAULT_CONTEXT_REVISION;
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
