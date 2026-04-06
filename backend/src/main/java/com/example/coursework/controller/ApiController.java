package com.example.coursework.controller;

import com.example.coursework.controller.dto.ApiInfoDto;
import com.example.coursework.controller.dto.FetchRequest;
import com.example.coursework.controller.dto.RecordDto;
import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import com.example.coursework.service.AggregationService;
import com.example.coursework.service.ApiSourceRegistry;
import com.example.coursework.service.FileService;
import com.example.coursework.service.ParallelDataFetcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final ApiSourceRegistry sourceRegistry;
    private final ParallelDataFetcher parallelDataFetcher;
    private final FileService fileService;

    public ApiController(ApiSourceRegistry sourceRegistry,
                         ParallelDataFetcher parallelDataFetcher,
                         FileService fileService) {
        this.sourceRegistry = sourceRegistry;
        this.parallelDataFetcher = parallelDataFetcher;
        this.fileService = fileService;
    }

    @GetMapping("/sources")
    public List<ApiInfoDto> listSources() {
        return sourceRegistry.getAllSources().stream()
                .map(s -> new ApiInfoDto(s.getName(), s.getDisplayName()))
                .collect(Collectors.toList());
    }

    @PostMapping("/fetch")
    public ResponseEntity<?> fetchData(@RequestBody FetchRequest request) {
        List<ApiSource> sources = sourceRegistry.getSourcesByName(request.getSources());
        if (sources.isEmpty()) {
            return ResponseEntity.badRequest().body("No valid sources selected");
        }

        int maxParallel = request.getMaxParallel() != null ? request.getMaxParallel() : 5;
        List<AggregatedRecord> newRecords = parallelDataFetcher.fetchAll(sources, maxParallel);

        String format = request.getFormat().toLowerCase();
        if (!format.equals("json") && !format.equals("csv")) {
            return ResponseEntity.badRequest().body("Format must be 'json' or 'csv'");
        }
        String filename = request.getFilename();
        if (filename == null || filename.isBlank()) {
            filename = "data." + format;
        }
        Path filePath = Paths.get(filename);

        try {
            fileService.save(newRecords, filePath, format, request.isAppend());
            return ResponseEntity.ok(newRecords.size() + " records saved to " + filename);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving file: " + e.getMessage());
        }
    }

    @GetMapping("/data")
    public ResponseEntity<?> getData(
            @RequestParam String filename,
            @RequestParam String format,
            @RequestParam(required = false) String source) {
        try {
            Path filePath = Paths.get(filename);
            List<AggregatedRecord> records = fileService.load(filePath, format);

            if (source != null && !source.isBlank()) {
                records = records.stream()
                        .filter(r -> source.equals(r.getSource()))
                        .toList();
            }

            List<RecordDto> dtos = records.stream()
                    .map(r -> new RecordDto(r.getId(), r.getSource(), r.getTimestamp(), r.getData()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading file: " + e.getMessage());
        }
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}