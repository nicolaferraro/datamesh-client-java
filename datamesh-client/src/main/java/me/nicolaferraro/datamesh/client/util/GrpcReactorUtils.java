package me.nicolaferraro.datamesh.client.util;

import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.publisher.UnicastProcessor;

import java.util.function.Consumer;

public class GrpcReactorUtils {

    public static <T> Flux<T> bridgeCall(Consumer<StreamObserver<T>> call) {
        UnicastProcessor<T> processor = UnicastProcessor.create();

        ReplayProcessor<T> result = ReplayProcessor.create(1, true);
        processor.subscribe(result);
        FluxSink<T> sink = processor.sink();
        try {
            call.accept(bridgeStreamObserver(sink));
        } catch (Exception ex) {
            return Flux.error(ex);
        }

        return result;
    }

    private static <T> StreamObserver<T> bridgeStreamObserver(FluxSink<T> sink) {
        return new StreamObserver<T>() {
            @Override
            public void onNext(T next) {
                sink.next(next);
            }

            @Override
            public void onError(Throwable throwable) {
                sink.error(throwable);
            }

            @Override
            public void onCompleted() {
                sink.complete();
            }
        };
    }

}
