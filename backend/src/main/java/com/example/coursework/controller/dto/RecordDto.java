package com.example.coursework.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public class RecordDto {
    private String id;
    private String source;
    private Instant timestamp;
    private JsonNode data;

    // конструктор из AggregatedRecord
    public RecordDto(String id, String source, Instant timestamp, JsonNode data) {
        this.id = id;
        this.source = source;
        this.timestamp = timestamp;
        this.data = data;
    }

    public String getId() {return id;}
    public String getSource() {return source;}
    public Instant getTimestamp() {return timestamp;}
    public JsonNode getData() {return data;}
}