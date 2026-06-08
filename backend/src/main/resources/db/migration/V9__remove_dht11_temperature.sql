DELETE m FROM measurements m
JOIN sensors s ON m.sensor_id = s.id
WHERE s.sensor_type = 'DHT11' AND m.metric = 'TEMPERATURE';
