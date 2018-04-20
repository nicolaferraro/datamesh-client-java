package me.nicolaferraro.datamesh.client.api;

import reactor.core.publisher.Mono;

public interface DataMeshReadableProjection {

    <T> Mono<T> read(String path, Class<T> type);

}
