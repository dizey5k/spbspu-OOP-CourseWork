package com.example.coursework.scheduler;

import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import com.example.coursework.service.FileService;
import com.example.coursework.service.ParallelDataFetcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.*;

class DataFetchSchedulerTest {

    private TestApiSource source1;
    private TestApiSource source2;
    private DataFetchScheduler scheduler;

    static class StubParallelDataFetcher extends ParallelDataFetcher {
        @Override
        public List<AggregatedRecord> fetchAll(List<ApiSource> sources, int maxParallel) {
            return List.of();
        }
    }

    static class StubFileService extends FileService {
        private List<AggregatedRecord> savedRecords;
        private Path savedPath;
        private String savedFormat;
        private Boolean savedAppend;

        @Override
        public void save(List<AggregatedRecord> records, Path file, String format, boolean append) throws IOException {
            this.savedRecords = records;
            this.savedPath = file;
            this.savedFormat = format;
            this.savedAppend = append;
        }

        public List<AggregatedRecord> getSavedRecords() {
            return savedRecords;
        }

        public Path getSavedPath() {
            return savedPath;
        }

        public String getSavedFormat() {
            return savedFormat;
        }

        public Boolean getSavedAppend() {
            return savedAppend;
        }
    }

    static class ThrowingParallelDataFetcher extends ParallelDataFetcher {
        @Override
        public List<AggregatedRecord> fetchAll(List<ApiSource> sources, int maxParallel) {
            throw new RuntimeException("Test exception");
        }
    }

    static class TestApiSource implements ApiSource {
        private final String name;
        TestApiSource(String name) {
            this.name = name;
        }
        TestApiSource() {
            this("test");
        }
        @Override public String getName() { return name; }
        @Override public String getDisplayName() { return "Test Source " + name; }
        @Override public String name() { return name; }
        @Override public String displayName() { return getDisplayName(); }
        @Override public List<AggregatedRecord> fetchData() { return List.of(); }
    }

