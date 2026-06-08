package com.example.meteonode.service.application;

import com.example.meteonode.model.dto.request.NodeProvisionRequest;
import com.example.meteonode.model.dto.response.NodeProvisionResponse;
import com.example.meteonode.model.enums.SensorType;
import com.example.meteonode.service.domain.NodeService;
import com.example.meteonode.service.domain.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NodeProvisioningService {

    private final NodeService nodeService;
    private final SensorService sensorService;

    @Transactional
    public NodeProvisionResponse provision(NodeProvisionRequest request) {
        boolean[] created = {false};
        var nodeId = findOrCreateNode(request, created);
        var sensorIds = findOrCreateSensors(request.sensors(), nodeId);
        return new NodeProvisionResponse(nodeId, sensorIds, created[0]);
    }

    private Integer findOrCreateNode(NodeProvisionRequest request, boolean[] created) {
        var existing = nodeService.findOptionalBySerialNumber(request.serialNumber());
        if (existing.isPresent()) {
            var id = existing.get().id();
            nodeService.markOnline(id);
            return id;
        }
        created[0] = true;
        return nodeService.create(request.serialNumber()).id();
    }

    private Map<SensorType, Integer> findOrCreateSensors(List<SensorType> requestedTypes, Integer nodeId) {
        var existing = sensorService.findByNodeId(nodeId);
        var sensorIds = new LinkedHashMap<SensorType, Integer>();

        for (SensorType type : requestedTypes) {
            existing.stream()
                    .filter(s -> s.sensorType() == type)
                    .findFirst()
                    .ifPresentOrElse(
                            s -> sensorIds.put(type, s.id()),
                            () -> sensorIds.put(type, sensorService.create(type, nodeId).id())
                    );
        }
        return sensorIds;
    }
}
