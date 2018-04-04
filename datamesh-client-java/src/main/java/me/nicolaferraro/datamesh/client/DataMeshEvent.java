package me.nicolaferraro.datamesh.client;

public class DataMeshEvent<T> {

    private String group;

    private String name;

    private String clientIdentifier;

    private String clientVersion;

    private Long version;

    private T payload;

    public DataMeshEvent(String group, String name, String clientIdentifier, String clientVersion, Long version, T payload) {
        this.group = group;
        this.name = name;
        this.clientIdentifier = clientIdentifier;
        this.clientVersion = clientVersion;
        this.version = version;
        this.payload = payload;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public Long getVersion() {
        return version;
    }

    public T getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "DataMeshEvent{" +
                "group='" + group + '\'' +
                ", name='" + name + '\'' +
                ", clientIdentifier='" + clientIdentifier + '\'' +
                ", clientVersion='" + clientVersion + '\'' +
                '}';
    }
}
