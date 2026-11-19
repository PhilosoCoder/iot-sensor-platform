package com.iotsensor.ingestion.exception;

import java.time.Instant;

public class InvalidTimeRangeException extends RuntimeException {

    public InvalidTimeRangeException(Instant from, Instant to) {
        super("'from' (%s) must be before 'to' (%s)".formatted(from, to));
    }

}
