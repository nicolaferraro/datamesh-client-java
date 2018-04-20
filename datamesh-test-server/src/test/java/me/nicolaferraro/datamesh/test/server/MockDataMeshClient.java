package me.nicolaferraro.datamesh.test.server;

import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import me.nicolaferraro.datamesh.client.api.DataMeshEvent;
import me.nicolaferraro.datamesh.client.api.DataMeshReadableProjection;
import org.reactivestreams.Publisher;

import java.util.function.Function;
import java.util.regex.Pattern;

public class MockDataMeshClient implements DataMeshClient {

    private String host;

    private Integer port;

    public MockDataMeshClient(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public DataMeshReadableProjection projection() {
        return null;
    }

    @Override
    public Publisher<Void> pushEvent(Object data, String group, String name, String version) {
        return null;
    }

    @Override
    public <T> void onEvent(Pattern group, Pattern name, Pattern version, Class<T> eventClass, Function<DataMeshEvent<T>, Publisher<?>> processing) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
