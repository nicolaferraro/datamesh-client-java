package me.nicolaferraro.datamesh.client;


import me.nicolaferraro.datamesh.client.api.DataMeshEvent;
import me.nicolaferraro.datamesh.client.api.DataMeshProjection;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EventProcessorTest {

    @Test
    public void testProcessorResumeOnError() throws Exception {

        EventProcessor processor = new EventProcessor();

        Pattern any = Pattern.compile(".*");
        processor.addProcessingFunction(any, any, any, String.class, evt -> {
            if ("ok".equals(evt.getPayload())) {
                return Mono.empty();
            }
            throw new RuntimeException();
        });

        processor.start();

        AtomicInteger collector = new AtomicInteger();

        processor.enqueue(createEvent("Hello", collector));
        processor.enqueue(createEvent("ok", collector));
        processor.enqueue(createEvent("World", collector));
        processor.enqueue(createEvent("ok", collector));


        processor.start();

        await().atMost(3, TimeUnit.SECONDS).untilAtomic(collector, equalTo(2));

        processor.stop();
    }

    @SuppressWarnings("unchecked")
    private <T> DataMeshEvent<T> createEvent(T data, AtomicInteger collector) {
        DataMeshProjection prj = mock(DataMeshProjection.class);
        when(prj.persist()).thenAnswer(c -> {

            collector.incrementAndGet();
            return Mono.empty();
        });
        AtomicBoolean errors = new AtomicBoolean();
        doAnswer(c -> {
            errors.set(c.getArgument(0));
            return null;
        }).when(prj).setErrors(true);
        when(prj.hasErrors()).thenAnswer(c -> errors.get());

        DataMeshEvent<T> event = mock(DataMeshEvent.class);

        when(event.projection()).thenReturn(prj);
        when(event.getPayload()).thenReturn(data);
        when(event.withPayload(any())).thenReturn((DataMeshEvent)event);
        return event;
    }
}
