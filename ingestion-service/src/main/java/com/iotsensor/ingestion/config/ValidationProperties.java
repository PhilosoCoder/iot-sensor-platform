package com.iotsensor.ingestion.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param clockDriftTolerance how far in the future a timestamp may be and still count as
 *                            "past or present", to absorb device clock drift
 */
@ConfigurationProperties("ingestion.validation")
public record ValidationProperties(@DefaultValue("5m") Duration clockDriftTolerance) {

}
