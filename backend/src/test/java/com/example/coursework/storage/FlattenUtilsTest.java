package com.example.coursework.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlattenUtilsTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void flattenShouldConvertNestedObjectToDotNotation() throws Exception {
        String json = "{\"user\":{\"name\":\"John\",\"age\":30},\"active\":true}";
        JsonNode node = mapper.readTree(json);

        Map<String, String> flat = FlattenUtils.flatten(node, "");

        assertThat(flat).containsExactlyInAnyOrderEntriesOf(Map.of(
                "user.name", "John",
                "user.age", "30",
                "active", "true"
        ));
    }

    @Test
    void flattenShouldHandleArrayAsString() throws Exception {
        String json = "{\"tags\":[\"java\",\"spring\"]}";
        JsonNode node = mapper.readTree(json);

        Map<String, String> flat = FlattenUtils.flatten(node, "");

        assertThat(flat).containsKey("tags");
        assertThat(flat.get("tags")).contains("java", "spring");
    }

    @Test
    void expandShouldRebuildNestedObject() {
        Map<String, String> flat = Map.of(
                "user.name", "Jane",
                "user.age", "25",
                "active", "false"
        );
        JsonNode expanded = FlattenUtils.expand(flat, mapper);

        assertThat(expanded.get("user").get("name").asText()).isEqualTo("Jane");
        assertThat(expanded.get("user").get("age").asLong()).isEqualTo(25L);
        assertThat(expanded.get("active").asBoolean()).isFalse();
    }

    @Test
    void expandShouldParseNumbersAndBooleans() {
        Map<String, String> flat = Map.of(
                "count", "42",
                "ratio", "3.14",
                "flag", "true"
        );
        JsonNode expanded = FlattenUtils.expand(flat, mapper);

        assertThat(expanded.get("count").isLong()).isTrue();
        assertThat(expanded.get("count").asLong()).isEqualTo(42L);
        assertThat(expanded.get("ratio").asDouble()).isEqualTo(3.14);
        assertThat(expanded.get("flag").asBoolean()).isTrue();
    }
}