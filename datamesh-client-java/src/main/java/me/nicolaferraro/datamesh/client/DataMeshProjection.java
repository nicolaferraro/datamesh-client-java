package me.nicolaferraro.datamesh.client;

import reactor.core.publisher.Mono;

import java.util.Optional;

public interface DataMeshProjection extends DataMeshReadableProjection {

    Optional<DataMeshEvent<?>> getEvent();

    Mono<Void> upsert(String path, Object value);

    Mono<Void> delete(String path);

    Mono<Void> persist();

}