    @BeforeEach
    void setUp() {
        StubParallelDataFetcher fetcher = new StubParallelDataFetcher();
        StubFileService fileService = new StubFileService();
        source1 = new TestApiSource("src1");
        source2 = new TestApiSource("src2");
        scheduler = new DataFetchScheduler(fetcher, fileService);
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.stopAll();
        }
    }

    // ---------------------- startScheduledFetch ----------------------

    @Test
    void startScheduledFetch_WithValidParams_ShouldReturnTrue() {
        boolean result = scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data", false);
        assertThat(result).isTrue();
        assertThat(scheduler.getStatus()).hasSize(1);
    }

    @Test
    void startScheduledFetch_WithEmptySources_ShouldReturnFalse() {
        boolean result = scheduler.startScheduledFetch(List.of(), 1, 60, "json", "data", false);
        assertThat(result).isFalse();
        assertThat(scheduler.getStatus()).isEmpty();
    }

    @Test
    void startScheduledFetch_WithNullSources_ShouldReturnFalse() {
        boolean result = scheduler.startScheduledFetch(null, 1, 60, "json", "data", false);
        assertThat(result).isFalse();
    }

    @Test
    void startScheduledFetch_DuplicateFilename_ShouldReturnFalse() {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data", false);
        boolean second = scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data", false);
        assertThat(second).isFalse();
        assertThat(scheduler.getStatus()).hasSize(1);
    }

    @Test
    void startScheduledFetch_WithNullFilename_UsesDefaultJobId() {
        boolean result = scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", null, false);
        assertThat(result).isTrue();
        assertThat(scheduler.getStatus()).containsKey("data:src1");
    }

    @Test
    void startScheduledFetch_AfterShutdown_ShouldRecreateScheduler() {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data", false);
        scheduler.stopAll();
        boolean result = scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "newdata", false);
        assertThat(result).isTrue();
        assertThat(scheduler.getStatus()).hasSize(1);
    }

    @Test
    void startScheduledFetch_WithMultipleSources_CreatesMultipleTasks() {
        boolean result = scheduler.startScheduledFetch(List.of(source1, source2), 1, 60, "csv", "multi", false);
        assertThat(result).isTrue();
        assertThat(scheduler.getStatus()).hasSize(2);
        assertThat(scheduler.getStatus()).containsKeys("multi:src1", "multi:src2");
    }

    @Test
    void scheduledTask_SavesRecordsWhenNotEmpty() throws Exception {
        StubFileService fileService = new StubFileService();
        DataFetchScheduler.NonEmptyParallelDataFetcher fetcher = new DataFetchScheduler.NonEmptyParallelDataFetcher();
        DataFetchScheduler scheduler = new DataFetchScheduler(fetcher, fileService);
        TestApiSource source = new TestApiSource("src");
        scheduler.startScheduledFetch(List.of(source), 1, 1, "json", "data", false);
        Thread.sleep(1500);
        scheduler.stopAll();
        assertThat(fileService.getSavedRecords()).isNotEmpty(); // нужен getter в StubFileService
    }

    // ---------------------- stopJob ----------------------

    @Test
    void stopJob_WithExistingFilename_ShouldRemoveTasksAndReturnTrue() {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data", false);
        boolean stopped = scheduler.stopJob("data");
        assertThat(stopped).isTrue();
        assertThat(scheduler.getStatus()).isEmpty();
    }

    @Test
    void stopJob_WithMultipleSources_ShouldRemoveAllTasksForFilename() {
        scheduler.startScheduledFetch(List.of(source1, source2), 1, 60, "json", "multi", false);
        assertThat(scheduler.getStatus()).hasSize(2);
        boolean stopped = scheduler.stopJob("multi");
        assertThat(stopped).isTrue();
        assertThat(scheduler.getStatus()).isEmpty();
    }

    @Test
    void stopJob_WithUnknownFilename_ShouldReturnFalse() {
        boolean stopped = scheduler.stopJob("unknown");
        assertThat(stopped).isFalse();
    }

    @Test
    void stopJob_WithNullFilename_UsesDefault() {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", null, false);
        boolean stopped = scheduler.stopJob(null);
        assertThat(stopped).isTrue();
        assertThat(scheduler.getStatus()).isEmpty();
    }

    @Test
    void scheduledTask_HandlesExceptionGracefully() throws Exception {
        ThrowingParallelDataFetcher fetcher = new ThrowingParallelDataFetcher();
        StubFileService fileService = new StubFileService();
        DataFetchScheduler scheduler = new DataFetchScheduler(fetcher, fileService);
        TestApiSource source = new TestApiSource("src");
        scheduler.startScheduledFetch(List.of(source), 1, 1, "json", "data", false);
        Thread.sleep(1500);
        scheduler.stopAll();
    }

    // ---------------------- stopAll ----------------------

    @Test
    void stopAll_ShouldCancelAllTasks() {
        scheduler.startScheduledFetch(List.of(source1, source2), 1, 60, "json", "multi", false);
        assertThat(scheduler.getStatus()).hasSize(2);
        scheduler.stopAll();
        assertThat(scheduler.getStatus()).isEmpty();
    }

    @Test
    void stopJob_WhenLastTaskRemoved_ShouldShutdownScheduler() {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data", false);
        assertThat(scheduler.getStatus()).isNotEmpty();
        scheduler.stopJob("data");
        assertThat(scheduler.getStatus()).isEmpty();
        boolean restarted = scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "newdata", false);
        assertThat(restarted).isTrue();
    }

    @Test
    void stopAll_WhenAwaitTerminationTimesOut_ShouldCallShutdownNow() throws Exception {
        ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);
        when(mockExecutor.isShutdown()).thenReturn(false);
        when(mockExecutor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);

        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        doReturn(mockFuture).when(mockExecutor).scheduleWithFixedDelay(
                any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)
        );

        StubParallelDataFetcher fetcher = new StubParallelDataFetcher();
        StubFileService fileService = new StubFileService();
        DataFetchScheduler scheduler = new DataFetchScheduler(fetcher, fileService, mockExecutor);
        TestApiSource source = new TestApiSource("src");
        scheduler.startScheduledFetch(List.of(source), 1, 60, "json", "data", false);

        scheduler.stopAll();

        verify(mockExecutor).shutdownNow();
    }

    @Test
    void stopAll_WhenInterruptedDuringAwait_ShouldRestoreInterrupt() throws Exception {
        ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);
        when(mockExecutor.isShutdown()).thenReturn(false);
        when(mockExecutor.awaitTermination(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        doReturn(mockFuture).when(mockExecutor).scheduleWithFixedDelay(
                any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)
        );

        StubParallelDataFetcher fetcher = new StubParallelDataFetcher();
        StubFileService fileService = new StubFileService();
        DataFetchScheduler scheduler = new DataFetchScheduler(fetcher, fileService, mockExecutor);
        TestApiSource source = new TestApiSource("src");
        scheduler.startScheduledFetch(List.of(source), 1, 60, "json", "data", false);

        assertThat(Thread.currentThread().isInterrupted()).isFalse();
        scheduler.stopAll();

        verify(mockExecutor).shutdownNow();
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted(); // сброс флага
    }

    @Test
    void stopAll_WhenNoTasks_ShouldDoNothing() {
        assertThatCode(() -> scheduler.stopAll()).doesNotThrowAnyException();
        assertThat(scheduler.getStatus()).isEmpty();
    }

    @Test
    void startScheduledFetch_WithNullFilename_UsesDefaultFilenameForSaving() throws Exception {
        StubFileService fileService = new StubFileService();
        DataFetchScheduler.NonEmptyParallelDataFetcher fetcher = new DataFetchScheduler.NonEmptyParallelDataFetcher();
        DataFetchScheduler scheduler = new DataFetchScheduler(fetcher, fileService);
        TestApiSource source = new TestApiSource("src");

        scheduler.startScheduledFetch(List.of(source), 1, 1, "json", null, false);
        Thread.sleep(1500); // даём выполниться хотя бы раз
        scheduler.stopAll();

        assertThat(fileService.getSavedRecords()).isNotEmpty();
        assertThat(fileService.getSavedPath()).isEqualTo(Paths.get("data.json"));
        assertThat(fileService.getSavedFormat()).isEqualTo("json");
        assertThat(fileService.getSavedAppend()).isFalse();
    }

    @Test
    void stopJob_WhenOtherTasksExist_ShouldNotAffectThem() {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data1", false);
        scheduler.startScheduledFetch(List.of(source2), 1, 60, "json", "data2", false);
        assertThat(scheduler.getStatus()).hasSize(2);

        boolean stopped = scheduler.stopJob("data1");
        assertThat(stopped).isTrue();
        assertThat(scheduler.getStatus()).hasSize(1);
        assertThat(scheduler.getStatus()).containsKey("data2:src2");

        stopped = scheduler.stopJob("unknown");
        assertThat(stopped).isFalse();
        assertThat(scheduler.getStatus()).hasSize(1); // задачи не изменились
    }

    @Test
    void stopJob_WhenSchedulerAlreadyShutdown_ShouldNotShutdownAgain() {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data", false);
        scheduler.stopJob("data");
        boolean stopped = scheduler.stopJob("data");
        assertThat(stopped).isFalse();
    }

    @Test
    void stopJob_WhenSchedulerNull_ShouldNotShutdown() {
        assertThat(scheduler.getStatus()).isEmpty();
        boolean stopped = scheduler.stopJob("data");
        assertThat(stopped).isFalse();
    }

    @Test
    void stopJob_WhenTasksRemain_ShouldNotShutdownScheduler() {
        scheduler.startScheduledFetch(List.of(source1, source2), 1, 60, "json", "multi", false);
        assertThat(scheduler.getStatus()).hasSize(2);
        boolean stopped = scheduler.stopJob("multi");
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data1", false);
        scheduler.startScheduledFetch(List.of(source2), 1, 60, "json", "data2", false);
        assertThat(scheduler.getStatus()).hasSize(2);
        stopped = scheduler.stopJob("data1");
        assertThat(stopped).isTrue();
        assertThat(scheduler.getStatus()).hasSize(1);
        boolean restarted = scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data3", false);
        assertThat(restarted).isTrue();
    }

    @Test
    void stopJob_WhenFutureIsNull_ShouldHandleGracefully() throws Exception {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data", false);
        assertThat(scheduler.getStatus()).hasSize(1);

        Field tasksField = DataFetchScheduler.class.getDeclaredField("tasks");
        tasksField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ScheduledFuture<?>> originalTasks = (Map<String, ScheduledFuture<?>>) tasksField.get(scheduler);

        Map<String, ScheduledFuture<?>> mockTasks = new HashMap<>(originalTasks);
        tasksField.set(scheduler, mockTasks);

        String key = mockTasks.keySet().iterator().next();
        mockTasks.put(key, null);

        boolean stopped = scheduler.stopJob("data");
        assertThat(stopped).isTrue();
        assertThat(scheduler.getStatus()).isEmpty();

        tasksField.set(scheduler, originalTasks);
    }

    // ---------------------- getStatus ----------------------

    @Test
    void getStatus_ReturnsActiveTasks() {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data", false);
        var status = scheduler.getStatus();
        assertThat(status).hasSize(1);
        assertThat(status.values().iterator().next()).isTrue();
    }

    @Test
    void getStatus_AfterStopJob_DoesNotContainRemovedTasks() {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data", false);
        scheduler.stopJob("data");
        assertThat(scheduler.getStatus()).isEmpty();
    }

    @Test
    void getStatus_WhenFutureIsDone_ReturnsFalse() throws Exception {
        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        when(mockFuture.isDone()).thenReturn(true);
        when(mockFuture.isCancelled()).thenReturn(false);

        Field tasksField = DataFetchScheduler.class.getDeclaredField("tasks");
        tasksField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ScheduledFuture<?>> tasks = (Map<String, ScheduledFuture<?>>) tasksField.get(scheduler);
        tasks.clear();
        tasks.put("test:src", mockFuture);

        Map<String, Boolean> status = scheduler.getStatus();
        assertThat(status).containsEntry("test:src", false);
    }

    @Test
    void getStatus_WhenFutureIsCancelledButNotDone_ReturnsFalse() throws Exception {
        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        when(mockFuture.isDone()).thenReturn(false);
        when(mockFuture.isCancelled()).thenReturn(true);

        Field tasksField = DataFetchScheduler.class.getDeclaredField("tasks");
        tasksField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ScheduledFuture<?>> tasks = (Map<String, ScheduledFuture<?>>) tasksField.get(scheduler);
        tasks.clear();
        tasks.put("test:src", mockFuture);

        Map<String, Boolean> status = scheduler.getStatus();
        assertThat(status).containsEntry("test:src", false);
    }

    @Test
    void getStatus_WhenFutureIsCancelled_ReturnsFalse() throws Exception {
        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        when(mockFuture.isDone()).thenReturn(true);
        when(mockFuture.isCancelled()).thenReturn(true);

        Field tasksField = DataFetchScheduler.class.getDeclaredField("tasks");
        tasksField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ScheduledFuture<?>> tasks = (Map<String, ScheduledFuture<?>>) tasksField.get(scheduler);
        tasks.clear();
        tasks.put("test:src", mockFuture);

        Map<String, Boolean> status = scheduler.getStatus();
        assertThat(status).containsEntry("test:src", false);
    }

    // ---------------------- shutdown (PreDestroy) ----------------------

    @Test
    void shutdown_ShouldStopAllTasksAndShutdownScheduler() {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "data", false);
        assertThat(scheduler.getStatus()).isNotEmpty();
        scheduler.shutdown();
        assertThat(scheduler.getStatus()).isEmpty();
        // Повторный запуск должен пересоздать scheduler
        boolean restarted = scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", "newdata", false);
        assertThat(restarted).isTrue();
    }

    // ---------------------- parameterized tests ----------------------

    @ParameterizedTest
    @ValueSource(strings = {"data1", "data2"})
    void stopJob_WithDifferentFilenames_ShouldRemoveTask(String filename) {
        scheduler.startScheduledFetch(List.of(source1), 1, 60, "json", filename, false);
        boolean stopped = scheduler.stopJob(filename);
        assertThat(stopped).isTrue();
        assertThat(scheduler.getStatus()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "60, json, data1",
            "30, csv, data2"
    })
    void startScheduledFetch_WithVariousParams_ShouldCreateTask(int intervalSec, String format, String filename) {
        boolean result = scheduler.startScheduledFetch(List.of(source1), 1, intervalSec, format, filename, false);
        assertThat(result).isTrue();
        assertThat(scheduler.getStatus()).hasSize(1);
    }
}