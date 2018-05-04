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

    /**
     * Context configuration for DataMesh
     */
    private DataMeshContextProperties context = new DataMeshContextProperties();

    /**
     * Health configuration for DataMesh
     */
    private DataMeshHealthProperties health = new DataMeshHealthProperties();

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

    public DataMeshHealthProperties getHealth() {
        return health;
    }

    public void setHealth(DataMeshHealthProperties health) {
        this.health = health;
    }

    public DataMeshContextProperties getContext() {
        return context;
    }

    public void setContext(DataMeshContextProperties context) {
        this.context = context;
    }

    public static class DataMeshHealthProperties {

        /**
         * Enable actuator health checks for DataMesh
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class DataMeshContextProperties {

        /**
         * The DataMesh context name to use for projections
         */
        private String name;

        /*
         * The DataMesh context revision to use for projections
         */
        private Long revision;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getRevision() {
            return revision;
        }

        public void setRevision(Long revision) {
            this.revision = revision;
        }
    }

}
