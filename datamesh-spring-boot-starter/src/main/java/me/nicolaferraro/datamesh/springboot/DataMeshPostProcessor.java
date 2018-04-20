package me.nicolaferraro.datamesh.springboot;

import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import me.nicolaferraro.datamesh.springboot.annotation.DataMeshListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public class DataMeshPostProcessor implements BeanPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DataMeshPostProcessor.class);

    private DataMeshClient client;

    public DataMeshPostProcessor(DataMeshClient client) {
        this.client = client;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        Map<Method, Optional<DataMeshListener>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<Optional<DataMeshListener>>) method -> {
                    DataMeshListener listenerMethod = AnnotatedElementUtils.getMergedAnnotation(
                            method, DataMeshListener.class);
                    return Optional.ofNullable(listenerMethod);
                });

        annotatedMethods
                .entrySet().stream()
                .filter(e -> e.getValue().isPresent())
                .peek(e -> LOG.debug("Found DataMesh annotated method: {}#{}", bean.getClass().getCanonicalName(), e.getKey().getName()))
                .map(e -> new DataMeshMethodInvoker(client, bean, e.getKey(), e.getValue().get()))
                .forEach(DataMeshMethodInvoker::bind);

        return bean;
    }

}
