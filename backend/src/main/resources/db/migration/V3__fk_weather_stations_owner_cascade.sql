ALTER TABLE weather_stations
    DROP FOREIGN KEY fk_weather_stations_owner;

ALTER TABLE weather_stations
    ADD CONSTRAINT fk_weather_stations_owner
        FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE;
