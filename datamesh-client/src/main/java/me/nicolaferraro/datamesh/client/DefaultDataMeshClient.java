package me.nicolaferraro.datamesh.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import me.nicolaferraro.datamesh.client.api.DataMeshConnectionInfo;
import me.nicolaferraro.datamesh.client.api.DataMeshEvent;
import me.nicolaferraro.datamesh.client.api.DataMeshProjection;
import me.nicolaferraro.datamesh.client.util.GrpcReactorUtils;
import me.nicolaferraro.datamesh.client.util.JsonUtils;
import me.nicolaferraro.datamesh.protobuf.DataMeshGrpc;
import me.nicolaferraro.datamesh.protobuf.Datamesh;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

class DefaultDataMeshClient implements DataMeshClient {

    private DataMeshConnectionInfo connectionInfo;

    private DataMeshGrpc.DataMeshStub stub;

    private EventProcessor eventProcessor;

    private EventQueueConnector connector;

    public DefaultDataMeshClient(DataMeshConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
        ManagedChannel channel = ManagedChannelBuilder.forAddress(connectionInfo.getHost(), connectionInfo.getPort()).usePlaintext().build();
        this.stub = DataMeshGrpc.newStub(channel);
        this.eventProcessor = new EventProcessor();
        this.connector = new EventQueueConnector(connectionInfo, stub, eventProcessor);
    }

    @Override
    public void start() {
        this.eventProcessor.start();
        this.connector.start();
    }

    @Override
    public void stop() {
        this.connector.stop();
        this.eventProcessor.stop();
    }

    @Override
    public DataMeshProjection projection() {
        return new DefaultDataMeshProjection(this.stub);
    }

    @Override
    public Publisher<Void> pushEvent(Object data, String group, String name, String version) {
        try {
            byte[] dataBytes = JsonUtils.MAPPER.writeValueAsBytes(data);
            Datamesh.Event event = Datamesh.Event.newBuilder()
                    .setPayload(ByteString.copyFrom(dataBytes))
                    .setGroup(group)
                    .setName(name)
                    .setClientIdentifier(UUID.randomUUID().toString())
                    .setClientVersion(version)
                    .build();

            Flux<Void> result = GrpcReactorUtils.<Datamesh.Empty>bridgeCall(obs -> stub.push(event, obs))
                    .filter(none -> false)
                    .cast(Void.class);

            // Trigger fast processing
            DefaultDataMeshEvent<?> publicEvent = new DefaultDataMeshEvent<>(stub, event.getGroup(), event.getName(),
                    event.getClientIdentifier(), event.getClientVersion(), null, data);
            eventProcessor.enqueue(publicEvent);

            return result;
        } catch (JsonProcessingException e) {
            return Mono.error(new DataMeshClientException("Cannot serialize object to JSON", e));
        }
    }

    @Override
    public <T> void onEvent(Pattern group, Pattern name, Pattern version, Class<T> eventClass, Function<DataMeshEvent<T>, Publisher<?>> processing) {
        eventProcessor.addProcessingFunction(group, name, version, eventClass, processing);
    }

}
