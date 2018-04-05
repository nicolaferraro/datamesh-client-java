package me.nicolaferraro.datamesh.client;

import java.util.function.Supplier;

public interface DataMeshEvent<T> {

    String getGroup();

    String getName();

    String getVersion();

    T getPayload();

    DataMeshProjection projection();

    <R> DataMeshEvent<R> withPayload(Supplier<R> supplier);

}
