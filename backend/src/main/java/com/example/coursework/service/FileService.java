package com.example.coursework.service;

import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.storage.CsvStorage;
import com.example.coursework.storage.DataStorage;
import com.example.coursework.storage.JsonStorage;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class FileService {
    private DataStorage getStorage(String format) {
        if ("json".equalsIgnoreCase(format)) {
            return new JsonStorage();
        } else if ("csv".equalsIgnoreCase(format)) {
            return new CsvStorage();
        } else {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    public List<AggregatedRecord> load(Path file, String format) throws IOException {
        return getStorage(format).load(file);
    }

    public void save(List<AggregatedRecord> records, Path file, String format, boolean append) throws IOException {
        getStorage(format).save(records, file, append);
    }
}