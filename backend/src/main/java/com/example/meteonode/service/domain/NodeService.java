package com.example.meteonode.service.domain;

import com.example.meteonode.exception.ResourceNotFoundException;
import com.example.meteonode.mapper.NodeMapper;
import com.example.meteonode.model.dto.request.UpdateNodeRequest;
import com.example.meteonode.model.dto.response.NodeDTO;
import com.example.meteonode.model.entity.Node;
import com.example.meteonode.model.entity.WeatherStation;
import com.example.meteonode.model.enums.Status;
import com.example.meteonode.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NodeService {

    private final NodeRepository nodeRepository;
    private final NodeMapper nodeMapper;

    @Transactional(readOnly = true)
    public NodeDTO findById(Integer id) {
        return nodeMapper.toDTO(getById(id));
    }

    @Transactional(readOnly = true)
    public Optional<NodeDTO> findOptionalBySerialNumber(String serialNumber) {
        return nodeRepository.findBySerialNumber(serialNumber).map(nodeMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> findByStationId(Integer stationId) {
        return nodeRepository.findByStationId(stationId).stream()
                .map(nodeMapper::toDTO)
                .toList();
    }

    @Transactional
    public NodeDTO create(String serialNumber) {
        var node = new Node();
        node.setSerialNumber(serialNumber);
        node.setStatus(Status.ONLINE);
        node.setLastSeen(Instant.now());
        return nodeMapper.toDTO(nodeRepository.save(node));
    }

    @Transactional(readOnly = true)
    public long count() {
        return nodeRepository.count();
    }

    @Transactional
    public NodeDTO assignStation(Integer nodeId, Integer stationId) {
        var node = getById(nodeId);
        var stationRef = new WeatherStation();
        stationRef.setId(stationId);
        node.setStation(stationRef);
        return nodeMapper.toDTO(node);
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> findAll() {
        return nodeRepository.findAll().stream()
                .map(nodeMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> findUnassigned() {
        return nodeRepository.findByStationIsNull().stream()
                .map(nodeMapper::toDTO)
                .toList();
    }

    @Transactional
    public NodeDTO updateStatus(Integer id, Status status) {
        var node = getById(id);
        node.setStatus(status);
        return nodeMapper.toDTO(node);
    }

    @Transactional
    public NodeDTO updateLastSeen(Integer id) {
        var node = getById(id);
        node.setLastSeen(Instant.now());
        return nodeMapper.toDTO(node);
    }

    @Transactional
    public void markOnline(Integer id) {
        var node = getById(id);
        node.setStatus(Status.ONLINE);
        node.setLastSeen(Instant.now());
    }

    @Transactional
    public NodeDTO update(Integer id, UpdateNodeRequest request) {
        var node = getById(id);
        if (request.displayName() != null) node.setDisplayName(request.displayName());
        return nodeMapper.toDTO(node);
    }

    @Transactional
    public NodeDTO updateReportingInterval(Integer id, Integer seconds) {
        var node = getById(id);
        node.setReportingIntervalSeconds(seconds);
        return nodeMapper.toDTO(node);
    }

    @Transactional
    public int markStaleNodesOffline(Instant threshold) {
        var stale = nodeRepository.findByStatusAndLastSeenBefore(Status.ONLINE, threshold);
        stale.forEach(n -> n.setStatus(Status.OFFLINE));
        return stale.size();
    }

    @Transactional
    public void unassignStation(Integer nodeId) {
        getById(nodeId).setStation(null);
    }

    @Transactional
    public void delete(Integer id) {
        nodeRepository.delete(getById(id));
    }

    private Node getById(Integer id) {
        return nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Node not found: " + id));
    }
}
