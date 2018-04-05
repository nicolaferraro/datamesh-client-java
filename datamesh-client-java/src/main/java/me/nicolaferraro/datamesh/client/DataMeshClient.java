package me.nicolaferraro.datamesh.client;

import org.reactivestreams.Publisher;

import java.util.function.Function;
import java.util.regex.Pattern;

public interface DataMeshClient {

    static DataMeshClient create(String host) {
        return new DefaultDataMeshClient(host);
    }

    static DataMeshClient create(String host, int port) {
        return new DefaultDataMeshClient(host, port);
    }

    DataMeshReadableProjection projection();

    Publisher<Void> pushEvent(Object data, String group, String name, String version);

    <T> void onEvent(Pattern group, Pattern name, Pattern version, Class<T> eventClass, Function<DataMeshEvent<T>, Publisher<?>> processing);

}
