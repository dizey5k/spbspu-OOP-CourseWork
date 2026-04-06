package com.example.coursework.service;

import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregationServiceTest {

    private final AggregationService service = new AggregationService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private ApiSource source1;
    @Mock
    private ApiSource source2;

    @Test
    void fetchDataShouldCombineResults() throws IOException {
        AggregatedRecord rec1 = new AggregatedRecord("s1", mapper.createObjectNode());
        AggregatedRecord rec2 = new AggregatedRecord("s2", mapper.createObjectNode());

        when(source1.fetchData()).thenReturn(List.of(rec1));
        when(source2.fetchData()).thenReturn(List.of(rec2));

        List<AggregatedRecord> result = service.fetchData(List.of(source1, source2));

        assertThat(result).containsExactly(rec1, rec2);
    }

    @Test
    void fetchDataShouldSkipFailingSource() throws IOException {
        when(source1.fetchData()).thenThrow(new IOException("fail"));
        when(source2.fetchData()).thenReturn(List.of());

        List<AggregatedRecord> result = service.fetchData(List.of(source1, source2));

        assertThat(result).isEmpty();
        verify(source2).fetchData();
    }
}