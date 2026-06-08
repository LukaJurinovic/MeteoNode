INSERT INTO weather_stations (name, owner_id, location_lat, location_lon, status)
VALUES
    ('Zagreb Weather Hub',       1, 45.81500000, 15.98200000, 'ONLINE'),
    ('Split Coastal Monitor',    1, 43.50810000, 16.44020000, 'ONLINE'),
    ('Risnjak Mountain Station', 1, 45.59290000, 14.50800000, 'ONLINE');


INSERT INTO nodes (serial_number, display_name, station_id, status, last_seen, reporting_interval_seconds)
SELECT 'NODE-ZG-01', 'Zagreb Hub Node 1', id, 'ONLINE', NOW(), 86400
FROM weather_stations WHERE name = 'Zagreb Weather Hub';

INSERT INTO nodes (serial_number, display_name, station_id, status, last_seen, reporting_interval_seconds)
SELECT 'NODE-ZG-02', 'Zagreb Hub Node 2', id, 'ONLINE', NOW(), 86400
FROM weather_stations WHERE name = 'Zagreb Weather Hub';

INSERT INTO nodes (serial_number, display_name, station_id, status, last_seen, reporting_interval_seconds)
SELECT 'NODE-SPL-01', 'Coastal Node 1', id, 'ONLINE', NOW(), 86400
FROM weather_stations WHERE name = 'Split Coastal Monitor';

INSERT INTO nodes (serial_number, display_name, station_id, status, last_seen, reporting_interval_seconds)
SELECT 'NODE-SPL-02', 'Coastal Node 2', id, 'ONLINE', NOW(), 86400
FROM weather_stations WHERE name = 'Split Coastal Monitor';

INSERT INTO nodes (serial_number, display_name, station_id, status, last_seen, reporting_interval_seconds)
SELECT 'NODE-SPL-03', 'Coastal Node 3', id, 'ONLINE', NOW(), 86400
FROM weather_stations WHERE name = 'Split Coastal Monitor';

INSERT INTO nodes (serial_number, display_name, station_id, status, last_seen, reporting_interval_seconds)
SELECT 'NODE-MTN-01', 'Mountain Node 1', id, 'ONLINE', NOW(), 86400
FROM weather_stations WHERE name = 'Risnjak Mountain Station';


INSERT INTO sensors (node_id, sensor_type, is_active)
SELECT id, 'BMP280', true FROM nodes
WHERE serial_number IN ('NODE-ZG-01','NODE-ZG-02','NODE-SPL-01','NODE-SPL-02','NODE-SPL-03','NODE-MTN-01');

INSERT INTO sensors (node_id, sensor_type, is_active)
SELECT id, 'DHT11', true FROM nodes
WHERE serial_number IN ('NODE-ZG-01','NODE-ZG-02','NODE-SPL-01','NODE-SPL-02','NODE-SPL-03','NODE-MTN-01');

INSERT INTO sensors (node_id, sensor_type, is_active)
SELECT id, 'GUVA_S12SD', true FROM nodes
WHERE serial_number IN ('NODE-ZG-01','NODE-ZG-02','NODE-SPL-01','NODE-SPL-02','NODE-SPL-03','NODE-MTN-01');

INSERT INTO sensors (node_id, sensor_type, is_active)
SELECT id, 'GY_302', true FROM nodes
WHERE serial_number IN ('NODE-ZG-01','NODE-ZG-02','NODE-SPL-01','NODE-SPL-02','NODE-SPL-03','NODE-MTN-01');


INSERT INTO measurements (sensor_id, metric, value, measured_at)
SELECT
    s.id,
    'TEMPERATURE',
    ROUND(CASE ws.name
        WHEN 'Zagreb Weather Hub'       THEN 18.0 + 6.0*(d.n/30.0) + 2.5*SIN(2*PI()*d.n/7.0) + (nd.id%3)*0.40
        WHEN 'Split Coastal Monitor'    THEN 20.0 + 6.0*(d.n/30.0) + 2.5*SIN(2*PI()*d.n/7.0) + (nd.id%3)*0.40
        ELSE                                  7.0 + 5.0*(d.n/30.0) + 3.0*SIN(2*PI()*d.n/7.0) + (nd.id%2)*0.50
    END, 2),
    TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 30 DAY), INTERVAL d.n DAY), '12:00:00')
