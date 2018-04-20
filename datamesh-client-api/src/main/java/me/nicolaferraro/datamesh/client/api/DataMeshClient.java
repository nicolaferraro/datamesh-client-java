package me.nicolaferraro.datamesh.client.api;

import org.reactivestreams.Publisher;

import java.util.function.Function;
import java.util.regex.Pattern;

public interface DataMeshClient {

    DataMeshReadableProjection projection();

    Publisher<Void> pushEvent(Object data, String group, String name, String version);

    <T> void onEvent(Pattern group, Pattern name, Pattern version, Class<T> eventClass, Function<DataMeshEvent<T>, Publisher<?>> processing);

    void start();

    void stop();

}
