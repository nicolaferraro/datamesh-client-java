package me.nicolaferraro.datamesh.springboot;

import me.nicolaferraro.datamesh.client.DataMeshClientFactory;
import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import me.nicolaferraro.datamesh.client.api.DataMeshConnectionInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DataMeshProperties.class)
@ConditionalOnProperty("datamesh.host")
public class DataMeshAutoConfiguration {

    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    public DataMeshClient dataMeshClient(DataMeshProperties properties) {
        DataMeshConnectionInfo info = new DataMeshConnectionInfo(properties.getHost());
        if (properties.getPort() != null) {
            info.setPort(properties.getPort());
        }
        if (properties.getContext() != null) {
            if (properties.getContext().getName() != null) {
                info.setContextName(properties.getContext().getName());
            }
            if (properties.getContext().getRevision() != null) {
                info.setContextRevision(properties.getContext().getRevision());
            }
        }

        return  DataMeshClientFactory.create(info);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataMeshInitializer dataMeshInitializer(DataMeshClient client) {
        return new DataMeshInitializer(client);
    }

    /**
     * Initializer used to start the DataMesh client only after the application is fully started.
     */
    public static class DataMeshInitializer implements ApplicationListener<ApplicationStartedEvent> {

        private DataMeshClient client;

        private boolean done;

        public DataMeshInitializer(DataMeshClient client) {
            this.client = client;
        }

        @Override
        public void onApplicationEvent(ApplicationStartedEvent event) {
            if (!done) {
                done = true;
                client.start();
            }
        }
    }

}
