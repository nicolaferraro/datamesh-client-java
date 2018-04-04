package me.nicolaferraro.datamesh.client;


import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;

public class DataMeshClientTest {

    static class MyEvent {
        public String name;
    }

    @Test
    public void testSimpleRead() {
        DataMeshClient client = DataMeshClient.create("localhost");

        Optional<String> data = client.readOnlyProjection().read("h1", String.class).blockOptional();
        assertFalse(data.isPresent());
    }

    @Test
    public void testClient() throws InterruptedException {
        DataMeshClient client = DataMeshClient.create("localhost");

        client.bind(Pattern.compile(".*"), Pattern.compile(".*"), MyEvent.class, eventFlux -> Flux.from(eventFlux).flatMap(event -> {
            DataMeshProjection prj = client.projection(event);

            return prj.read("hello", Integer.class)
                    .defaultIfEmpty(0)
                    .map(i -> i + 1)
                    .map(i -> prj.upsert("hello", i))
                    .map(any -> prj);
        }));

        client.pushEvent(new MyEvent(), "group", "name", UUID.randomUUID().toString(), "v1");
        Thread.sleep(1000);
        System.out.println(client.readOnlyProjection().read("hello", Integer.class).blockOptional());

    }

}
