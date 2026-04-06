package com.example.coursework.scheduler;

import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import com.example.coursework.service.FileService;
import com.example.coursework.service.ParallelDataFetcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataFetchSchedulerTest {

    private TestApiSource source;
    private DataFetchScheduler scheduler;

    static class StubParallelDataFetcher extends ParallelDataFetcher {
        @Override
        public List<AggregatedRecord> fetchAll(List<ApiSource> sources, int maxParallel) {
            return List.of();
        }
    }

    static class StubFileService extends FileService {
        @Override
        public void save(List<AggregatedRecord> records, Path file, String format, boolean append) throws IOException {
        }
    }

    static class TestApiSource implements ApiSource {
        @Override
        public String getName() {
            return "test";
        }

        @Override
        public String getDisplayName() {
            return "Test Source";
        }

        @Override
        public String name() {
            return "";
        }

        @Override
        public String displayName() {
            return "";
        }

        @Override
        public List<AggregatedRecord> fetchData() {
            return List.of();
        }
    }

    @BeforeEach
    void setUp() {
        StubParallelDataFetcher fetcher = new StubParallelDataFetcher();
        StubFileService fileService = new StubFileService();
        source = new TestApiSource();
        scheduler = new DataFetchScheduler(fetcher, fileService);
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.stopAll();
        }
    }

    @Test
    void startScheduledFetchShouldNotDuplicateTasks() {
        scheduler.startScheduledFetch(List.of(source), 1, 60, "json", "data", false);
        int sizeAfterFirst = scheduler.getStatus().size();

        scheduler.startScheduledFetch(List.of(source), 1, 60, "json", "data", false);
        int sizeAfterSecond = scheduler.getStatus().size();

        assertThat(sizeAfterSecond).isEqualTo(sizeAfterFirst);
    }

    @Test
    void stopJobShouldRemoveTasks() {
        scheduler.startScheduledFetch(List.of(source), 1, 60, "json", "data", false);
        boolean stopped = scheduler.stopJob("data");
        assertThat(stopped).isTrue();
        assertThat(scheduler.getStatus()).isEmpty();
    }

    @Test
    void stopJobWithUnknownFilenameReturnsFalse() {
        boolean stopped = scheduler.stopJob("unknown");
        assertThat(stopped).isFalse();
    }

    @Test
    void getStatusReturnsActiveTasks() {
        scheduler.startScheduledFetch(List.of(source), 1, 60, "json", "data", false);
        var status = scheduler.getStatus();
        assertThat(status).hasSize(1);
        assertThat(status.values().iterator().next()).isTrue();
    }
}