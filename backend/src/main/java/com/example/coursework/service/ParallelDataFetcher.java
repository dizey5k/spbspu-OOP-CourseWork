package com.example.coursework.service;

import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class ParallelDataFetcher {

    public List<AggregatedRecord> fetchAll(List<ApiSource> sources, int maxParallel) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        List<AggregatedRecord> allRecords;
        try (ExecutorService executor = Executors.newFixedThreadPool(maxParallel)) {
            List<Future<List<AggregatedRecord>>> futures = new ArrayList<>();

            for (ApiSource source : sources) {
                futures.add(executor.submit(() -> {
                    try {
                        return source.fetchData();
                    } catch (Exception e) {
                        System.err.println("Error fetching from " + source.displayName() + ": " + e.getMessage());
                        return List.<AggregatedRecord>of();
                    }
                }));
            }

            allRecords = new ArrayList<>();
            for (Future<List<AggregatedRecord>> future : futures) {
                try {
                    allRecords.addAll(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Interrupted while waiting for results: " + e.getMessage());
                }
            }
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        return allRecords;
    }
}