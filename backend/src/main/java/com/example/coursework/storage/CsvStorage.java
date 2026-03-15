package com.example.coursework.storage;

import com.example.coursework.model.AggregatedRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class CsvStorage implements DataStorage {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public List<AggregatedRecord> load(Path file) throws IOException {
        if (!file.toFile().exists()) {
            return new ArrayList<>();
        }
        try (CSVReader reader = new CSVReader(new FileReader(file.toFile()))) {
            List<String[]> lines = reader.readAll();
            if (lines.isEmpty()) return new ArrayList<>();
            String[] header = lines.get(0);
            // Индексы обязательных колонок
            int idIdx = -1, sourceIdx = -1, timestampIdx = -1;
            List<String> dataHeaders = new ArrayList<>();
            for (int i = 0; i < header.length; i++) {
                switch (header[i]) {
                    case "id": idIdx = i; break;
                    case "source": sourceIdx = i; break;
                    case "timestamp": timestampIdx = i; break;
                    default: dataHeaders.add(header[i]);
                }
            }
            if (idIdx == -1 || sourceIdx == -1 || timestampIdx == -1) {
                throw new IOException("CSV missing required columns");
            }

            List<AggregatedRecord> records = new ArrayList<>();
            for (int row = 1; row < lines.size(); row++) {
                String[] line = lines.get(row);
                if (line.length <= idIdx) continue;

                Map<String, String> flatData = new LinkedHashMap<>();
                for (int i = 0; i < dataHeaders.size(); i++) {
                    int colIdx = dataHeaders.size() == 0 ? -1 : i + 3; // после трёх обязательных
                    if (colIdx >= 0 && colIdx < line.length && !line[colIdx].isEmpty()) {
                        flatData.put(dataHeaders.get(i), line[colIdx]);
                    }
                }

                AggregatedRecord record = new AggregatedRecord();
                record.setId(line[idIdx]);
                record.setSource(line[sourceIdx]);
                record.setTimestamp(Instant.parse(line[timestampIdx]));
                record.setData(FlattenUtils.expand(flatData, mapper));
                records.add(record);
            }
            return records;
        } catch (CsvException e) {
            throw new IOException("Error parsing CSV", e);
        }
    }

    @Override
    public void save(List<AggregatedRecord> records, Path file, boolean append) throws IOException {
        List<AggregatedRecord> allRecords = new ArrayList<>();
        if (append && file.toFile().exists()) {
            allRecords.addAll(load(file));
        }
        allRecords.addAll(records);

        // collect all keys from data
        Set<String> allKeys = new LinkedHashSet<>();
        List<Map<String, String>> flatRecords = new ArrayList<>();
        for (AggregatedRecord rec : allRecords) {
            Map<String, String> flat = FlattenUtils.flatten(rec.getData(), "");
            flatRecords.add(flat);
            allKeys.addAll(flat.keySet());
        }
        List<String> sortedKeys = allKeys.stream().sorted().collect(Collectors.toList());

        // form header
        List<String> header = new ArrayList<>();
        header.add("id");
        header.add("source");
        header.add("timestamp");
        header.addAll(sortedKeys);

        try (CSVWriter writer = new CSVWriter(new FileWriter(file.toFile()))) {
            writer.writeNext(header.toArray(new String[0]));

            for (int i = 0; i < allRecords.size(); i++) {
                AggregatedRecord rec = allRecords.get(i);
                Map<String, String> flat = flatRecords.get(i);
                List<String> row = new ArrayList<>();
                row.add(rec.getId());
                row.add(rec.getSource());
                row.add(rec.getTimestamp().toString());
                for (String key : sortedKeys) {
                    row.add(flat.getOrDefault(key, ""));
                }
                writer.writeNext(row.toArray(new String[0]));
            }
        }
    }
}