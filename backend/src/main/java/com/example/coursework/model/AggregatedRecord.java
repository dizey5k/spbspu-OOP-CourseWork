package com.example.coursework.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public class AggregatedRecord {
    private String id;
    private String source;
    private Instant timestamp;
    private JsonNode data;

    public AggregatedRecord() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public AggregatedRecord(String source, JsonNode data) {
        this();
        this.source = source;
        this.data = data;
    }

    // getter and setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public JsonNode getData() { return data; }
    public void setData(JsonNode data) { this.data = data; }
}