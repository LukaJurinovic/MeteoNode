package com.example.meteonode.service.application;

import com.example.meteonode.exception.ResourceNotFoundException;
import com.example.meteonode.model.dto.request.UpdateNodeRequest;
import com.example.meteonode.model.dto.response.NodeDTO;
import com.example.meteonode.model.enums.Status;
import com.example.meteonode.service.domain.NodeService;
import com.example.meteonode.service.domain.WeatherStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeManagementService {

    private final NodeService nodeService;
    private final WeatherStationService weatherStationService;

    @Transactional(readOnly = true)
    public List<NodeDTO> getAll() {
        return nodeService.findAll();
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> getUnassigned() {
        return nodeService.findUnassigned();
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> getByStation(Integer stationId) {
        return nodeService.findByStationId(stationId);
    }

    @Transactional
    public NodeDTO assignStation(Integer nodeId, Integer stationId) {
        if (!weatherStationService.existsById(stationId)) {
            throw new ResourceNotFoundException("Weather station not found: " + stationId);
        }
        return nodeService.assignStation(nodeId, stationId);
    }

    @Transactional
    public NodeDTO update(Integer id, UpdateNodeRequest request) {
        return nodeService.update(id, request);
    }

    @Transactional
    public NodeDTO updateStatus(Integer id, Status status) {
        return nodeService.updateStatus(id, status);
    }

    @Transactional
    public void unassignStation(Integer nodeId) {
        nodeService.unassignStation(nodeId);
    }

    @Transactional
    public void delete(Integer id) {
        nodeService.delete(id);
    }
}
