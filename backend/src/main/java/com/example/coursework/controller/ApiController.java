package com.example.coursework.controller;

import com.example.coursework.controller.dto.ApiInfoDto;
import com.example.coursework.controller.dto.FetchRequest;
import com.example.coursework.controller.dto.RecordDto;
import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import com.example.coursework.service.AggregationService;
import com.example.coursework.service.ApiSourceRegistry;
import com.example.coursework.service.FileService;
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
    private final AggregationService aggregationService;
    private final FileService fileService;

    public ApiController(ApiSourceRegistry sourceRegistry,
                         AggregationService aggregationService,
                         FileService fileService) {
        this.sourceRegistry = sourceRegistry;
        this.aggregationService = aggregationService;
        this.fileService = fileService;
    }

    /** Получить список доступных источников данных */
    @GetMapping("/sources")
    public List<ApiInfoDto> listSources() {
        return sourceRegistry.getAllSources().stream()
                .map(s -> new ApiInfoDto(s.getName(), s.getDisplayName()))
                .collect(Collectors.toList());
    }

    /** Запустить сбор данных из указанных источников и сохранить в файл */
    @PostMapping("/fetch")
    public ResponseEntity<?> fetchData(@RequestBody FetchRequest request) {
        // 1. Получаем выбранные источники
        List<ApiSource> sources = sourceRegistry.getSourcesByName(request.getSources());
        if (sources.isEmpty()) {
            return ResponseEntity.badRequest().body("No valid sources selected");
        }

        // 2. Собираем данные
        List<AggregatedRecord> newRecords = aggregationService.fetchData(sources);

        // 3. Определяем имя файла
        String format = request.getFormat().toLowerCase();
        if (!format.equals("json") && !format.equals("csv")) {
            return ResponseEntity.badRequest().body("Format must be 'json' or 'csv'");
        }
        String filename = request.getFilename();
        if (filename == null || filename.isBlank()) {
            filename = "data." + format;
        }
        Path filePath = Paths.get(filename);

        // 4. Сохраняем
        try {
            fileService.save(newRecords, filePath, format, request.isAppend());
            return ResponseEntity.ok(newRecords.size() + " records saved to " + filename);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving file: " + e.getMessage());
        }
    }

    /** Получить содержимое ранее сохранённого файла (с возможностью фильтрации по источнику) */
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
                        .collect(Collectors.toList());
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

    /** Простой эндпоинт для проверки работоспособности */
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}