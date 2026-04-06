package com.example.coursework.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AggregatedRecordTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void constructorShouldSetIdAndTimestamp() {
        AggregatedRecord record = new AggregatedRecord("test", mapper.createObjectNode());
        assertThat(record.getId()).isNotNull();
        assertThat(record.getTimestamp()).isNotNull();
        assertThat(record.getSource()).isEqualTo("test");
    }

    @Test
    void settersShouldWork() {
        AggregatedRecord record = new AggregatedRecord();
        record.setId("123");
        record.setSource("src");
        assertThat(record.getId()).isEqualTo("123");
        assertThat(record.getSource()).isEqualTo("src");
    }
}