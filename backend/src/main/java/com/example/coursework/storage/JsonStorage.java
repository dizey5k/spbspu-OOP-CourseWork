package com.example.coursework.storage;

import com.example.coursework.model.AggregatedRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonStorage implements DataStorage {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public List<AggregatedRecord> load(Path file) throws IOException {
        if (!file.toFile().exists()) {
            return new ArrayList<>();
        }
        return mapper.readValue(file.toFile(), new TypeReference<List<AggregatedRecord>>() {});
    }

    @Override
    public void save(List<AggregatedRecord> records, Path file, boolean append) throws IOException {
        List<AggregatedRecord> allRecords = new ArrayList<>();
        if (append && file.toFile().exists()) {
            allRecords.addAll(load(file));
        }
        allRecords.addAll(records);
        mapper.writeValue(file.toFile(), allRecords);
    }
}