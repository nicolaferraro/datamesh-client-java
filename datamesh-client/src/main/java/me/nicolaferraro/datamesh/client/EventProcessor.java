package me.nicolaferraro.datamesh.client;

import me.nicolaferraro.datamesh.client.api.DataMeshEvent;
import me.nicolaferraro.datamesh.client.api.DataMeshProjection;
import me.nicolaferraro.datamesh.client.util.JsonUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
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

    private Function<DataMeshEvent<?>, Publisher<?>> processors;

    private Flux<?> connection;

    private Disposable connectionSubscription;

    public EventProcessor() {
        UnicastProcessor<DataMeshEvent<?>> flux = UnicastProcessor.create();
        this.sink = flux.sink();

        this.processors = evt -> Flux.empty();

        this.connection = flux.delayUntil(evt -> Flux.from(this.processors.apply(evt)))
                .filter(evt -> !evt.projection().hasErrors())
                .delayUntil(evt ->
                    evt.projection().persist().onErrorResume(err -> {
                        LOG.warn("Could not persist projection changes due to error", err);
                        return Mono.empty();
                    })
                );
    }

    public void start() {
        if (this.connectionSubscription == null) {
            this.connectionSubscription = connection.subscribe(evt -> {
                LOG.debug("Processing of event {} has completed", evt);
            }, e -> {
                LOG.error("Fatal error occurred while processing events", e);
            }, () -> {
                LOG.error("Event processing has terminated unexpectedly");
            });
        }
    }

    public void stop() {
        if (this.connectionSubscription != null) {
            this.connectionSubscription.dispose();
            this.connectionSubscription = null;
        }
    }

    public void enqueue(DataMeshEvent<?> event) {
        sink.next(event);
    }

    public <T> void addProcessingFunction(Pattern group, Pattern name, Pattern version, Class<T> eventClass, Function<DataMeshEvent<T>, Publisher<?>> processing) {
        Function<DataMeshEvent<?>, Publisher<?>> processor = processor(group, name, version, eventClass, processing);

        Function<DataMeshEvent<?>, Publisher<?>> currentProcessors = this.processors;
        this.processors = evt -> Mono.from(Flux.concat(currentProcessors.apply(evt), processor.apply(evt)));
    }

    private <T> Function<DataMeshEvent<?>, Publisher<?>> processor(Pattern group, Pattern name, Pattern version, Class<T> eventClass, Function<DataMeshEvent<T>, Publisher<?>> processing) {
        return evt -> {
            if (!group.asPredicate().test(Optional.ofNullable(evt.getGroup()).orElse(""))) {
                return Flux.empty();
            }
            if (!name.asPredicate().test(Optional.ofNullable(evt.getName()).orElse(""))) {
                return Flux.empty();
            }
            if (!version.asPredicate().test(Optional.ofNullable(evt.getVersion()).orElse(""))) {
                return Flux.empty();
            }

            Optional<DataMeshEvent<T>> convertedEvent = convertEventPayload(evt, eventClass);
            if (!convertedEvent.isPresent()) {
                return Flux.empty();
            }

            Publisher<?> resultPublisher;
            try {
                resultPublisher = processing.apply(convertedEvent.get());
            } catch (Exception ex) {
                resultPublisher = Flux.error(new DataMeshClientException("Error while calling function", ex));
            }

            return Flux.from(resultPublisher)
                .onErrorResume(e -> {
                    LOG.warn("An error occurred while invoking user defined method on event " + evt, e);
                    evt.projection().setErrors(true);
                    return Flux.empty();
                });
        };
    }

    private <T> Optional<DataMeshEvent<T>> convertEventPayload(DataMeshEvent<?> event, Class<T> targetClass) {
        if (event.getPayload() == null || targetClass.isInstance(event.getPayload())) {
            return Optional.of(event.withPayload(() -> targetClass.cast(event.getPayload())));
        } else if (event.getPayload() instanceof byte[]) {
            byte[] data = (byte[]) event.getPayload();
            try {
                T newPayload = JsonUtils.MAPPER.readValue(data, targetClass);
                return Optional.of(event.withPayload(() -> newPayload));
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

}
