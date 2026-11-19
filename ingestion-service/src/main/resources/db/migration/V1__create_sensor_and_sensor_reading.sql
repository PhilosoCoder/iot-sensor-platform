CREATE TABLE sensor (
    id       VARCHAR(64)  PRIMARY KEY,
    name     VARCHAR(255),
    type     VARCHAR(255),
    location VARCHAR(255),
    unit     VARCHAR(255),
    active   BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE sensor_reading (
    id          UUID             PRIMARY KEY,
    sensor_id   VARCHAR(64)      NOT NULL,
    value       DOUBLE PRECISION NOT NULL,
    measured_at TIMESTAMPTZ      NOT NULL
);

CREATE INDEX idx_sensor_reading_sensor_id_measured_at
    ON sensor_reading (sensor_id, measured_at DESC, id DESC);
