package me.nicolaferraro.datamesh.client.api;

import reactor.core.publisher.Mono;

public interface DataMeshProjection extends DataMeshReadableProjection {

    Mono<Void> upsert(String path, Object value);

    Mono<Void> delete(String path);

    Mono<Void> persist();

}