FROM sensors s
JOIN nodes nd ON s.node_id = nd.id
JOIN weather_stations ws ON nd.station_id = ws.id
CROSS JOIN (
    SELECT  0 AS n UNION ALL SELECT  1 UNION ALL SELECT  2 UNION ALL SELECT  3 UNION ALL
    SELECT  4        UNION ALL SELECT  5 UNION ALL SELECT  6 UNION ALL SELECT  7 UNION ALL
    SELECT  8        UNION ALL SELECT  9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL
    SELECT 12        UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL
    SELECT 16        UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL
    SELECT 20        UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL
    SELECT 24        UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL
    SELECT 28        UNION ALL SELECT 29 UNION ALL SELECT 30
) d
WHERE s.sensor_type = 'BMP280'
  AND nd.serial_number IN ('NODE-ZG-01','NODE-ZG-02','NODE-SPL-01','NODE-SPL-02','NODE-SPL-03','NODE-MTN-01');

INSERT INTO measurements (sensor_id, metric, value, measured_at)
SELECT
    s.id,
    'PRESSURE',
    ROUND(CASE ws.name
        WHEN 'Zagreb Weather Hub'       THEN 1018.5 + 4.5*SIN(2*PI()*d.n/10.0) + 1.5*SIN(2*PI()*d.n/5.0) + (nd.id%2)*0.30
        WHEN 'Split Coastal Monitor'    THEN 1016.5 + 4.0*SIN(2*PI()*d.n/10.0) + 1.5*SIN(2*PI()*d.n/5.0) + (nd.id%2)*0.30
        ELSE                              851.5 + 4.0*SIN(2*PI()*d.n/10.0) + 1.5*SIN(2*PI()*d.n/5.0)
    END, 2),
    TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 30 DAY), INTERVAL d.n DAY), '12:00:00')
FROM sensors s
JOIN nodes nd ON s.node_id = nd.id
JOIN weather_stations ws ON nd.station_id = ws.id
CROSS JOIN (
    SELECT  0 AS n UNION ALL SELECT  1 UNION ALL SELECT  2 UNION ALL SELECT  3 UNION ALL
    SELECT  4        UNION ALL SELECT  5 UNION ALL SELECT  6 UNION ALL SELECT  7 UNION ALL
    SELECT  8        UNION ALL SELECT  9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL
    SELECT 12        UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL
    SELECT 16        UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL
    SELECT 20        UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL
    SELECT 24        UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL
    SELECT 28        UNION ALL SELECT 29 UNION ALL SELECT 30
) d
WHERE s.sensor_type = 'BMP280'
  AND nd.serial_number IN ('NODE-ZG-01','NODE-ZG-02','NODE-SPL-01','NODE-SPL-02','NODE-SPL-03','NODE-MTN-01');

INSERT INTO measurements (sensor_id, metric, value, measured_at)
SELECT
    s.id,
    'TEMPERATURE',
    ROUND(CASE ws.name
        WHEN 'Zagreb Weather Hub'       THEN 19.0 + 6.0*(d.n/30.0) + 2.5*SIN(2*PI()*d.n/7.0) + (nd.id%3)*0.50
        WHEN 'Split Coastal Monitor'    THEN 21.0 + 6.0*(d.n/30.0) + 2.5*SIN(2*PI()*d.n/7.0) + (nd.id%3)*0.50
        ELSE                                  8.0 + 5.0*(d.n/30.0) + 3.0*SIN(2*PI()*d.n/7.0) + (nd.id%2)*0.50
    END, 2),
    TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 30 DAY), INTERVAL d.n DAY), '12:00:00')
FROM sensors s
JOIN nodes nd ON s.node_id = nd.id
JOIN weather_stations ws ON nd.station_id = ws.id
CROSS JOIN (
    SELECT  0 AS n UNION ALL SELECT  1 UNION ALL SELECT  2 UNION ALL SELECT  3 UNION ALL
    SELECT  4        UNION ALL SELECT  5 UNION ALL SELECT  6 UNION ALL SELECT  7 UNION ALL
    SELECT  8        UNION ALL SELECT  9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL
    SELECT 12        UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL
    SELECT 16        UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL
    SELECT 20        UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL
    SELECT 24        UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL
    SELECT 28        UNION ALL SELECT 29 UNION ALL SELECT 30
) d
WHERE s.sensor_type = 'DHT11'
  AND nd.serial_number IN ('NODE-ZG-01','NODE-ZG-02','NODE-SPL-01','NODE-SPL-02','NODE-SPL-03','NODE-MTN-01');

INSERT INTO measurements (sensor_id, metric, value, measured_at)
SELECT
    s.id,
    'HUMIDITY',
    LEAST(100.0, GREATEST(25.0, ROUND(CASE ws.name
        WHEN 'Zagreb Weather Hub'       THEN 58.0 - 10.0*(d.n/30.0) + 8.0*SIN(2*PI()*d.n/7.0) + (nd.id%3)*2.00
        WHEN 'Split Coastal Monitor'    THEN 65.0 - 10.0*(d.n/30.0) + 7.0*SIN(2*PI()*d.n/7.0) + (nd.id%3)*2.00
        ELSE                                  72.0 -  8.0*(d.n/30.0) + 8.0*SIN(2*PI()*d.n/7.0) + (nd.id%2)*3.00
    END, 2))),
    TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 30 DAY), INTERVAL d.n DAY), '12:00:00')
