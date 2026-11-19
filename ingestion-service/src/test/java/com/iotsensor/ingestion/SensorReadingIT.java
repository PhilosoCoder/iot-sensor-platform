package com.iotsensor.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Every test uses its own sensor IDs: the database is shared by all integration tests.
 */
class SensorReadingIT extends IngestionIT {

    @Test
    @SneakyThrows
    void postedReadingsAreReturnedNewestFirst() {
        postReading("it-sensor-1", 20.0, "2026-11-16T10:00:00Z");
        postReading("it-sensor-1", 22.0, "2026-11-16T10:10:00Z");
        postReading("it-sensor-1", 21.0, "2026-11-16T10:05:00Z");
        postReading("it-sensor-2", 99.0, "2026-11-16T10:30:00Z");

        mockMvc.perform(get("/v1/sensors/it-sensor-1/readings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.content[0].value").value(22.0))
            .andExpect(jsonPath("$.content[1].value").value(21.0))
            .andExpect(jsonPath("$.content[2].value").value(20.0))
            .andExpect(jsonPath("$.page.size").value(50))
            .andExpect(jsonPath("$.page.number").value(0))
            .andExpect(jsonPath("$.page.totalElements").value(3))
            .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    @SneakyThrows
    void postedReadingWithoutMeasuredAtGetsOne() {
        mockMvc.perform(post("/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sensorId": "it-sensor-3", "value": 5.5}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.measuredAt").isNotEmpty());
    }

    @Test
    @SneakyThrows
    void postAndGetReturnTheSameMeasuredAt() {
        mockMvc.perform(post("/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sensorId": "it-sensor-4", "value": 1.0, "measuredAt": "2026-11-16T10:15:30.123456789Z"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.measuredAt").value("2026-11-16T10:15:30.123456Z"));

        mockMvc.perform(get("/v1/sensors/it-sensor-4/readings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].measuredAt").value("2026-11-16T10:15:30.123456Z"));
    }

    @Test
    @SneakyThrows
    void readingsCanBeFilteredByHalfOpenTimeRange() {
        postReading("it-sensor-5", 1.0, "2026-11-16T09:00:00Z");
        postReading("it-sensor-5", 2.0, "2026-11-16T10:00:00Z");
        postReading("it-sensor-5", 3.0, "2026-11-16T10:30:00Z");
        postReading("it-sensor-5", 4.0, "2026-11-16T11:00:00Z");

        // from is inclusive, to is exclusive
        mockMvc.perform(get("/v1/sensors/it-sensor-5/readings")
                .param("from", "2026-11-16T10:00:00Z")
                .param("to", "2026-11-16T11:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].value").value(3.0))
            .andExpect(jsonPath("$.content[1].value").value(2.0))
            .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(get("/v1/sensors/it-sensor-5/readings").param("from", "2026-11-16T10:30:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].value").value(4.0))
            .andExpect(jsonPath("$.content[1].value").value(3.0));

        mockMvc.perform(get("/v1/sensors/it-sensor-5/readings").param("to", "2026-11-16T10:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].value").value(1.0));
    }

    @Test
    @SneakyThrows
    void pagesSplitReadingsAtTheRightBoundaries() {
        for (var minute = 0; minute < 5; minute++) {
            postReading("it-sensor-7", minute, "2026-11-16T10:0%d:00Z".formatted(minute));
        }

        mockMvc.perform(get("/v1/sensors/it-sensor-7/readings").param("page", "0").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].value", contains(4.0, 3.0)))
            .andExpect(jsonPath("$.page.size").value(2))
            .andExpect(jsonPath("$.page.number").value(0))
            .andExpect(jsonPath("$.page.totalElements").value(5))
            .andExpect(jsonPath("$.page.totalPages").value(3));

        mockMvc.perform(get("/v1/sensors/it-sensor-7/readings").param("page", "1").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].value", contains(2.0, 1.0)))
            .andExpect(jsonPath("$.page.number").value(1));

        // Last page is partial
        mockMvc.perform(get("/v1/sensors/it-sensor-7/readings").param("page", "2").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].value", contains(0.0)))
            .andExpect(jsonPath("$.page.number").value(2));

        // Past the end: empty content, totals still reported
        mockMvc.perform(get("/v1/sensors/it-sensor-7/readings").param("page", "3").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.page.totalElements").value(5));
    }

    @Test
    @SneakyThrows
    void pagingCombinesWithTimeRange() {
        for (var minute = 0; minute < 5; minute++) {
            postReading("it-sensor-8", minute, "2026-11-16T10:0%d:00Z".formatted(minute));
        }

        // [10:01, 10:04) -> readings 3, 2, 1; second page of size 2 -> only 1
        mockMvc.perform(get("/v1/sensors/it-sensor-8/readings")
                .param("from", "2026-11-16T10:01:00Z")
                .param("to", "2026-11-16T10:04:00Z")
                .param("page", "1")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].value", contains(1.0)))
            .andExpect(jsonPath("$.page.totalElements").value(3))
            .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    @SneakyThrows
    void readingsWithTheSameTimestampArePagedWithoutDuplicatesOrGaps() {
        for (var i = 0; i < 4; i++) {
            postReading("it-sensor-9", i, "2026-11-16T10:00:00Z");
        }

        var ids = new ArrayList<String>();
        for (var page = 0; page < 4; page++) {
            var json = mockMvc.perform(get("/v1/sensors/it-sensor-9/readings").param("page", "" + page).param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            ids.add(JsonPath.read(json, "$.content[0].id"));
        }

        assertThat(new HashSet<>(ids)).hasSize(4);
        // The id tie-break means ids come in descending order within the same timestamp. PostgreSQL compares
        // uuids byte-wise, which matches the lowercase hex string order (unlike java.util.UUID#compareTo).
        assertThat(ids).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @SneakyThrows
    void clientSuppliedSortIsIgnored() {
        postReading("it-sensor-10", 1.0, "2026-11-16T10:00:00Z");
        postReading("it-sensor-10", 2.0, "2026-11-16T10:01:00Z");

        mockMvc.perform(get("/v1/sensors/it-sensor-10/readings").param("sort", "measuredAt,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].value", contains(2.0, 1.0)));
    }

    @Test
    @SneakyThrows
    void pageSizeIsCappedAt500() {
        mockMvc.perform(get("/v1/sensors/it-sensor-11/readings").param("size", "10000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(500));
    }

    @Test
    @SneakyThrows
    void invalidTimeRangeIsRejected() {
        mockMvc.perform(get("/v1/sensors/it-sensor-5/readings")
                .param("from", "2026-11-16T10:00:00Z")
                .param("to", "2026-11-16T10:00:00Z"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errors[0].field").value("from"))
            .andExpect(jsonPath("$.errors[0].message").value("must be before 'to'"));
    }

    @Test
    @SneakyThrows
    void invalidReadingIsRejectedAndNotStored() {
        mockMvc.perform(post("/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sensorId": "it-sensor-6"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.instance").value("/v1/readings"))
            .andExpect(jsonPath("$.errors[0].field").value("value"))
            .andExpect(jsonPath("$.errors[0].message").value("must not be null"));

        mockMvc.perform(get("/v1/sensors/it-sensor-6/readings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @SneakyThrows
    void sensorsAreListedAsPage() {
        mockMvc.perform(get("/v1/sensors").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page.size").value(10))
            .andExpect(jsonPath("$.page.number").value(0));
    }

    @SneakyThrows
    private void postReading(String sensorId, double value, String measuredAt) {
        mockMvc.perform(post("/v1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sensorId": "%s", "value": %s, "measuredAt": "%s"}
                    """.formatted(sensorId, value, measuredAt)))
            .andExpect(status().isCreated());
    }

}
