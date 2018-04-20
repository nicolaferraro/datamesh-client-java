package me.nicolaferraro.datamesh.springboot;

import me.nicolaferraro.datamesh.client.DataMeshClientException;
import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import me.nicolaferraro.datamesh.client.api.DataMeshEvent;
import me.nicolaferraro.datamesh.springboot.annotation.DataMeshListener;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
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
            if (Publisher.class.isAssignableFrom(method.getReturnType())) {
                return (Publisher<?>) invoke(method, bean, evt);
            } else {
                return Mono.just(true)
                        .subscribeOn(Schedulers.newElastic("datamesh"))
                        .map(b -> Optional.ofNullable(invoke(method, bean, evt)));
            }
        };
    }

    protected Object invoke(Method method, Object bean, DataMeshEvent evt) {
        try {
            return method.invoke(bean, evt);
        } catch (Exception ex) {
            throw new DataMeshClientException("Error while invoking user provided method " + method.getName() + " on class " + (bean != null ? bean.getClass().getCanonicalName() : "null"), ex);
        }
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
