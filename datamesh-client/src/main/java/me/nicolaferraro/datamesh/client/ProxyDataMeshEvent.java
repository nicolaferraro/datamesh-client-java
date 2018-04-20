package me.nicolaferraro.datamesh.client;

import me.nicolaferraro.datamesh.client.api.DataMeshEvent;
import me.nicolaferraro.datamesh.client.api.DataMeshProjection;

import java.util.function.Supplier;

abstract class ProxyDataMeshEvent<F, T> implements DataMeshEvent<T> {

    private DataMeshEvent<F> delegate;

    public ProxyDataMeshEvent(DataMeshEvent<F> delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getGroup() {
        return delegate.getGroup();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @Override
    public DataMeshProjection projection() {
        return delegate.projection();
    }

    @Override
    public <R> DataMeshEvent<R> withPayload(Supplier<R> supplier) {
        R newPayload = supplier.get();
        return new ProxyDataMeshEvent<T, R>(this) {
            @Override
            public R getPayload() {
                return newPayload;
            }
        };
    }
}
