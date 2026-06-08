package com.example.meteonode.service.domain;

import com.example.meteonode.mapper.MeasurementMapper;
import com.example.meteonode.model.dto.response.MeasurementDTO;
import com.example.meteonode.model.entity.Measurement;
import com.example.meteonode.model.entity.Sensor;
import com.example.meteonode.model.enums.Metric;
import com.example.meteonode.repository.MeasurementRepository;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final MeasurementMapper measurementMapper;

    @Transactional(readOnly = true)
    public Optional<MeasurementDTO> findLatest(Integer sensorId, Metric metric) {
        return measurementRepository.findTopBySensorIdAndMetricOrderByMeasuredAtDesc(sensorId, metric)
                .map(measurementMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<MeasurementDTO> findByRange(Integer sensorId, Metric metric, Instant from, Instant to, Pageable pageable) {
        return measurementRepository.findBySensorIdAndMetricAndMeasuredAtBetween(sensorId, metric, from, to, pageable)
                .map(measurementMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<MeasurementDTO> findLatestPerMetricBySensorId(Integer sensorId) {
        return measurementRepository.findLatestPerMetricBySensorId(sensorId).stream()
                .map(measurementMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeasurementDTO> findLatestPerMetricBySensorIds(List<Integer> sensorIds) {
        if (sensorIds.isEmpty()) return List.of();
        return measurementRepository.findLatestPerMetricBySensorIds(sensorIds).stream()
                .map(measurementMapper::toDTO)
                .toList();
    }

    @Transactional
    public MeasurementDTO create(Integer sensorId, Metric metric, BigDecimal value, Instant measuredAt) {
        var sensorRef = new Sensor();
        sensorRef.setId(sensorId);

        var measurement = new Measurement();
        measurement.setSensor(sensorRef);
        measurement.setMetric(metric);
        measurement.setValue(value);
        measurement.setMeasuredAt(measuredAt);

        return measurementMapper.toDTO(measurementRepository.save(measurement));
    }
}
