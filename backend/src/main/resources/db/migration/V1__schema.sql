CREATE TABLE IF NOT EXISTS users
(
    id            INT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email)
);

CREATE TABLE IF NOT EXISTS refresh_tokens
(
    id          INT          NOT NULL AUTO_INCREMENT,
    user_id     INT          NOT NULL,
    token       VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_tokens_token (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS weather_stations
(
    id           INT          NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    owner_id     INT          NOT NULL,
    location_lat DECIMAL(10, 8),
    location_lon DECIMAL(11, 8),
    api_key      VARCHAR(64)  NOT NULL,
    gateway_url  VARCHAR(255),
    status       VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE',
    PRIMARY KEY (id),
    UNIQUE KEY uq_weather_stations_api_key (api_key),
    CONSTRAINT fk_weather_stations_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS nodes
(
    id                        INT         NOT NULL AUTO_INCREMENT,
    serial_number             VARCHAR(64) NOT NULL,
    display_name              VARCHAR(100),
    station_id                INT         NOT NULL,
    status                    VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    last_seen                 TIMESTAMP,
    reporting_interval_seconds INT        NOT NULL DEFAULT 86400,
    PRIMARY KEY (id),
    UNIQUE KEY uq_nodes_serial_number (serial_number),
    CONSTRAINT fk_nodes_station FOREIGN KEY (station_id) REFERENCES weather_stations (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sensors
(
    id          INT         NOT NULL AUTO_INCREMENT,
    node_id     INT         NOT NULL,
    sensor_type VARCHAR(50) NOT NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_sensors_node FOREIGN KEY (node_id) REFERENCES nodes (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS measurements
(
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    sensor_id   INT            NOT NULL,
    metric      VARCHAR(30)    NOT NULL,
    value       DECIMAL(12, 4) NOT NULL,
    measured_at TIMESTAMP      NOT NULL,
    received_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_measurements_sensor FOREIGN KEY (sensor_id) REFERENCES sensors (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS alarm_rules
(
    id               INT            NOT NULL AUTO_INCREMENT,
    name             VARCHAR(100),
    sensor_id        INT            NOT NULL,
    metric           VARCHAR(30)    NOT NULL,
    threshold_min    DECIMAL(12, 4),
    threshold_max    DECIMAL(12, 4),
    severity         VARCHAR(20)    NOT NULL DEFAULT 'WARNING',
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    cooldown_seconds INT            NOT NULL DEFAULT 0,
    last_fired_at    TIMESTAMP      NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_alarm_rules_sensor FOREIGN KEY (sensor_id) REFERENCES sensors (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS alarm_notifications
(
    id               INT       NOT NULL AUTO_INCREMENT,
    rule_id          INT       NOT NULL,
    measurement_id   BIGINT    NOT NULL,
    notified_user_id INT       NOT NULL,
    message          TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_alarm_notifications_rule        FOREIGN KEY (rule_id)        REFERENCES alarm_rules (id)  ON DELETE CASCADE,
    CONSTRAINT fk_alarm_notifications_measurement FOREIGN KEY (measurement_id) REFERENCES measurements (id) ON DELETE CASCADE,
    CONSTRAINT fk_alarm_notifications_user        FOREIGN KEY (notified_user_id) REFERENCES users (id)      ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS alarm_notification_reads
(
    notification_id INT       NOT NULL,
    user_id         INT       NOT NULL,
    read_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id, user_id),
    CONSTRAINT fk_anr_notification FOREIGN KEY (notification_id) REFERENCES alarm_notifications (id) ON DELETE CASCADE,
    CONSTRAINT fk_anr_user         FOREIGN KEY (user_id)         REFERENCES users (id)               ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS node_commands
(
    id           INT         NOT NULL AUTO_INCREMENT,
    node_id      INT         NOT NULL,
    command      VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payload      VARCHAR(255)         NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP            NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_node_commands_node FOREIGN KEY (node_id) REFERENCES nodes (id) ON DELETE CASCADE
);

CREATE INDEX idx_measurements_sensor_metric_time
    ON measurements (sensor_id, metric, measured_at);

CREATE INDEX idx_nodes_status_last_seen
    ON nodes (status, last_seen);

CREATE INDEX idx_alarm_notifications_user_time
    ON alarm_notifications (notified_user_id, created_at);

CREATE INDEX idx_node_commands_node_status
    ON node_commands (node_id, status);

CREATE INDEX idx_alarm_rules_sensor_metric_active
    ON alarm_rules (sensor_id, metric, is_active);
