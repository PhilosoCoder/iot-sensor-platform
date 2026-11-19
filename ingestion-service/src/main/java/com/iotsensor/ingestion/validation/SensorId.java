package com.iotsensor.ingestion.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A sensor ID as accepted by the API: same rules for request bodies and path variables.
 * The max length matches the VARCHAR(64) columns in the database.
 * Each violated rule is reported separately, with its own message.
 */
@NotBlank
@Size(max = SensorId.MAX_LENGTH, message = "must be at most {max} characters")
@Pattern(regexp = SensorId.PATTERN, message = "may only contain letters, digits, '.', '_' and '-'")
@Documented
@Retention(RUNTIME)
@Target({FIELD, PARAMETER})
@Constraint(validatedBy = {})
public @interface SensorId {

    int MAX_LENGTH = 64;

    String PATTERN = "[A-Za-z0-9._-]+";

    String message() default "invalid sensor ID";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
