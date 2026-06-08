CREATE TABLE IF NOT EXISTS gateways
(
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    api_key     VARCHAR(64)  NOT NULL,
    gateway_url VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uq_gateways_api_key (api_key)
);

INSERT INTO gateways (name, api_key)
VALUES ('Main Gateway', 'esp32-test-key-001');

ALTER TABLE nodes
    DROP FOREIGN KEY fk_nodes_station;

ALTER TABLE nodes
    MODIFY COLUMN station_id INT NULL;

ALTER TABLE nodes
    ADD CONSTRAINT fk_nodes_station
        FOREIGN KEY (station_id) REFERENCES weather_stations (id) ON DELETE SET NULL;

ALTER TABLE weather_stations
    DROP KEY uq_weather_stations_api_key;

ALTER TABLE weather_stations
    DROP COLUMN api_key,
    DROP COLUMN gateway_url;
