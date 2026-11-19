package com.iotsensor.ingestion;

import com.iotsensor.ingestion.dependency.TestPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
abstract class IngestionIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestPostgres::jdbcUrl);
        registry.add("spring.datasource.username", TestPostgres::username);
        registry.add("spring.datasource.password", TestPostgres::password);
    }

    /**
     * Before, not after each test: a test always starts empty, and a failed test's data is left for inspection.
     * Tables are looked up, so new migrations need no change here.
     */
    @BeforeEach
    void truncateTables() {
        var tables = jdbcClient.sql("""
                SELECT string_agg(format('%I', tablename), ', ')
                FROM pg_tables
                WHERE schemaname = current_schema() AND tablename <> 'flyway_schema_history'
                """)
            .query(String.class)
            .optional();
        tables.ifPresent(names -> jdbcClient.sql("TRUNCATE TABLE " + names + " RESTART IDENTITY CASCADE").update());
    }

}
