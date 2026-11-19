package com.iotsensor.ingestion.config;

import java.util.Arrays;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;

/**
 * Spec at {@code /v3/api-docs}, Swagger UI at {@code /swagger-ui.html}.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI ingestionServiceOpenApi() {
        // Version of the API contract (matches the /v1 path prefix), not of the build.
        return new OpenAPI().info(new Info()
            .title("IoT Sensor Platform - Ingestion Service API")
            .description("Ingests sensor readings and serves them per sensor.")
            .version("v1"));
    }

    /**
     * {@code @ParameterObject Pageable} documents page, size and sort, but every paged endpoint applies its own
     * fixed order and ignores {@code sort}. Drop it so Swagger UI doesn't offer a parameter with no effect.
     * Revisit once an endpoint actually honours client-supplied sorting.
     */
    @Bean
    OperationCustomizer hideIgnoredSortParameter() {
        return (operation, handlerMethod) -> {
            var takesPageable = Arrays.stream(handlerMethod.getMethodParameters())
                .anyMatch(parameter -> Pageable.class.isAssignableFrom(parameter.getParameterType()));
            if (takesPageable && operation.getParameters() != null) {
                operation.getParameters().removeIf(parameter -> "query".equals(parameter.getIn())
                    && "sort".equals(parameter.getName()));
            }
            return operation;
        };
    }

}
