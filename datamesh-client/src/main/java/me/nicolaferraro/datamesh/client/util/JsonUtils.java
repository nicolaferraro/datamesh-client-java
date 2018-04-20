package me.nicolaferraro.datamesh.client.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static <T> T unmarshal(byte[] data, Class<T> type) {
        try {
            return JsonUtils.MAPPER.readValue(data, type);
        } catch (Exception ex) {
            throw new RuntimeException("Error during unmarshal", ex);
        }
    }

}
