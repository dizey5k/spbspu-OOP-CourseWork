package com.example.coursework.model;

import java.io.IOException;
import java.util.List;

public interface ApiSource {
    String getName();

    String getDisplayName();

    String name();
    String displayName();
    List<AggregatedRecord> fetchData() throws IOException;
}