package com.example.meteonode.service.application;

import com.example.meteonode.exception.BadRequestException;
import com.example.meteonode.model.dto.response.NodeCommandDTO;
import com.example.meteonode.model.enums.NodeCommandType;
import com.example.meteonode.model.enums.Status;
import com.example.meteonode.service.domain.NodeCommandService;
import com.example.meteonode.service.domain.NodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeCommandManagementService {

    private final NodeCommandService nodeCommandService;
    private final NodeService nodeService;

    @Value("${meteonode.command.resend-window-seconds:180}")
    private long resendWindowSeconds;

    @Transactional
    public NodeCommandDTO issueCommand(Integer nodeId, NodeCommandType command) {
        if (command == NodeCommandType.SET_INTERVAL) {
            throw new BadRequestException("Use the set-interval endpoint to issue SET_INTERVAL commands");
        }
        nodeService.findById(nodeId);
        return nodeCommandService.create(nodeId, command, null);
    }

    @Transactional
    public NodeCommandDTO issueSetInterval(Integer nodeId, Integer intervalSeconds) {
        nodeService.findById(nodeId);
        nodeService.updateReportingInterval(nodeId, intervalSeconds);
        return nodeCommandService.create(nodeId, NodeCommandType.SET_INTERVAL, String.valueOf(intervalSeconds));
    }

    @Transactional
    public List<NodeCommandDTO> getPendingCommands(String serialNumber) {
        Instant resendCutoff = Instant.now().minusSeconds(resendWindowSeconds);
        return nodeCommandService.findDeliverableBySerialNumber(serialNumber, resendCutoff);
    }

    @Transactional
    public void confirmDelivered(String serialNumber, Integer commandId) {
        var result = nodeCommandService.markDelivered(serialNumber, commandId);
        if (!result.newlyDelivered()) {
            return;
        }
        var confirmed = result.command();
        switch (confirmed.command()) {
            case SET_INTERVAL -> {
                if (confirmed.payload() != null) {
                    nodeService.updateReportingInterval(confirmed.nodeId(), Integer.parseInt(confirmed.payload()));
                }
            }
            case REBOOT -> nodeService.updateStatus(confirmed.nodeId(), Status.OFFLINE);
        }
    }

}
