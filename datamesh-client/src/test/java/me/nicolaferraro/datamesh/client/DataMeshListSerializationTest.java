package me.nicolaferraro.datamesh.client;


import me.nicolaferraro.datamesh.client.api.DataMeshClient;
import me.nicolaferraro.datamesh.test.server.DataMeshTestServer;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

public class DataMeshListSerializationTest {

    @Test
    public void testListSerialization() throws Exception {
        DataMeshTestServer testServer = DataMeshTestServer.newTestServer();
        DataMeshClient client = DataMeshClientFactory.create(testServer.getHost(), testServer.getPort());

        MyPerson edson = new MyPerson("Edson", "Arantes", "Do", "Nascimiento");
        MyPerson edson2 = new MyPerson("Edson", "Arantes", "x", "Nascimiento");
        MyPerson pele = new MyPerson("Pelé");

        Pattern any = Pattern.compile(".*");
        client.onEvent(any, any, any, String.class, evt -> {
            if ("edson".equals(evt.getPayload())) {
                return evt.projection().upsert("people.edson", edson);
            } else if ("pele".equals(evt.getPayload())) {
                return evt.projection().upsert("people.pele", pele);
            } else if ("change".equals(evt.getPayload())) {
                return evt.projection().upsert("people.edson.surnames.1.value", "x");
            }
            return Flux.empty();
        });

        client.start();
        client.pushEvent("edson", "group", "name", "v1");
        client.pushEvent("pele", "group", "name", "v1");
        client.pushEvent("change", "group", "name", "v1");

        await().atMost(8, TimeUnit.SECONDS)
                .until(() -> client.projection().read("people.edson", MyPerson.class).blockOptional().orElse(null), equalTo(edson2));

        await().atMost(8, TimeUnit.SECONDS)
                .until(() -> client.projection().read("people.pele", MyPerson.class).blockOptional().orElse(null), equalTo(pele));

        client.stop();
        testServer.stop();
    }


    public static class MyPerson {
        private String name;

        private List<MySurname> surnames;

        public MyPerson() {
        }

        public MyPerson(String name, String... surnames) {
            this.name = name;
            this.surnames = Arrays.stream(surnames)
                .map(MySurname::new)
                .collect(Collectors.toList());
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<MySurname> getSurnames() {
            return surnames;
        }

        public void setSurnames(List<MySurname> surnames) {
            this.surnames = surnames;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MyPerson myPerson = (MyPerson) o;
            return Objects.equals(name, myPerson.name) &&
                    Objects.equals(nullToBlank(surnames), nullToBlank(myPerson.surnames));
        }

        @Override
        public int hashCode() {

            return Objects.hash(name, surnames);
        }

        @Override
        public String toString() {
            return "MyPerson{" +
                    "name='" + name + '\'' +
                    ", surnames=" + surnames +
                    '}';
        }
    }

    public static class MySurname {
        private String value;

        public MySurname() {
        }

        public MySurname(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MySurname mySurname = (MySurname) o;
            return Objects.equals(value, mySurname.value);
        }

        @Override
        public int hashCode() {

            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "MySurname{" +
                    "value='" + value + '\'' +
                    '}';
        }

    }

    private static <T> List<T> nullToBlank(List<T> lst) {
        return lst != null ? lst : Collections.emptyList();
    }


}
