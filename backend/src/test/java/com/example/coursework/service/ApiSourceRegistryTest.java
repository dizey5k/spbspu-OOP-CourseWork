package com.example.coursework.service;

import com.example.coursework.model.AggregatedRecord;
import com.example.coursework.model.ApiSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ApiSourceRegistryTest {

    private ApiSource sourceA = new ApiSource() {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public String getDisplayName() {
            return "";
        }

        public String name() { return "a"; }
        public String displayName() { return "A"; }

        @Override
        public List<AggregatedRecord> fetchData() throws IOException {
            return List.of();
        }
    };

    private ApiSource sourceB = new ApiSource() {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public String getDisplayName() {
            return "";
        }

        public String name() { return "b"; }
        public String displayName() { return "B"; }

        @Override
        public List<AggregatedRecord> fetchData() throws IOException {
            return List.of();
        }
    };

    @Test
    void registryShouldMapByName() {
        ApiSourceRegistry registry = new ApiSourceRegistry(List.of(sourceA, sourceB));

        assertThat(registry.getSource("a")).isEqualTo(sourceA);
        assertThat(registry.getSource("b")).isEqualTo(sourceB);
        assertThat(registry.getSource("c")).isNull();
    }

    @Test
    void getSourcesByNameShouldReturnExisting() {
        ApiSourceRegistry registry = new ApiSourceRegistry(List.of(sourceA, sourceB));

        List<ApiSource> found = registry.getSourcesByName(List.of("a", "c", "b"));
        assertThat(found).containsExactly(sourceA, sourceB);
    }

    @Test
    void getAllSourcesReturnsAll() {
        ApiSourceRegistry registry = new ApiSourceRegistry(List.of(sourceA, sourceB));

        assertThat(registry.getAllSources()).containsExactlyInAnyOrder(sourceA, sourceB);
    }

    @ParameterizedTest
    @CsvSource({
            "a, a",
            "b, b",
            "c, null"
    })
    void getSource_ShouldReturnCorrect(String name, String expectedName) {
        ApiSourceRegistry registry = new ApiSourceRegistry(List.of(sourceA, sourceB));
        ApiSource result = registry.getSource(name);
        if ("null".equals(expectedName)) {
            assertThat(result).isNull();
        } else {
            assertThat(result.name()).isEqualTo(expectedName);
        }
    }
}