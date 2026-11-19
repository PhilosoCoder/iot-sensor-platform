package com.iotsensor.ingestion.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ingestion.validation")
public record ValidationProperties(@DefaultValue("5m") Duration clockDriftTolerance) {

}
