package com.iotsensor.ingestion.exception;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Adds an {@code errors: [{field, message}]} list to 400 responses.
 * <p>
 * Runs before Spring Boot's ProblemDetailsExceptionHandler (spring.mvc.problemdetails.enabled), which
 * still handles every other exception. It deliberately does not extend ResponseEntityExceptionHandler:
 * that would make Boot back off and leave us responsible for every MVC exception.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

    static final String ERRORS_PROPERTY = "errors";

    /** Invalid {@code @Valid @RequestBody}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleInvalidBody(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getAllErrors().stream()
            .map(error -> new ValidationError(fieldName(error), error.getDefaultMessage()))
            .toList();
        return badRequest(ex.getBody(), errors);
    }

    /** Built-in method validation, e.g. constraints on {@code @PathVariable} / {@code @RequestParam}. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> handleInvalidParameters(HandlerMethodValidationException ex) {
        var errors = new ArrayList<ValidationError>();
        for (var result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors beanErrors) {
                beanErrors.getAllErrors().forEach(error ->
                    errors.add(new ValidationError(fieldName(error), error.getDefaultMessage())));
            } else {
                var parameterName = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(error ->
                    errors.add(new ValidationError(parameterName, error.getDefaultMessage())));
            }
        }
        return badRequest(ex.getBody(), errors);
    }

    /** Unparseable query/path parameter, e.g. {@code from=yesterday}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request parameter.");
        return badRequest(problem, List.of(new ValidationError(ex.getName(), "has an invalid format")));
    }

    @ExceptionHandler(InvalidTimeRangeException.class)
    ResponseEntity<ProblemDetail> handleInvalidTimeRange(InvalidTimeRangeException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid time range.");
        return badRequest(problem, List.of(new ValidationError("from", "must be before 'to'")));
    }

    private static ResponseEntity<ProblemDetail> badRequest(ProblemDetail problem, List<ValidationError> errors) {
        // Validation reports constraints in no particular order; sort for a stable response.
        var sorted = errors.stream()
            .sorted(Comparator.comparing(ValidationError::field, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ValidationError::message, Comparator.nullsFirst(Comparator.naturalOrder())))
            .toList();
        problem.setProperty(ERRORS_PROPERTY, sorted);
        return ResponseEntity.of(problem).build();
    }

    private static String fieldName(MessageSourceResolvable error) {
        // Object-level errors (class-level constraints) are not tied to a single field.
        return error instanceof FieldError fieldError ? fieldError.getField() : null;
    }

    public record ValidationError(String field, String message) {

    }

}
