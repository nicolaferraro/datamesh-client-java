package me.nicolaferraro.datamesh.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "datamesh")
public class DataMeshProperties {

    /**
     * Hostname of the DataMesh service
     */
    private String host;

    /**
     * Port of the DataMesh service
     */
    private Integer port;

    public DataMeshProperties() {
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
}
