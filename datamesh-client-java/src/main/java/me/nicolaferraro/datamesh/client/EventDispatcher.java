package me.nicolaferraro.datamesh.client;

import me.nicolaferraro.datamesh.client.util.GrpcReactorUtils;
import me.nicolaferraro.datamesh.protobuf.DataMeshGrpc;
import me.nicolaferraro.datamesh.protobuf.Datamesh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;

class EventDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(EventDispatcher.class);

    private static final long RETRY_PERIOD = 5000L;

    private DataMeshGrpc.DataMeshStub stub;

    private EventProcessor processor;

    public EventDispatcher(DataMeshGrpc.DataMeshStub stub, EventProcessor processor) {
        this.stub = stub;
        this.processor = processor;
        this.run();
    }

    private void run() {
        persistentStream()
                .map(evt -> new DefaultDataMeshEvent<>(stub, evt.getGroup(), evt.getName(),
                        evt.getClientIdentifier(), evt.getClientVersion(), evt.getVersion(),
                        evt.getPayload().toByteArray()))
                .doOnNext(devt -> processor.enqueue(devt))
                .subscribe();
    }

    private Flux<Datamesh.Event> persistentStream() {
        return GrpcReactorUtils.<Datamesh.Event>bridgeCall(
                obs -> stub.processQueue(Datamesh.Empty.newBuilder().build(), obs)
        ).onErrorResume(e -> {
            LOG.error("Error while connecting to the DataMesh server", e);
            return persistentStream().delaySubscription(Duration.ofMillis(RETRY_PERIOD));
        });
    }
}
