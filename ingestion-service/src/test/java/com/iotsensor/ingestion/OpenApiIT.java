package com.iotsensor.ingestion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class OpenApiIT extends IngestionIT {

    @Test
    @SneakyThrows
    void apiDocsDescribeTheReadingsEndpoints() {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("IoT Sensor Platform - Ingestion Service API"))
            .andExpect(jsonPath("$.info.version").value("v1"))
            .andExpect(jsonPath("$.paths['/v1/sensors/{sensorId}/readings'].get").exists())
            .andExpect(jsonPath("$.paths['/v1/readings'].post").exists());
    }

    @Test
    @SneakyThrows
    void pageableIsDocumentedAsPageAndSizeQueryParameters() {
        var parameters = "$.paths['/v1/sensors/{sensorId}/readings'].get.parameters";
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(parameters + "[?(@.name == 'page')].in").value("query"))
            .andExpect(jsonPath(parameters + "[?(@.name == 'size')].in").value("query"))
            // Taken from spring.data.web.pageable.default-page-size
            .andExpect(jsonPath(parameters + "[?(@.name == 'size')].schema.default").value(50))
            // Without @ParameterObject the whole Pageable would show up as one object parameter
            .andExpect(jsonPath(parameters + "[?(@.name == 'pageable')]").isEmpty())
            // Ignored by the endpoint, so not advertised
            .andExpect(jsonPath(parameters + "[?(@.name == 'sort')]").isEmpty());
    }

    @Test
    @SneakyThrows
    void swaggerUiIsServed() {
        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/swagger-ui/index.html"));

        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
    }

}
