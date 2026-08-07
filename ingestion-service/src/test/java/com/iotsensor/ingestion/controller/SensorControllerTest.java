package com.iotsensor.ingestion.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.iotsensor.ingestion.dto.SensorDto;
import com.iotsensor.ingestion.service.SensorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SensorController.class)
class SensorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorService sensorService;

    @Test
    void listSensorsReturnsSensors() throws Exception {
        var sensors = List.of(
            new SensorDto("sensor-1", "Temperature Sensor", "temperature", "warehouse", "celsius", true),
            new SensorDto("sensor-2", "Humidity Sensor", "humidity", "office", "percent", false)
        );

        when(sensorService.listSensors()).thenReturn(sensors);

        mockMvc.perform(get("/v1/sensors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("sensor-1"))
            .andExpect(jsonPath("$[0].name").value("Temperature Sensor"))
            .andExpect(jsonPath("$[0].type").value("temperature"))
            .andExpect(jsonPath("$[0].location").value("warehouse"))
            .andExpect(jsonPath("$[0].unit").value("celsius"))
            .andExpect(jsonPath("$[0].active").value(true))
            .andExpect(jsonPath("$[1].id").value("sensor-2"))
            .andExpect(jsonPath("$[1].active").value(false));

        verify(sensorService).listSensors();
    }

}
