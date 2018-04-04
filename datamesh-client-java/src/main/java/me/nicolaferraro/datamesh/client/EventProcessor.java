package me.nicolaferraro.datamesh.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.nicolaferraro.datamesh.client.util.GrpcReactorUtils;
import me.nicolaferraro.datamesh.client.util.JsonUtils;
import me.nicolaferraro.datamesh.protobuf.DataMeshGrpc;
import me.nicolaferraro.datamesh.protobuf.Datamesh;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

class EventProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(EventProcessor.class);

    private FluxSink<DataMeshEvent<?>> sink;

    private Function<DataMeshEvent<?>, Publisher<DataMeshProjection>> processors;

    public EventProcessor() {
        UnicastProcessor<DataMeshEvent<?>> flux = UnicastProcessor.create();
        this.sink = flux.sink();

        this.processors = evt -> Flux.empty();

        flux.flatMap(evt -> this.processors.apply(evt))
                .flatMap(DataMeshProjection::persist)
                .onErrorResume(e -> {
                    LOG.error("Cannot persist projection", e);
                    return Mono.empty();
                })
                .subscribe();
    }

    public void enqueue(DataMeshEvent<?> event) {
        sink.next(event);
    }

    public <T> void addProcessingFunction(Pattern group, Pattern name, Class<T> eventClass, Function<Publisher<DataMeshEvent<T>>, Publisher<DataMeshProjection>> processing) {
        Function<DataMeshEvent<?>, Publisher<DataMeshProjection>> processor = processor(group, name, eventClass, processing);

        Function<DataMeshEvent<?>, Publisher<DataMeshProjection>> currentProcessors = this.processors;
        this.processors = evt -> Mono.from(Flux.merge(currentProcessors.apply(evt), processor.apply(evt)));
    }

    private <T> Function<DataMeshEvent<?>, Publisher<DataMeshProjection>> processor(Pattern group, Pattern name, Class<T> eventClass, Function<Publisher<DataMeshEvent<T>>, Publisher<DataMeshProjection>> processing) {
        return evt -> {
            if (!group.asPredicate().test(evt.getGroup())) {
                return Flux.empty();
            }
            if (!name.asPredicate().test(evt.getName())) {
                return Flux.empty();
            }

            Optional<DataMeshEvent<T>> convertedEvent = convertEventPayload(evt, eventClass);
            if (!convertedEvent.isPresent()) {
                return Flux.empty();
            }

            Publisher<DataMeshEvent<T>> stream = Mono.just(convertedEvent.get());
            return processing.apply(stream);
        };
    }

    private <T> Optional<DataMeshEvent<T>> convertEventPayload(DataMeshEvent<?> event, Class<T> targetClass) {
        if (event.getPayload() == null || targetClass.isInstance(event.getPayload())) {
            return Optional.of(setEventPayload(event, targetClass.cast(event.getPayload())));
        } else if (event.getPayload() instanceof byte[]) {
            byte[] data = (byte[]) event.getPayload();
            try {
                T newPayload = JsonUtils.MAPPER.readValue(data, targetClass);
                return Optional.of(setEventPayload(event, newPayload));
            } catch (Exception e) {
                LOG.warn("Cannot convert JSON payload for event {} to type {}", event, targetClass);
                LOG.warn("Got exception while parsing JSON", e);
                return Optional.empty();
            }
        } else {
            LOG.warn("Cannot convert event {} payload of type {} to class {}", event, event.getPayload().getClass(), targetClass);
            return Optional.empty();
        }
    }

    private <T> DataMeshEvent<T> setEventPayload(DataMeshEvent<?> event, T data) {
        return new DataMeshEvent<>(event.getGroup(), event.getName(), event.getClientIdentifier(), event.getClientVersion(),
                event.getVersion(), data);
    }

}
