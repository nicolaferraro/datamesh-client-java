package me.nicolaferraro.datamesh.client;


import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class DataMeshClientIT {

    static class MyEvent {
        public String name;

        public MyEvent() {
        }

        public MyEvent(String name) {
            this.name = name;
        }
    }

    @Test
    public void testSimpleRead() throws InterruptedException {
        DataMeshClient client = DataMeshClient.create("localhost");

//        Optional<String> data = client.projection().read("h1", String.class).blockOptional();
//        assertFalse(data.isPresent());

        Thread.sleep(100000);
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

        final int events = 10;
        for (int i=1; i<=events; i++) {
            client.pushEvent(new MyEvent("evt-" + i), "group", "evt-" + i, "v1");
        }

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> client.projection().read("counter", Integer.class).blockOptional().orElse(0), equalTo(initial + events));


        assertThat(client.projection().read("plus.counter", Integer.class).blockOptional().orElse(0), equalTo(initial + events + 1));
    }

    @Test
    public void testParallel() throws InterruptedException {
        DataMeshClient client = DataMeshClient.create("localhost");

        int value = new Random().nextInt();

        client.onEvent(Pattern.compile(".*"), Pattern.compile(".*"), Pattern.compile(".*"), MyEvent.class, evt -> {

            Mono<Void> delete = evt.projection().delete(evt.getName());

            Mono<Void> upsert = evt.projection().upsert(evt.getName(), value);

            return Flux.concat(delete, upsert);
        });

        final int events = 20;
        for (int i=1; i<=events; i++) {
            client.pushEvent(new MyEvent("evt-par-" + i), "group", "evt-par-" + i, "v1");
        }

        for (int i=1; i<=events; i++) {
            int idx = i;
            await().atMost(5, TimeUnit.SECONDS)
                    .until(() -> client.projection().read("evt-par-" + idx, Integer.class).blockOptional().orElse(0), equalTo(value));
        }
    }

}
