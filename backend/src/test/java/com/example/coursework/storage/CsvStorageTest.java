package com.example.coursework.storage;

import com.example.coursework.model.AggregatedRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvStorageTest {

    private final CsvStorage storage = new CsvStorage();
    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadWithFlattening() throws Exception {
        Path file = tempDir.resolve("test.csv");
        JsonNode data = mapper.readTree("{\"name\":\"Alex\",\"age\":28,\"address\":{\"city\":\"Moscow\"}}");
        AggregatedRecord record = new AggregatedRecord("csv-source", data);
        storage.save(List.of(record), file, false);

        List<AggregatedRecord> loaded = storage.load(file);
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getSource()).isEqualTo("csv-source");
        assertThat(loaded.get(0).getData().get("name").asText()).isEqualTo("Alex");
        assertThat(loaded.get(0).getData().get("address").get("city").asText()).isEqualTo("Moscow");
    }

    @Test
    void appendShouldAddRows() throws Exception {
        Path file = tempDir.resolve("append.csv");
        JsonNode data1 = mapper.readTree("{\"x\":1}");
        JsonNode data2 = mapper.readTree("{\"y\":2}");

        storage.save(List.of(new AggregatedRecord("src1", data1)), file, false);
        storage.save(List.of(new AggregatedRecord("src2", data2)), file, true);

        List<AggregatedRecord> loaded = storage.load(file);
        assertThat(loaded).hasSize(2);
    }

    @Test
    void loadEmptyFileReturnsEmptyList() throws Exception {
        Path empty = tempDir.resolve("empty.csv");
        List<AggregatedRecord> loaded = storage.load(empty);
        assertThat(loaded).isEmpty();
    }
}