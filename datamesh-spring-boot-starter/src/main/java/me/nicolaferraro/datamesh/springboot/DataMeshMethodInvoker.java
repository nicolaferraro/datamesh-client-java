package me.nicolaferraro.datamesh.springboot;

import me.nicolaferraro.datamesh.client.DataMeshClient;
import me.nicolaferraro.datamesh.client.DataMeshEvent;
import me.nicolaferraro.datamesh.springboot.annotation.DataMeshListener;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.regex.Pattern;

class DataMeshMethodInvoker {

    private DataMeshClient client;

    private Object bean;

    private Method method;

    private DataMeshListener annotation;

    private boolean bound;

    public DataMeshMethodInvoker(DataMeshClient client, Object bean, Method method, DataMeshListener annotation) {
        this.client = client;
        this.bean = bean;
        this.method = method;
        this.annotation = annotation;
    }

    public void bind() {
        if (!bound) {
            bound = true;

            client.onEvent(findEventGroupPattern(), findEventNamePattern(), findEventVersionPattern(), findTargetClass(), findProcessingFunction());
        }
    }

    protected Function<DataMeshEvent<Object>, Publisher<?>> findProcessingFunction() {
        return evt -> {

            try {
                Object result = method.invoke(bean, evt);
                if (result instanceof Publisher) {
                    return (Publisher<?>) result;
                } else {
                    return Mono.justOrEmpty(result);
                }
            } catch (Exception e) {
                return Flux.error(e);
            }

        };
    }

    protected Class<Object> findTargetClass() {
        if (this.method.getParameterCount() != 1) {
            throw new IllegalStateException("Expected only 1 parameter in DataMesh listener method, found " + this.method.getParameterCount());
        }

        Type type = this.method.getGenericParameterTypes()[0];
        if (!(type instanceof ParameterizedType) || !(((ParameterizedType)type).getRawType() instanceof Class) || !(DataMeshEvent.class.isAssignableFrom((Class<?>)((ParameterizedType)type).getRawType()))) {
            throw new IllegalStateException("DataMesh listener methods should declare a single parameter of type DataMeshEvent<T>");
        }

        ParameterizedType pType = (ParameterizedType) type;
        Type argType = pType.getActualTypeArguments()[0];
        if (!(argType instanceof Class)) {
            throw new IllegalStateException("Illegal argument type for DataMeshEvent: " + argType);
        }

        return (Class<Object>) argType;
    }

    protected Pattern findEventNamePattern() {
        int namePlaces = 0;
        String namePattern = ".*";
        if (!StringUtils.isEmpty(annotation.name())) {
            namePlaces++;
            namePattern = Pattern.quote(annotation.name());
        }
        if (!StringUtils.isEmpty(annotation.value())) {
            namePlaces++;
            namePattern = Pattern.quote(annotation.value());
        }
        if (!StringUtils.isEmpty(annotation.namePattern())) {
            namePlaces++;
            namePattern = annotation.namePattern();
        }

        if (namePlaces > 1) {
            throw new IllegalStateException("Too many DataMesh event name patterns provided in the annotation");
        }
        return Pattern.compile(namePattern);
    }

    protected Pattern findEventGroupPattern() {
        return Pattern.compile(".*");
    }

    protected Pattern findEventVersionPattern() {
        return Pattern.compile(".*");
    }
}
