package com.example.coursework.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource({"id123, src1", "id456, src2"})
    void settersShouldWorkWithDifferentValues(String id, String source) {
        AggregatedRecord record = new AggregatedRecord();
        record.setId(id);
        record.setSource(source);
        assertThat(record.getId()).isEqualTo(id);
        assertThat(record.getSource()).isEqualTo(source);
    }
}