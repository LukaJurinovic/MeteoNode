package com.example.meteonode.model.dto.response;

import com.example.meteonode.model.enums.NodeCommandStatus;
import com.example.meteonode.model.enums.NodeCommandType;

import java.time.Instant;

public record NodeCommandDTO(
        Integer id,
        Integer nodeId,
        NodeCommandType command,
        NodeCommandStatus status,
        String payload,
        Instant createdAt,
        Instant deliveredAt
) {}
