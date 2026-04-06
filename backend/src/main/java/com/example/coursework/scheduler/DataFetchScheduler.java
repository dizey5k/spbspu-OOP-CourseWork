package com.example.coursework.scheduler;

import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import com.example.coursework.service.FileService;
import com.example.coursework.service.ParallelDataFetcher;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newScheduledThreadPool;

@Service
public class DataFetchScheduler {

    private final ParallelDataFetcher fetcher;
    private final FileService fileService;

    // every task has future to track and cancel if needed
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    public DataFetchScheduler(ParallelDataFetcher fetcher, FileService fileService) {
        this.fetcher = fetcher;
        this.fileService = fileService;
    }

    /**
     * @param sources       sources list
     * @param maxParallel   max amount parallel tasks
     * @param intervalSec   time between ping of single source
     * @param format        format file
     * @param filename      name file (without .*)
     * @param append        append
     * @return true, if start succeeded (no active task with simillar name)
     */
    public boolean startScheduledFetch(List<ApiSource> sources, int maxParallel,
                                       long intervalSec, String format,
                                       String filename, boolean append) {
        if (sources == null || sources.isEmpty()) return false;

        // filename = identifier of task
        String jobId = filename != null ? filename : "data";
        if (tasks.containsKey(jobId)) {
            return false; // уже запущено
        }

        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = newScheduledThreadPool(maxParallel);
        }

        for (ApiSource source : sources) {
            Runnable task = () -> {
                try {
                    List<AggregatedRecord> records = fetcher.fetchAll(List.of(source), 1);
                    if (!records.isEmpty()) {
                        Path filePath = Paths.get(filename + "." + format);
                        fileService.save(records, filePath, format, append);
                    }
                } catch (Exception e) {
                    System.err.println("Scheduled fetch error for " + source.getDisplayName() + ": " + e.getMessage());
                }
            };
            ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                    task, 0, intervalSec, TimeUnit.SECONDS);
            tasks.put(jobId + ":" + source.getName(), future);
        }
        return true;
    }

    public void stopAll() {
        tasks.values().forEach(future -> future.cancel(false));
        tasks.clear();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean stopJob(String filename) {
        String jobId = filename != null ? filename : "data";
        boolean removed = tasks.keySet().removeIf(key -> {
            if (key.startsWith(jobId + ":")) {
                ScheduledFuture<?> future = tasks.get(key);
                if (future != null) {
                    future.cancel(false);
                }
                return true;
            }
            return false;
        });
        if (tasks.isEmpty() && scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        return removed;
    }

    /**
     * get status of active tasks
     */
    public Map<String, Boolean> getStatus() {
        Map<String, Boolean> status = new ConcurrentHashMap<>();
        tasks.forEach((key, future) -> status.put(key, !future.isDone() && !future.isCancelled()));
        return status;
    }

    @PreDestroy
    public void shutdown() {
        stopAll();
    }
}