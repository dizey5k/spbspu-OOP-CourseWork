package com.example.coursework.service;

import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ParallelDataFetcherTest {

    private final ParallelDataFetcher fetcher = new ParallelDataFetcher();
    private final ObjectMapper mapper = new ObjectMapper();

    private final ApiSource source1 = mock(ApiSource.class);
    private final ApiSource source2 = mock(ApiSource.class);

    @Test
    void fetchAllShouldReturnCombinedResults() throws IOException {
        JsonNode data1 = mapper.createObjectNode().put("id", 1);
        JsonNode data2 = mapper.createObjectNode().put("id", 2);
        AggregatedRecord rec1 = new AggregatedRecord("src1", data1);
        AggregatedRecord rec2 = new AggregatedRecord("src2", data2);

        when(source1.fetchData()).thenReturn(List.of(rec1));
        when(source2.fetchData()).thenReturn(List.of(rec2));

        List<AggregatedRecord> result = fetcher.fetchAll(List.of(source1, source2), 2);

        assertThat(result).containsExactlyInAnyOrder(rec1, rec2);
        verify(source1).fetchData();
        verify(source2).fetchData();
    }

    @Test
    void fetchAllShouldHandleExceptionsGracefully() throws IOException {
        when(source1.fetchData()).thenThrow(new IOException("Network error"));
        when(source2.fetchData()).thenReturn(List.of());

        List<AggregatedRecord> result = fetcher.fetchAll(List.of(source1, source2), 2);

        assertThat(result).isEmpty();
        verify(source1).fetchData();
        verify(source2).fetchData();
    }

    @Test
    void fetchAllWithEmptySourcesReturnsEmptyList() {
        List<AggregatedRecord> result = fetcher.fetchAll(List.of(), 5);
        assertThat(result).isEmpty();
    }

    @Test
    void fetchAllUsesMaxParallelism() throws IOException {
        when(source1.fetchData()).thenReturn(List.of());
        when(source2.fetchData()).thenReturn(List.of());

        fetcher.fetchAll(List.of(source1, source2), 1);

        verify(source1).fetchData();
        verify(source2).fetchData();
    }
}