package me.nicolaferraro.datamesh.springboot;

import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
@AutoConfigureAfter(DataMeshAutoConfiguration.class)
@ConditionalOnBean(DataMeshClient.class)
@ConditionalOnClass(ReactiveHealthIndicator.class)
@ConditionalOnProperty(value = "datamesh.health.enabled", matchIfMissing = true)
public class DataMeshHealthCheckAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataMeshHealthIndicator dataMeshHealthIndicator(DataMeshClient client) {
        return new DataMeshHealthIndicator(client);
    }


    public static class DataMeshHealthIndicator implements ReactiveHealthIndicator {

        private DataMeshClient client;

        public DataMeshHealthIndicator(DataMeshClient client) {
            this.client = client;
        }

        @Override
        public Mono<Health> health() {
            return Mono.first(
                    Mono.delay(Duration.ofSeconds(10)).map(tick -> false),
                    client.projection().isReady()
            ).map(ready -> {
                if (ready) {
                    return Health.up().build();
                } else {
                    return Health.down().build();
                }
            }).onErrorResume(e -> Mono.just(Health.down().build()));
        }
    }

}
