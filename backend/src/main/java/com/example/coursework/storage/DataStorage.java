package com.example.coursework.storage;

import com.example.coursework.model.AggregatedRecord;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DataStorage {
    List<AggregatedRecord> load(Path file) throws IOException;
    void save(List<AggregatedRecord> records, Path file, boolean append) throws IOException;
}