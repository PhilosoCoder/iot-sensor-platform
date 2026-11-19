package com.iotsensor.ingestion.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.iotsensor.ingestion.dto.SensorDto;
import com.iotsensor.ingestion.service.SensorService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(SensorController.class)
class SensorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorService sensorService;

    @Test
    @SneakyThrows
    void listSensorsReturnsPageOfSensors() {
        var sensors = List.of(
            new SensorDto("sensor-1", "Temperature Sensor", "temperature", "warehouse", "celsius", true),
            new SensorDto("sensor-2", "Humidity Sensor", "humidity", "office", "percent", false)
        );

        when(sensorService.listSensors(any(Pageable.class))).thenReturn(new PageImpl<>(sensors, PageRequest.of(0, 50), 2));

        mockMvc.perform(get("/v1/sensors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("sensor-1"))
            .andExpect(jsonPath("$.content[0].name").value("Temperature Sensor"))
            .andExpect(jsonPath("$.content[0].type").value("temperature"))
            .andExpect(jsonPath("$.content[0].location").value("warehouse"))
            .andExpect(jsonPath("$.content[0].unit").value("celsius"))
            .andExpect(jsonPath("$.content[0].active").value(true))
            .andExpect(jsonPath("$.content[1].id").value("sensor-2"))
            .andExpect(jsonPath("$.content[1].active").value(false))
            .andExpect(jsonPath("$.page.size").value(50))
            .andExpect(jsonPath("$.page.number").value(0))
            .andExpect(jsonPath("$.page.totalElements").value(2))
            .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    @SneakyThrows
    void listSensorsUsesDefaultPageSizeOf50() {
        var pageable = performAndCapturePageable(get("/v1/sensors"));

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    @Test
    @SneakyThrows
    void listSensorsPassesRequestedPageAndCapsSizeAt500() {
        var pageable = performAndCapturePageable(get("/v1/sensors").param("page", "2").param("size", "501"));

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(500);
    }

    @SneakyThrows
    private Pageable performAndCapturePageable(MockHttpServletRequestBuilder request) {
        when(sensorService.listSensors(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(request).andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorService).listSensors(captor.capture());
        return captor.getValue();
    }

}
