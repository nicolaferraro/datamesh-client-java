package me.nicolaferraro.datamesh.springboot;

import me.nicolaferraro.datamesh.client.DataMeshClientFactory;
import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DataMeshProperties.class)
@ConditionalOnProperty("datamesh.host")
public class DataMeshAutoConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public DataMeshClient dataMeshClient(DataMeshProperties properties) {
        DataMeshClient client;
        if (properties.getPort() == null) {
            client = DataMeshClientFactory.create(properties.getHost());
        } else {
            client = DataMeshClientFactory.create(properties.getHost(), properties.getPort());
        }

        return client;
    }

    @Bean
    @ConditionalOnMissingBean
    public DataMeshPostProcessor dataMeshPostProcessor(DataMeshClient client) {
        return new DataMeshPostProcessor(client);
    }

}
