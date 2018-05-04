package me.nicolaferraro.datamesh.client;

import io.grpc.stub.StreamObserver;
import me.nicolaferraro.datamesh.client.api.DataMeshConnectionInfo;
import me.nicolaferraro.datamesh.client.util.GrpcReactorUtils;
import me.nicolaferraro.datamesh.protobuf.DataMeshGrpc;
import me.nicolaferraro.datamesh.protobuf.Datamesh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

class EventQueueConnector {

    private static final Logger LOG = LoggerFactory.getLogger(EventQueueConnector.class);

    private static final long RETRY_PERIOD = 5000L;

    private static final long PING_PERIOD = 60000L;

    private DataMeshConnectionInfo connectionInfo;

    private DataMeshGrpc.DataMeshStub stub;

    private EventProcessor processor;

    private Disposable pingSender;

    private boolean running;

    private StreamObserver<Datamesh.Status> statusChannel;

    public EventQueueConnector(DataMeshConnectionInfo connectionInfo, DataMeshGrpc.DataMeshStub stub, EventProcessor processor) {
        this.connectionInfo = connectionInfo;
        this.stub = stub;
        this.processor = processor;
    }


    public void start() {
        if (!this.running) {
            this.running = true;
            persistentStream()
                    .map(evt -> new DefaultDataMeshEvent<>(connectionInfo, stub, evt.getGroup(), evt.getName(),
                            evt.getClientIdentifier(), evt.getClientVersion(), evt.getVersion(),
                            evt.getPayload().toByteArray()))
                    .doOnNext(devt -> processor.enqueue(devt))
                    .subscribe();

            this.pingSender = Flux.interval(Duration.ofMillis(PING_PERIOD))
                    .subscribeOn(Schedulers.newSingle("datamesh-ping"))
                    .doOnEach(tick -> {
                        try {
                            StreamObserver<Datamesh.Status> chan = this.statusChannel;
                            if (chan != null) {
                                chan.onNext(Datamesh.Status.newBuilder()
                                        .setPing(Datamesh.Empty.newBuilder()).build());
                            }
                        } catch (Exception ex) {
                            LOG.warn("Cannot ping DataMesh", ex);
                        }
                    }).subscribe();
        }
    }

    public void stop() {
        if (this.running) {
            this.running = false;
            if (statusChannel != null) {
                try {
                    statusChannel.onNext(Datamesh.Status.newBuilder()
                                .setDisconnect(Datamesh.Empty.newBuilder().build())
                            .build());
                    statusChannel.onCompleted();
                    statusChannel = null;
                } catch (Exception ex) {
                    // ignore
                }
            }
            if (pingSender != null) {
                pingSender.dispose();
                pingSender = null;
            }
        }
    }

    private Flux<Datamesh.Event> persistentStream() {
        return GrpcReactorUtils.<Datamesh.Event>bridgeCall(obs -> {
                statusChannel = stub.connect(obs);
                statusChannel.onNext(Datamesh.Status.newBuilder().setConnect(Datamesh.Context.newBuilder()
                            .setName(connectionInfo.getContextName())
                            .setRevision(connectionInfo.getContextRevision())
                        .build()).build());

            })
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
