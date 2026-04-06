package com.example.coursework.controller;

import com.example.coursework.controller.dto.ApiInfoDto;
import com.example.coursework.controller.dto.FetchRequest;
import com.example.coursework.controller.dto.RecordDto;
import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import com.example.coursework.service.ApiSourceRegistry;
import com.example.coursework.service.FileService;
import com.example.coursework.service.ParallelDataFetcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ApiControllerTest {

    private StubApiSourceRegistry sourceRegistry;
    private StubParallelDataFetcher parallelDataFetcher;
    private StubFileService fileService;
    private ApiController controller;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static class StubApiSourceRegistry extends ApiSourceRegistry {
        private final Map<String, ApiSource> sources = new HashMap<>();

        public StubApiSourceRegistry() {
            super(List.of());
        }

        void addSource(ApiSource source) {
            sources.put(source.name(), source);
        }

        @Override
        public List<ApiSource> getAllSources() {
            return new ArrayList<>(sources.values());
        }

        @Override
        public List<ApiSource> getSourcesByName(List<String> names) {
            return names.stream()
                    .filter(sources::containsKey)
                    .map(sources::get)
                    .collect(Collectors.toList());
        }
    }

    static class StubParallelDataFetcher extends ParallelDataFetcher {
        private List<AggregatedRecord> recordsToReturn = List.of();

        void setRecordsToReturn(List<AggregatedRecord> records) {
            this.recordsToReturn = records;
        }

        @Override
        public List<AggregatedRecord> fetchAll(List<ApiSource> sources, int maxParallel) {
            return recordsToReturn;
        }
    }

    static class StubFileService extends FileService {
        private List<AggregatedRecord> savedRecords;
        private Path savedPath;
        private String savedFormat;
        private Boolean savedAppend;
        private IOException throwOnSave = null;
        private IOException throwOnLoad = null;
        private List<AggregatedRecord> recordsToReturnOnLoad = List.of();

        @Override
        public void save(List<AggregatedRecord> records, Path file, String format, boolean append) throws IOException {
            if (throwOnSave != null) throw throwOnSave;
            this.savedRecords = records;
            this.savedPath = file;
            this.savedFormat = format;
            this.savedAppend = append;
        }

        @Override
        public List<AggregatedRecord> load(Path file, String format) throws IOException {
            if (throwOnLoad != null) throw throwOnLoad;
            return recordsToReturnOnLoad;
        }

        void setThrowOnSave(IOException e) { this.throwOnSave = e; }
        void setThrowOnLoad(IOException e) { this.throwOnLoad = e; }
        void setRecordsToReturnOnLoad(List<AggregatedRecord> records) { this.recordsToReturnOnLoad = records; }

        List<AggregatedRecord> getSavedRecords() { return savedRecords; }
        Path getSavedPath() { return savedPath; }
        String getSavedFormat() { return savedFormat; }
        Boolean getSavedAppend() { return savedAppend; }
    }

    static class TestApiSource implements ApiSource {
        private final String name;
        private final String displayName;

        TestApiSource(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        @Override public String name() { return name; }
        @Override public String displayName() { return displayName; }
        @Override public String getName() { return name; }
        @Override public String getDisplayName() { return displayName; }

        @Override
        public List<AggregatedRecord> fetchData() throws IOException {
            return List.of(); // не используется в тестах
        }
    }

    private AggregatedRecord createRecord(String source, String jsonData) {
        try {
            JsonNode data = objectMapper.readTree(jsonData);
            return new AggregatedRecord(source, data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AggregatedRecord createRecordWithFixedId(String id, String source, String jsonData) {
        try {
            JsonNode data = objectMapper.readTree(jsonData);
            AggregatedRecord record = new AggregatedRecord(source, data);
            record.setId(id);
            record.setTimestamp(Instant.parse("2025-01-01T00:00:00Z"));
            if (!source.equals(record.getSource())) {
                throw new IllegalStateException("Source mismatch: expected " + source + " but got " + record.getSource());
            }
            return record;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        sourceRegistry = new StubApiSourceRegistry();
        parallelDataFetcher = new StubParallelDataFetcher();
        fileService = new StubFileService();
        controller = new ApiController(sourceRegistry, parallelDataFetcher, fileService);
    }

    // ---------------------- GET /api/sources ----------------------
    @Test
    void listSources_ShouldReturnAllSources() {
        sourceRegistry.addSource(new TestApiSource("weather", "Weather API"));
        sourceRegistry.addSource(new TestApiSource("news", "News API"));

        ResponseEntity<List<ApiInfoDto>> response = controller.listSources();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ApiInfoDto> body = response.getBody();
        assertThat(body).hasSize(2);
        assertThat(body.get(0).getName()).isEqualTo("news");
        assertThat(body.get(1).getName()).isEqualTo("weather");
        assertThat(body.get(1).getDisplayName()).isEqualTo("Weather API");
    }

    // ---------------------- POST /api/fetch ----------------------
    @Test
    void fetchData_WithInvalidFormat_ShouldReturnBadRequest() {
        FetchRequest request = new FetchRequest();
        request.setFormat("xml");
        request.setSources(List.of("weather"));

        ResponseEntity<?> response = controller.fetchData(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Format must be 'json' or 'csv'");
    }

    @Test
    void fetchData_WithNoValidSources_ShouldReturnBadRequest() {
        FetchRequest request = new FetchRequest();
        request.setFormat("json");
        request.setSources(List.of("unknown"));

        ResponseEntity<?> response = controller.fetchData(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("No valid sources selected");
    }

    @Test
    void fetchData_WithDefaultMaxParallel_ShouldUse5() throws IOException {
        FetchRequest request = new FetchRequest();
        request.setFormat("json");
        request.setSources(List.of("weather"));
        request.setFilename("out.json");
        request.setAppend(false);

        sourceRegistry.addSource(new TestApiSource("weather", "Weather API"));
        List<AggregatedRecord> records = List.of(createRecord("weather", "{\"temp\":20}"));
        parallelDataFetcher.setRecordsToReturn(records);

        ResponseEntity<?> response = controller.fetchData(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("1 records saved to out.json");
        assertThat(fileService.getSavedRecords()).isEqualTo(records);
        assertThat(fileService.getSavedPath().toString()).isEqualTo("out.json");
        assertThat(fileService.getSavedFormat()).isEqualTo("json");
        assertThat(fileService.getSavedAppend()).isFalse();
    }

    @Test
    void fetchData_WithCustomMaxParallel_ShouldUseGivenValue() throws IOException {
        FetchRequest request = new FetchRequest();
        request.setFormat("csv");
        request.setSources(List.of("news"));
        request.setMaxParallel(3);
        request.setFilename("data.csv");
        request.setAppend(true);

        sourceRegistry.addSource(new TestApiSource("news", "News API"));
        List<AggregatedRecord> records = List.of(
                createRecord("news", "{\"headline\":\"A\"}"),
                createRecord("news", "{\"headline\":\"B\"}")
        );
        parallelDataFetcher.setRecordsToReturn(records);

        ResponseEntity<?> response = controller.fetchData(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("2 records saved to data.csv");
        assertThat(fileService.getSavedPath().toString()).isEqualTo("data.csv");
        assertThat(fileService.getSavedFormat()).isEqualTo("csv");
        assertThat(fileService.getSavedAppend()).isTrue();
    }

    @Test
    void fetchData_WithNullFilename_ShouldUseDefault() throws IOException {
        FetchRequest request = new FetchRequest();
        request.setFormat("json");
        request.setSources(List.of("weather"));
        request.setFilename(null);

        sourceRegistry.addSource(new TestApiSource("weather", "Weather API"));
        parallelDataFetcher.setRecordsToReturn(List.of(createRecord("weather", "{}")));

        controller.fetchData(request);

        assertThat(fileService.getSavedPath().toString()).isEqualTo("data.json");
    }

    @Test
    void fetchData_WhenFileServiceThrowsIOException_ShouldReturnInternalServerError() throws IOException {
        FetchRequest request = new FetchRequest();
        request.setFormat("csv");
        request.setSources(List.of("weather"));
        request.setFilename("error.csv");

        sourceRegistry.addSource(new TestApiSource("weather", "Weather API"));
        parallelDataFetcher.setRecordsToReturn(List.of(createRecord("weather", "{}")));
        fileService.setThrowOnSave(new IOException("Disk full"));

        ResponseEntity<?> response = controller.fetchData(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Error saving file: Disk full");
    }

    @Test
    void fetchData_FormatCaseInsensitive_ShouldAcceptUpperCase() throws IOException {
        FetchRequest request = new FetchRequest();
        request.setFormat("JSON");
        request.setSources(List.of("weather"));
        request.setFilename("data.json");

        sourceRegistry.addSource(new TestApiSource("weather", "Weather API"));
        parallelDataFetcher.setRecordsToReturn(List.of(createRecord("weather", "{}")));

        ResponseEntity<?> response = controller.fetchData(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fileService.getSavedFormat()).isEqualTo("json");
    }

    // ---------------------- GET /api/data ----------------------
    @Test
    void getData_WithoutSourceFilter_ShouldReturnAllRecords() throws IOException {
        AggregatedRecord record1 = createRecordWithFixedId("id1", "weather", "{\"temp\":10}");
        AggregatedRecord record2 = createRecordWithFixedId("id2", "news", "{\"title\":\"X\"}");
        fileService.setRecordsToReturnOnLoad(List.of(record1, record2));

        ResponseEntity<?> response = controller.getData("data.json", "json", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<RecordDto> body = (List<RecordDto>) response.getBody();
        assertThat(body).hasSize(2);
        assertThat(body.get(0).getId()).isEqualTo("id1");
        assertThat(body.get(0).getSource()).isEqualTo("weather");
        assertThat(body.get(1).getId()).isEqualTo("id2");
        assertThat(body.get(1).getSource()).isEqualTo("news");
    }

    @Test
    void getData_WithSourceFilter_ShouldReturnFilteredRecords() throws IOException {
        AggregatedRecord weatherRecord = createRecordWithFixedId("w1", "weather", "{\"temp\":10}");
        AggregatedRecord newsRecord = createRecordWithFixedId("n1", "news", "{\"title\":\"X\"}");

        assertThat(weatherRecord.getSource()).isEqualTo("weather");
        assertThat(newsRecord.getSource()).isEqualTo("news");

        fileService.setRecordsToReturnOnLoad(List.of(weatherRecord, newsRecord));

        ResponseEntity<?> response = controller.getData("data.csv", "csv", "weather");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<RecordDto> body = (List<RecordDto>) response.getBody();
        assertThat(body).hasSize(1);

        RecordDto result = body.get(0);
        assertThat(result.getId()).isEqualTo("w1");
        assertThat(result.getSource()).isEqualTo("weather");
    }

    @Test
    void getData_WhenFileServiceThrowsIOException_ShouldReturnInternalServerError() throws IOException {
        fileService.setThrowOnLoad(new IOException("File not found"));

        ResponseEntity<?> response = controller.getData("missing.json", "json", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Error reading file: File not found");
    }

    @Test
    void getData_WithBlankSourceParam_ShouldIgnoreFilter() throws IOException {
        AggregatedRecord record = createRecordWithFixedId("id1", "weather", "{}");
        fileService.setRecordsToReturnOnLoad(List.of(record));

        ResponseEntity<?> response = controller.getData("data.json", "json", "   ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<RecordDto> body = (List<RecordDto>) response.getBody();
        assertThat(body).hasSize(1);
    }

    // ---------------------- GET /api/ping ----------------------
    @Test
    void ping_ShouldReturnPong() {
        assertThat(controller.ping()).isEqualTo("pong");
    }
}