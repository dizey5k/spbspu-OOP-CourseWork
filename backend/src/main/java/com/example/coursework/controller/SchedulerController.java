package com.example.coursework.controller;

import com.example.coursework.controller.dto.SchedulerRequest;
import com.example.coursework.model.ApiSource;
import com.example.coursework.scheduler.DataFetchScheduler;
import com.example.coursework.service.ApiSourceRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final ApiSourceRegistry sourceRegistry;
    private final DataFetchScheduler scheduler;

    public SchedulerController(ApiSourceRegistry sourceRegistry, DataFetchScheduler scheduler) {
        this.sourceRegistry = sourceRegistry;
        this.scheduler = scheduler;
    }

    @PostMapping("/start")
    public ResponseEntity<String> start(@RequestBody SchedulerRequest request) {
        List<ApiSource> sources = sourceRegistry.getSourcesByName(request.getSources());
        if (sources.isEmpty()) {
            return ResponseEntity.badRequest().body("No valid sources selected");
        }
        boolean started = scheduler.startScheduledFetch(
                sources,
                request.getMaxParallel(),
                request.getIntervalSeconds(),
                request.getFormat(),
                request.getFilename(),
                request.isAppend()
        );
        if (started) {
            return ResponseEntity.ok("Scheduled fetching started");
        } else {
            return ResponseEntity.status(409).body("Job already running");
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stop(@RequestParam(required = false) String filename) {
        if (filename == null || filename.isBlank()) {
            scheduler.stopAll();
            return ResponseEntity.ok("All jobs stopped");
        } else {
            boolean stopped = scheduler.stopJob(filename);
            return stopped ? ResponseEntity.ok("Job stopped") : ResponseEntity.status(404).body("Job not found");
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(scheduler.getStatus());
    }
}