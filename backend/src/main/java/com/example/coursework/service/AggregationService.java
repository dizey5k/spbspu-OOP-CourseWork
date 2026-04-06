package com.example.coursework.service;

import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AggregationService {
    public List<AggregatedRecord> fetchData(List<ApiSource> sources) {
        List<AggregatedRecord> allRecords = new ArrayList<>();
        for (ApiSource source : sources) {
            try {
                allRecords.addAll(source.fetchData());
            } catch (IOException e) {
                System.err.println("Err while getting data from " + source.displayName() + ": " + e.getMessage());
            }
        }
        return allRecords;
    }
}