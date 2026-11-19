package com.iotsensor.ingestion.dependency;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * One PostgreSQL container per JVM, shared by every Spring context. Keep {@link #IMAGE} in sync with
 * k8s/infrastructure/postgresql/statefulset.yaml.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestPostgres {

    private static final String IMAGE = "postgres:18.7";

    private static final PostgreSQLContainer container = new PostgreSQLContainer(DockerImageName.parse(IMAGE));

    static {
        container.start();
    }

    public static PostgreSQLContainer container() {
        return container;
    }

    public static String jdbcUrl() {
        return container.getJdbcUrl();
    }

    public static String username() {
        return container.getUsername();
    }

    public static String password() {
        return container.getPassword();
    }

}
