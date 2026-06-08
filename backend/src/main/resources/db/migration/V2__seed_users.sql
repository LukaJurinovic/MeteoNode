INSERT INTO users (username, email, password_hash, role)
VALUES ('admin', 'admin@meteonode.local',
        '$2a$10$vMzckEnIISwNHmwmOzHznu89TFRYTU1f6rjgWsQUENzjB8ZXMdLCm',
        'ADMIN');

INSERT INTO users (username, email, password_hash, role)
VALUES ('operator', 'operator@meteonode.local',
        '$2a$10$vMzckEnIISwNHmwmOzHznu89TFRYTU1f6rjgWsQUENzjB8ZXMdLCm',
        'OPERATOR');

INSERT INTO users (username, email, password_hash, role)
VALUES ('viewer', 'viewer@meteonode.local',
        '$2a$10$vMzckEnIISwNHmwmOzHznu89TFRYTU1f6rjgWsQUENzjB8ZXMdLCm',
        'USER');

INSERT INTO weather_stations (name, owner_id, api_key, status)
VALUES ('ESP32 Test Station', 1, 'esp32-test-key-001', 'OFFLINE');
