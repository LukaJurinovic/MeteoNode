package com.example.meteonode.service.application;

import com.example.meteonode.model.dto.request.MeasurementBatchRequest;
import com.example.meteonode.model.dto.request.NodeProvisionRequest;
import com.example.meteonode.model.dto.response.NodeCommandDTO;
import com.example.meteonode.model.dto.response.NodeProvisionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GatewayOperationService {

    private final NodeProvisioningService nodeProvisioningService;
    private final MeasurementIngestionService measurementIngestionService;
    private final NodeCommandManagementService nodeCommandManagementService;

    @Transactional
    public NodeProvisionResponse provision(NodeProvisionRequest request) {
        return nodeProvisioningService.provision(request);
    }

    @Transactional
    public void ingest(MeasurementBatchRequest request) {
        measurementIngestionService.ingest(request);
    }

    @Transactional
    public List<NodeCommandDTO> getPendingCommands(String serialNumber) {
        return nodeCommandManagementService.getPendingCommands(serialNumber);
    }

    @Transactional
    public void confirmDelivered(String serialNumber, Integer commandId) {
        nodeCommandManagementService.confirmDelivered(serialNumber, commandId);
    }
}
