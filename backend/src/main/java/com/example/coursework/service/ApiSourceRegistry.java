package com.example.coursework.service;

import com.example.coursework.model.ApiSource;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ApiSourceRegistry {
    private final Map<String, ApiSource> sourcesMap;

    public ApiSourceRegistry(List<ApiSource> sources) {
        this.sourcesMap = sources.stream()
                .collect(Collectors.toMap(ApiSource::getName, Function.identity()));
    }

    public List<ApiSource> getAllSources() {
        return List.copyOf(sourcesMap.values());
    }

    public ApiSource getSource(String name) {
        return sourcesMap.get(name);
    }

    public List<ApiSource> getSourcesByName(List<String> names) {
        return names.stream()
                .map(sourcesMap::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}