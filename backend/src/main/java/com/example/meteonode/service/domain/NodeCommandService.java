package com.example.meteonode.service.domain;

import com.example.meteonode.exception.ConflictException;
import com.example.meteonode.exception.ResourceNotFoundException;
import com.example.meteonode.mapper.NodeCommandMapper;
import com.example.meteonode.model.dto.response.NodeCommandDTO;
import com.example.meteonode.model.entity.Node;
import com.example.meteonode.model.entity.NodeCommand;
import com.example.meteonode.model.enums.NodeCommandStatus;
import com.example.meteonode.model.enums.NodeCommandType;
import com.example.meteonode.repository.NodeCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeCommandService {

    private final NodeCommandRepository nodeCommandRepository;
    private final NodeCommandMapper nodeCommandMapper;

    @Transactional
    public NodeCommandDTO create(Integer nodeId, NodeCommandType type, String payload) {
        Node nodeRef = new Node();
        nodeRef.setId(nodeId);

        NodeCommand cmd = new NodeCommand();
        cmd.setNode(nodeRef);
        cmd.setCommand(type);
        cmd.setPayload(payload);

        return nodeCommandMapper.toDTO(nodeCommandRepository.save(cmd));
    }

    public record ConfirmResult(NodeCommandDTO command, boolean newlyDelivered) {}

    @Transactional
    public List<NodeCommandDTO> findDeliverableBySerialNumber(String serialNumber, Instant resendCutoff) {
        var commands = nodeCommandRepository.findDeliverable(serialNumber, NodeCommandStatus.PENDING, resendCutoff);
        Instant now = Instant.now();
        commands.forEach(c -> c.setLastSentAt(now));
        return commands.stream().map(nodeCommandMapper::toDTO).toList();
    }

    @Transactional
    public ConfirmResult markDelivered(String serialNumber, Integer commandId) {
        if (!nodeCommandRepository.existsByIdAndNode_SerialNumber(commandId, serialNumber)) {
            throw new ResourceNotFoundException("Command " + commandId + " not found for node " + serialNumber);
        }
        NodeCommand command = nodeCommandRepository.findById(commandId)
                .orElseThrow(() -> new ResourceNotFoundException("Node command not found: " + commandId));

        if (command.getStatus() == NodeCommandStatus.DELIVERED) {
            return new ConfirmResult(nodeCommandMapper.toDTO(command), false);
        }
        if (command.getStatus() != NodeCommandStatus.PENDING) {
            throw new ConflictException("Command " + commandId + " cannot be marked delivered (status: " + command.getStatus() + ")");
        }
        command.setStatus(NodeCommandStatus.DELIVERED);
        command.setDeliveredAt(Instant.now());
        return new ConfirmResult(nodeCommandMapper.toDTO(command), true);
    }

    @Transactional
    public int expireStaleCommands(Instant before) {
        List<NodeCommand> stale = nodeCommandRepository.findByStatusAndCreatedAtBefore(NodeCommandStatus.PENDING, before);
        stale.forEach(c -> c.setStatus(NodeCommandStatus.EXPIRED));
        return stale.size();
    }
}
