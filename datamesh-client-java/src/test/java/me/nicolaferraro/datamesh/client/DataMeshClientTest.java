package me.nicolaferraro.datamesh.client;


import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class DataMeshClientTest {

    static class MyEvent {
        public String name;

        public MyEvent(String name) {
            this.name = name;
        }
    }

    @Test
    public void testSimpleRead() {
        DataMeshClient client = DataMeshClient.create("localhost");

        Optional<String> data = client.projection().read("h1", String.class).blockOptional();
        assertFalse(data.isPresent());
    }

    @Test
    public void testClientApi() throws InterruptedException {
        DataMeshClient client = DataMeshClient.create("localhost");

        int initial = client.projection().read("counter", Integer.class).blockOptional().orElse(0);

        client.onEvent(Pattern.compile(".*"), Pattern.compile(".*"), Pattern.compile(".*"), MyEvent.class, evt -> {

            Mono<Integer> counter = evt.projection().read("counter", Integer.class)
                    .defaultIfEmpty(0);

            Mono<Void> upsert = counter.map(i -> i + 1)
                    .flatMap(i -> evt.projection().upsert("counter", i));

            Mono<Void> upsert2 = counter.map(i -> i + 2)
                    .flatMap(i -> evt.projection().upsert("plus.counter", i));

            return Flux.concat(upsert, upsert2);
        });

        // TODO manage transaction clash to increase the number of events
        final int events = 1;
        for (int i=0; i<events; i++) {
            client.pushEvent(new MyEvent("evt-" + i), "group", "name", "v1");
        }

        await().atMost(1, TimeUnit.SECONDS)
                .until(() -> client.projection().read("counter", Integer.class).blockOptional().orElse(0), equalTo(initial + events));


        assertThat(client.projection().read("plus.counter", Integer.class).blockOptional().orElse(0), equalTo(initial + events + 1));
    }

}
