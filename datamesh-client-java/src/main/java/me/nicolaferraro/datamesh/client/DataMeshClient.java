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

    DataMeshReadableProjection readOnlyProjection();

    DataMeshProjection projection(DataMeshEvent<?> event);

    Publisher<Void> pushEvent(Object data, String group, String name, String clientIdentifier, String clientVersion);

    <T> void bind(Pattern group, Pattern name, Class<T> eventClass, Function<Publisher<DataMeshEvent<T>>, Publisher<DataMeshProjection>> processing);

}
