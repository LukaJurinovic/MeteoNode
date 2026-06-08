package com.example.meteonode.service.domain;

import com.example.meteonode.mapper.SensorMapper;
import com.example.meteonode.model.dto.response.SensorDTO;
import com.example.meteonode.model.entity.Node;
import com.example.meteonode.model.entity.Sensor;
import com.example.meteonode.model.enums.SensorType;
import com.example.meteonode.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.meteonode.exception.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorService {

    private final SensorRepository sensorRepository;
    private final SensorMapper sensorMapper;

    @Transactional(readOnly = true)
    public SensorDTO findById(Integer id) {
        return sensorMapper.toDTO(getById(id));
    }

    @Transactional(readOnly = true)
    public List<SensorDTO> findByNodeId(Integer nodeId) {
        return sensorRepository.findByNodeId(nodeId).stream()
                .map(sensorMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SensorDTO> findActiveByNodeIds(List<Integer> nodeIds) {
        if (nodeIds.isEmpty()) return List.of();
        return sensorRepository.findByNodeIdInAndIsActiveTrue(nodeIds).stream()
                .map(sensorMapper::toDTO)
                .toList();
    }

    @Transactional
    public SensorDTO create(SensorType type, Integer nodeId) {
        var nodeRef = new Node();
        nodeRef.setId(nodeId);

        var sensor = new Sensor();
        sensor.setNode(nodeRef);
        sensor.setSensorType(type);

        return sensorMapper.toDTO(sensorRepository.save(sensor));
    }

    @Transactional
    public void deactivate(Integer id) {
        getById(id).setActive(false);
    }

    @Transactional
    public void activate(Integer id) {
        getById(id).setActive(true);
    }

    @Transactional(readOnly = true)
    public void validateBelongsToNode(Integer sensorId, Integer nodeId) {
        sensorRepository.findByIdAndNodeId(sensorId, nodeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sensor " + sensorId + " not found on node " + nodeId));
    }

    @Transactional
    public void delete(Integer id) {
        sensorRepository.delete(getById(id));
    }

    private Sensor getById(Integer id) {
        return sensorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor not found: " + id));
    }
}
