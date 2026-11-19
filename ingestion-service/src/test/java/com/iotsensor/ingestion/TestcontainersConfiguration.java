package com.iotsensor.ingestion;

import com.iotsensor.ingestion.dependency.TestPostgres;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * For bootTestRun only. Integration tests get the database via {@link IngestionIT}: as a bean, the container
 * would be stopped whenever a cached test context is closed, while other contexts still use it.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return TestPostgres.container();
    }

}
