package me.nicolaferraro.datamesh.client;

import io.grpc.stub.StreamObserver;
import me.nicolaferraro.datamesh.client.util.GrpcReactorUtils;
import me.nicolaferraro.datamesh.protobuf.DataMeshGrpc;
import me.nicolaferraro.datamesh.protobuf.Datamesh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Optional;

class EventQueueConnector {

    private static final Logger LOG = LoggerFactory.getLogger(EventQueueConnector.class);

    private static final long RETRY_PERIOD = 5000L;

    private DataMeshGrpc.DataMeshStub stub;

    private EventProcessor processor;

    private boolean running;

    private Optional<StreamObserver<Datamesh.Disconnect>> disconnectChannel = Optional.empty();

    public EventQueueConnector(DataMeshGrpc.DataMeshStub stub, EventProcessor processor) {
        this.stub = stub;
        this.processor = processor;
    }


    public void start() {
        if (!this.running) {
            this.running = true;
            persistentStream()
                    .map(evt -> new DefaultDataMeshEvent<>(stub, evt.getGroup(), evt.getName(),
                            evt.getClientIdentifier(), evt.getClientVersion(), evt.getVersion(),
                            evt.getPayload().toByteArray()))
                    .doOnNext(devt -> processor.enqueue(devt))
                    .subscribe();
        }
    }

    public void stop() {
        if (this.running) {
            this.running = false;
            if (disconnectChannel.isPresent()) {
                try {
                    disconnectChannel.get().onNext(Datamesh.Disconnect.newBuilder().build());
                    disconnectChannel.get().onCompleted();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
    }

    private Flux<Datamesh.Event> persistentStream() {
        return GrpcReactorUtils.<Datamesh.Event>bridgeCall(obs -> disconnectChannel = Optional.of(stub.connect(obs)))
                .onErrorResume(e -> {
                    if (this.running) {
                        LOG.error("Error while connecting to the DataMesh server", e);
                        return persistentStream().delaySubscription(Duration.ofMillis(RETRY_PERIOD));
                    } else {
                        return Flux.empty();
                    }
                });
    }
}
