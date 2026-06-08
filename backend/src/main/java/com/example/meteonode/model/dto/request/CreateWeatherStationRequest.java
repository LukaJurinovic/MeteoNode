package com.example.meteonode.model.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateWeatherStationRequest(@NotBlank String name, BigDecimal locationLat, BigDecimal locationLon) {}