FROM sensors s
JOIN nodes nd ON s.node_id = nd.id
JOIN weather_stations ws ON nd.station_id = ws.id
CROSS JOIN (
    SELECT  0 AS n UNION ALL SELECT  1 UNION ALL SELECT  2 UNION ALL SELECT  3 UNION ALL
    SELECT  4        UNION ALL SELECT  5 UNION ALL SELECT  6 UNION ALL SELECT  7 UNION ALL
    SELECT  8        UNION ALL SELECT  9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL
    SELECT 12        UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL
    SELECT 16        UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL
    SELECT 20        UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL
    SELECT 24        UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL
    SELECT 28        UNION ALL SELECT 29 UNION ALL SELECT 30
) d
WHERE s.sensor_type = 'DHT11'
  AND nd.serial_number IN ('NODE-ZG-01','NODE-ZG-02','NODE-SPL-01','NODE-SPL-02','NODE-SPL-03','NODE-MTN-01');

INSERT INTO measurements (sensor_id, metric, value, measured_at)
SELECT
    s.id,
    'UV_INDEX',
    GREATEST(0.10, ROUND(CASE ws.name
        WHEN 'Zagreb Weather Hub'       THEN 4.0 + 2.0*(d.n/30.0) + 2.5*SIN(2*PI()*d.n/7.0) + (nd.id%2)*0.40
        WHEN 'Split Coastal Monitor'    THEN 5.5 + 2.0*(d.n/30.0) + 2.5*SIN(2*PI()*d.n/7.0) + (nd.id%3)*0.30
        ELSE                                  5.0 + 2.5*(d.n/30.0) + 3.0*SIN(2*PI()*d.n/7.0) + (nd.id%2)*0.50
    END, 2)),
    TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 30 DAY), INTERVAL d.n DAY), '12:00:00')
FROM sensors s
JOIN nodes nd ON s.node_id = nd.id
JOIN weather_stations ws ON nd.station_id = ws.id
CROSS JOIN (
    SELECT  0 AS n UNION ALL SELECT  1 UNION ALL SELECT  2 UNION ALL SELECT  3 UNION ALL
    SELECT  4        UNION ALL SELECT  5 UNION ALL SELECT  6 UNION ALL SELECT  7 UNION ALL
    SELECT  8        UNION ALL SELECT  9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL
    SELECT 12        UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL
    SELECT 16        UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL
    SELECT 20        UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL
    SELECT 24        UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL
    SELECT 28        UNION ALL SELECT 29 UNION ALL SELECT 30
) d
WHERE s.sensor_type = 'GUVA_S12SD'
  AND nd.serial_number IN ('NODE-ZG-01','NODE-ZG-02','NODE-SPL-01','NODE-SPL-02','NODE-SPL-03','NODE-MTN-01');

INSERT INTO measurements (sensor_id, metric, value, measured_at)
SELECT
    s.id,
    'ILLUMINANCE',
    GREATEST(300.0, ROUND(CASE ws.name
        WHEN 'Zagreb Weather Hub'       THEN 30000 + 16000*SIN(2*PI()*d.n/7.0) + 5000*(d.n/30.0) + (nd.id%2)*1500
        WHEN 'Split Coastal Monitor'    THEN 38000 + 18000*SIN(2*PI()*d.n/7.0) + 6000*(d.n/30.0) + (nd.id%3)*1000
        ELSE                                  25000 + 15000*SIN(2*PI()*d.n/7.0) + 5000*(d.n/30.0)
    END, 2)),
    TIMESTAMP(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 30 DAY), INTERVAL d.n DAY), '12:00:00')
FROM sensors s
JOIN nodes nd ON s.node_id = nd.id
JOIN weather_stations ws ON nd.station_id = ws.id
CROSS JOIN (
    SELECT  0 AS n UNION ALL SELECT  1 UNION ALL SELECT  2 UNION ALL SELECT  3 UNION ALL
    SELECT  4        UNION ALL SELECT  5 UNION ALL SELECT  6 UNION ALL SELECT  7 UNION ALL
    SELECT  8        UNION ALL SELECT  9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL
    SELECT 12        UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL
    SELECT 16        UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL
    SELECT 20        UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL
    SELECT 24        UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL
    SELECT 28        UNION ALL SELECT 29 UNION ALL SELECT 30
) d
WHERE s.sensor_type = 'GY_302'
  AND nd.serial_number IN ('NODE-ZG-01','NODE-ZG-02','NODE-SPL-01','NODE-SPL-02','NODE-SPL-03','NODE-MTN-01');
