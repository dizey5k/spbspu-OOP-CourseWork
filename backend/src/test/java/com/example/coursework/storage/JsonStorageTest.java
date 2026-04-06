package com.example.coursework.storage;

import com.example.coursework.model.AggregatedRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonStorageTest {

    private final JsonStorage storage = new JsonStorage();
    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadShouldPreserveRecords() throws Exception {
        Path file = tempDir.resolve("test.json");
        JsonNode data = mapper.readTree("{\"key\":\"value\"}");
        AggregatedRecord record = new AggregatedRecord("test-source", data);
        List<AggregatedRecord> records = List.of(record);

        storage.save(records, file, false);
        List<AggregatedRecord> loaded = storage.load(file);

        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getSource()).isEqualTo("test-source");
        assertThat(loaded.get(0).getData().get("key").asText()).isEqualTo("value");
    }

    @Test
    void appendShouldAddToExistingFile() throws Exception {
        Path file = tempDir.resolve("append.json");
        JsonNode data1 = mapper.readTree("{\"a\":1}");
        JsonNode data2 = mapper.readTree("{\"b\":2}");

        storage.save(List.of(new AggregatedRecord("src1", data1)), file, false);
        storage.save(List.of(new AggregatedRecord("src2", data2)), file, true);

        List<AggregatedRecord> loaded = storage.load(file);
        assertThat(loaded).hasSize(2);
        assertThat(loaded.get(0).getSource()).isEqualTo("src1");
        assertThat(loaded.get(1).getSource()).isEqualTo("src2");
    }

    @Test
    void loadNonExistentFileReturnsEmptyList() throws Exception {
        Path missing = tempDir.resolve("missing.json");
        List<AggregatedRecord> loaded = storage.load(missing);
        assertThat(loaded).isEmpty();
    }
}