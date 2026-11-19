package com.iotsensor.ingestion.config;

import org.hibernate.validator.HibernateValidatorConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ValidationProperties.class)
public class ValidationConfig {

    /**
     * Applies the clock-drift tolerance to @Past/@PastOrPresent/@Future/@FutureOrPresent.
     * The customized validator is also the one Spring MVC uses for @Valid and method validation.
     */
    @Bean
    ValidationConfigurationCustomizer clockDriftToleranceCustomizer(ValidationProperties properties) {
        var toleranceMillis = properties.clockDriftTolerance().toMillis();
        return configuration -> configuration.addProperty(
            HibernateValidatorConfiguration.TEMPORAL_VALIDATION_TOLERANCE, String.valueOf(toleranceMillis));
    }

}
