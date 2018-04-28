package me.nicolaferraro.datamesh.client;


import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import me.nicolaferraro.datamesh.client.api.DataMeshConnectionInfo;
import me.nicolaferraro.datamesh.test.server.DataMeshTestServer;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

public class DataMeshMultipleProjectsionsTest {


    @Test
    public void testMultipleProjections() throws Exception {
        DataMeshTestServer testServer = DataMeshTestServer.newTestServer();

        DataMeshClient client1 = DataMeshClientFactory.create(testServer.getHost(), testServer.getPort());
        DataMeshClient client2 = DataMeshClientFactory.create(new DataMeshConnectionInfo(testServer.getHost(), testServer.getPort(), DataMeshConnectionInfo.DEFAULT_CONTEXT_NAME, 2L));

        Pattern any = Pattern.compile(".*");

        client1.onEvent(any, any, any, String.class, evt -> {
            Mono<Integer> val = evt.projection().read("counter1", Integer.class).defaultIfEmpty(0);
            return val.flatMap(num -> evt.projection().upsert("counter1", num + 1));
        });

        client2.onEvent(any, any, any, String.class, evt -> {
            Mono<Integer> val = evt.projection().read("counter2", Integer.class).defaultIfEmpty(0);
            return val.flatMap(num -> evt.projection().upsert("counter2", num + 1));
        });

        client1.start();
        client2.start();

        client1.pushEvent("eventlog-is-shared", "group", "name", "v1");

        await().atMost(8, TimeUnit.SECONDS)
                .until(() -> client1.projection().read("counter1", Integer.class).blockOptional().orElse(0), equalTo(1));

        await().atMost(8, TimeUnit.SECONDS)
                .until(() -> client2.projection().read("counter2", Integer.class).blockOptional().orElse(0), equalTo(1));

        // TODO: optimize. Second transaction is slow because context default/2 waits for a preflight transaction that will never arrive (event is pushed by client 1)

        assertEquals(0, client1.projection().read("counter2", Integer.class).blockOptional().orElse(0).intValue());
        assertEquals(0, client2.projection().read("counter1", Integer.class).blockOptional().orElse(0).intValue());

        client1.stop();
        client2.stop();
        testServer.stop();
    }


}
