package com.example.meteonode.repository;

import com.example.meteonode.model.entity.Measurement;
import com.example.meteonode.model.enums.Metric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {
    Page<Measurement> findBySensorIdAndMetricAndMeasuredAtBetween(Integer sensorId, Metric metric, Instant from, Instant to, Pageable pageable);
    Optional<Measurement> findTopBySensorIdAndMetricOrderByMeasuredAtDesc(Integer sensorId, Metric metric);

    @Query("SELECT m FROM Measurement m WHERE m.sensor.id = :sensorId AND m.measuredAt = " +
           "(SELECT MAX(m2.measuredAt) FROM Measurement m2 WHERE m2.sensor.id = :sensorId AND m2.metric = m.metric)")
    List<Measurement> findLatestPerMetricBySensorId(@Param("sensorId") Integer sensorId);

    @Query("SELECT m FROM Measurement m WHERE m.sensor.id IN :sensorIds AND m.measuredAt = " +
           "(SELECT MAX(m2.measuredAt) FROM Measurement m2 WHERE m2.sensor.id = m.sensor.id AND m2.metric = m.metric)")
    List<Measurement> findLatestPerMetricBySensorIds(@Param("sensorIds") List<Integer> sensorIds);
}
