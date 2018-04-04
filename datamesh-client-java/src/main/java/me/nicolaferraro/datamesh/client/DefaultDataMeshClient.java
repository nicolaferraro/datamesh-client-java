package me.nicolaferraro.datamesh.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import me.nicolaferraro.datamesh.client.util.GrpcReactorUtils;
import me.nicolaferraro.datamesh.client.util.JsonUtils;
import me.nicolaferraro.datamesh.protobuf.DataMeshGrpc;
import me.nicolaferraro.datamesh.protobuf.Datamesh;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.MonoProcessor;

import java.util.function.Function;
import java.util.regex.Pattern;

class DefaultDataMeshClient implements DataMeshClient {

    private static final int DEFAULT_PORT = 6543;

    private DataMeshGrpc.DataMeshStub stub;

    private EventProcessor eventProcessor;

    public DefaultDataMeshClient(String host) {
        this(host, DEFAULT_PORT);
    }

    public DefaultDataMeshClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.stub = DataMeshGrpc.newStub(channel);
        this.eventProcessor = new EventProcessor();
    }

    @Override
    public DataMeshProjection readOnlyProjection() {
        return new DefaultDataMeshProjection(this.stub);
    }

    @Override
    public DataMeshProjection projection(DataMeshEvent<?> event) {
        return new DefaultDataMeshProjection(this.stub, event);
    }

    @Override
    public Publisher<Void> pushEvent(Object data, String group, String name, String clientIdentifier, String clientVersion) {
        try {
            byte[] dataBytes = JsonUtils.MAPPER.writeValueAsBytes(data);
            Datamesh.Event event = Datamesh.Event.newBuilder()
                    .setPayload(ByteString.copyFrom(dataBytes))
                    .setGroup(group)
                    .setName(name)
                    .setClientIdentifier(clientIdentifier)
                    .setClientVersion(clientVersion)
                    .build();

            Flux<Void> result = GrpcReactorUtils.<Datamesh.Empty>bridgeCall(obs -> stub.push(event, obs))
                    .filter(none -> false)
                    .cast(Void.class);

            // Trigger fast processing
            DataMeshEvent<?> publicEvent = new DataMeshEvent<>(event.getGroup(), event.getName(),
                    event.getClientIdentifier(), event.getClientVersion(), null, data);
            eventProcessor.enqueue(publicEvent);

            return result;
        } catch (JsonProcessingException e) {
            throw new DataMeshClientException("Cannot serialize object to JSON", e);
        }
    }

    @Override
    public <T> void bind(Pattern group, Pattern name, Class<T> eventClass, Function<Publisher<DataMeshEvent<T>>, Publisher<DataMeshProjection>> processing) {
        eventProcessor.addProcessingFunction(group, name, eventClass, processing);
    }

}
