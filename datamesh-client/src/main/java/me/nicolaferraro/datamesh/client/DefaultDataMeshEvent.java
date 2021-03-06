package me.nicolaferraro.datamesh.client;

import me.nicolaferraro.datamesh.client.api.DataMeshConnectionInfo;
import me.nicolaferraro.datamesh.client.api.DataMeshEvent;
import me.nicolaferraro.datamesh.client.api.DataMeshProjection;
import me.nicolaferraro.datamesh.protobuf.DataMeshGrpc;

import java.util.function.Supplier;

class DefaultDataMeshEvent<T> implements DataMeshEvent<T> {

    private DataMeshConnectionInfo connectionInfo;

    private DataMeshGrpc.DataMeshStub stub;

    private String group;

    private String name;

    private String clientIdentifier;

    private String clientVersion;

    private Long internalVersion;

    private T payload;

    private DefaultDataMeshProjection projection;

    DefaultDataMeshEvent(DataMeshConnectionInfo connectionInfo, DataMeshGrpc.DataMeshStub stub, String group, String name, String clientIdentifier, String clientVersion, Long internalVersion, T payload) {
       this(connectionInfo, stub, group, name, clientIdentifier, clientVersion, internalVersion, payload, null);
    }

    DefaultDataMeshEvent(DataMeshConnectionInfo connectionInfo, DataMeshGrpc.DataMeshStub stub, String group, String name, String clientIdentifier, String clientVersion, Long internalVersion, T payload, DefaultDataMeshProjection projection) {
        this.connectionInfo = connectionInfo;
        this.stub = stub;
        this.group = group;
        this.name = name;
        this.clientIdentifier = clientIdentifier;
        this.clientVersion = clientVersion;
        this.internalVersion = internalVersion;
        this.payload = payload;
        this.projection = projection;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    @Override
    public String getVersion() {
        return clientVersion;
    }

    public Long getInternalVersion() {
        return internalVersion;
    }

    @Override
    public T getPayload() {
        return payload;
    }

    @Override
    public DataMeshProjection projection() {
        if (this.projection == null) {
            this.projection = new DefaultDataMeshProjection(connectionInfo, stub, this, this.internalVersion);
        }
        return this.projection;
    }

    @Override
    public <R> DataMeshEvent<R> withPayload(Supplier<R> supplier) {
        R newPayload = supplier.get();
        return new ProxyDataMeshEvent<T, R>(this) {
            @Override
            public R getPayload() {
                return newPayload;
            }
        };
    }

    @Override
    public String toString() {
        return "DefaultDataMeshEvent{" +
                "group='" + group + '\'' +
                ", name='" + name + '\'' +
                ", clientIdentifier='" + clientIdentifier + '\'' +
                ", clientVersion='" + clientVersion + '\'' +
                '}';
    }
}
