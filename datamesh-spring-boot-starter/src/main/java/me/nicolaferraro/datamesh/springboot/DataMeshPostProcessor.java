package me.nicolaferraro.datamesh.springboot;

import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import me.nicolaferraro.datamesh.springboot.annotation.DataMeshListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public class DataMeshPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private static final Logger LOG = LoggerFactory.getLogger(DataMeshPostProcessor.class);

    private BeanFactory beanFactory;

    public DataMeshPostProcessor() {
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        if (bean instanceof DataMeshAutoConfiguration ||
                bean instanceof DataMeshAutoConfiguration.DataMeshInitializer ||
                bean instanceof DataMeshClient ||
                bean instanceof DataMeshProperties) {
            return bean;
        }

        DataMeshClient client;
        if (beanFactory != null && beanFactory instanceof ConfigurableListableBeanFactory) {
            try {
                client = beanFactory.getBean(DataMeshClient.class);
            } catch (BeansException e) {
                return bean;
            }
        } else {
            return bean;
        }

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
