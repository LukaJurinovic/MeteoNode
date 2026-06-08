package com.example.meteonode.model.dto.response;

import com.example.meteonode.model.enums.Status;

import java.math.BigDecimal;

public record WeatherStationDTO(
        Integer id,
        String name,
        Integer ownerId,
        BigDecimal locationLat,
        BigDecimal locationLon,
        Status status
) {}
