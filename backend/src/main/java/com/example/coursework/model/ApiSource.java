package com.example.coursework.model;

import java.io.IOException;
import java.util.List;

public interface ApiSource {
    String getName();
    String getDisplayName();
    List<AggregatedRecord> fetchData() throws IOException;
}