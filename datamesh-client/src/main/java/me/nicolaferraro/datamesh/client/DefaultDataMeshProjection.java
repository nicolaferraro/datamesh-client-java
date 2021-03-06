package me.nicolaferraro.datamesh.client;

import com.google.protobuf.ByteString;
import me.nicolaferraro.datamesh.client.api.DataMeshConnectionInfo;
import me.nicolaferraro.datamesh.client.api.DataMeshProjection;
import me.nicolaferraro.datamesh.client.util.GrpcReactorUtils;
import me.nicolaferraro.datamesh.client.util.JsonUtils;
import me.nicolaferraro.datamesh.protobuf.DataMeshGrpc;
import me.nicolaferraro.datamesh.protobuf.Datamesh;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

class DefaultDataMeshProjection implements DataMeshProjection {

    private DataMeshConnectionInfo connectionInfo;

    private DataMeshGrpc.DataMeshStub stub;

    private Optional<DefaultDataMeshEvent<?>> event;

    private Optional<Long> internalVersion;

    private Queue<Datamesh.Operation> operations;

    private boolean errors;

    public DefaultDataMeshProjection(DataMeshConnectionInfo connectionInfo, DataMeshGrpc.DataMeshStub stub) {
        this(connectionInfo, stub, null, null);
    }

    public DefaultDataMeshProjection(DataMeshConnectionInfo connectionInfo, DataMeshGrpc.DataMeshStub stub, DefaultDataMeshEvent<?> event, Long internalVersion) {
        this.connectionInfo = connectionInfo;
        this.stub = stub;
        this.event = Optional.ofNullable(event);
        this.internalVersion = Optional.ofNullable(internalVersion);
        this.operations = new ArrayBlockingQueue<>(10);
    }

    @Override
    public Mono<Boolean> isReady() {
        Datamesh.Context context = Datamesh.Context.newBuilder()
                .setName(connectionInfo.getContextName())
                .setRevision(connectionInfo.getContextRevision())
                .build();

        Flux<Datamesh.Readiness> flux = GrpcReactorUtils.bridgeCall(obs -> stub.health(context, obs));

        return Mono.from(flux).map(Datamesh.Readiness::getReady);
    }

    @Override
    public <T> Mono<T> read(String path, Class<T> type) {
        Datamesh.Context context = Datamesh.Context.newBuilder()
                .setName(connectionInfo.getContextName())
                .setRevision(connectionInfo.getContextRevision())
                .build();

        Datamesh.Path dmPath = buildPath(path);

        Datamesh.ReadRequest request = Datamesh.ReadRequest.newBuilder()
                .setContext(context)
                .setPath(dmPath)
                .build();

        Flux<Datamesh.Data> flux = GrpcReactorUtils.bridgeCall(obs -> stub.read(request, obs));

        return Mono.from(flux)
                .doOnNext(this::registerRead)
                .flatMap(data -> Mono.justOrEmpty(data.getContent())
                        .flatMap(content -> Mono.justOrEmpty(JsonUtils.unmarshal(content.toByteArray(), type))));
    }

    private void registerRead(Datamesh.Data data) {
        Datamesh.ReadOperation read = Datamesh.ReadOperation.newBuilder()
                .setPath(data.getPath())
                .build();

        Datamesh.Operation operation = Datamesh.Operation.newBuilder()
                .setRead(read)
                .build();

        operations.add(operation);
    }

    @Override
    public Mono<Void> upsert(String path, Object value) {
        if (!this.event.isPresent()) {
            return Mono.error(new UnsupportedOperationException("Cannot change a read-only view"));
        }

        Datamesh.Path dmPath = buildPath(path);

        byte[] marshalled;
        try {
            marshalled = JsonUtils.MAPPER.writeValueAsBytes(value);
        } catch (Throwable t) {
            return Mono.error(t);
        }

        Datamesh.Data data = Datamesh.Data.newBuilder()
                .setPath(dmPath)
                .setContent(ByteString.copyFrom(marshalled))
                .build();

        Datamesh.UpsertOperation upsert = Datamesh.UpsertOperation.newBuilder()
                .setData(data)
                .build();

        Datamesh.Operation operation = Datamesh.Operation.newBuilder()
                .setUpsert(upsert)
                .build();

        operations.add(operation);

        return Mono.empty();
    }

    @Override
    public Mono<Void> delete(String path) {
        if (!this.event.isPresent()) {
            return Mono.error(new UnsupportedOperationException("Cannot change a read-only view"));
        }

        Datamesh.Path dmPath = buildPath(path);

        Datamesh.DeleteOperation delete = Datamesh.DeleteOperation.newBuilder()
                .setPath(dmPath)
                .build();

        Datamesh.Operation operation = Datamesh.Operation.newBuilder()
                .setDelete(delete)
                .build();

        operations.add(operation);

        return Mono.empty();
    }

    @Override
    public boolean hasErrors() {
        return this.errors;
    }

    @Override
    public void setErrors(boolean errors) {
        this.errors = errors;
    }

    @Override
    public Mono<Void> persist() {
        if (!this.event.isPresent()) {
            return Mono.error(new UnsupportedOperationException("Cannot change a read-only view"));
        }

        Datamesh.Event.Builder eventBuilder = Datamesh.Event.newBuilder()
                .setGroup(this.event.get().getGroup())
                .setName(this.event.get().getName())
                .setClientIdentifier(this.event.get().getClientIdentifier())
                .setClientVersion(this.event.get().getVersion());

        if (this.internalVersion.isPresent()) {
            eventBuilder = eventBuilder.setVersion(this.internalVersion.get());
        }

        Datamesh.Context context = Datamesh.Context.newBuilder()
                .setName(this.connectionInfo.getContextName())
                .setRevision(this.connectionInfo.getContextRevision())
                .build();

        Datamesh.Transaction tx = Datamesh.Transaction.newBuilder()
                .setEvent(eventBuilder.build())
                .setContext(context)
                .addAllOperations(this.operations)
                .build();

        Flux<Void> result = GrpcReactorUtils.<Datamesh.Empty>bridgeCall(obs -> stub.process(tx, obs))
                .filter(none -> false)
                .cast(Void.class);

        return Mono.from(result);
    }

    private Datamesh.Path buildPath(String location) {
        Datamesh.Path.Builder dmPath = Datamesh.Path.newBuilder().setLocation(location);
        if (this.internalVersion.isPresent()) {
            dmPath = dmPath.setVersion(this.internalVersion.get());
        }
        return dmPath.build();
    }


}
