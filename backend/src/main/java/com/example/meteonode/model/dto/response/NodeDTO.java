package com.example.meteonode.model.dto.response;

import com.example.meteonode.model.enums.Status;

import java.time.Instant;

public record NodeDTO(
        Integer id,
        String serialNumber,
        String displayName,
        Integer stationId,
        Status status,
        Instant lastSeen,
        Integer reportingIntervalSeconds
) {}
