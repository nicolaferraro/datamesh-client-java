package me.nicolaferraro.datamesh.client.api;

import reactor.core.publisher.Mono;

public interface DataMeshReadableProjection {

    Mono<Boolean> isReady();

    <T> Mono<T> read(String path, Class<T> type);

}
