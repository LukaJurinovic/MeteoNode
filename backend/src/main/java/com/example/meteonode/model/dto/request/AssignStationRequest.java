package com.example.meteonode.model.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignStationRequest(@NotNull Integer stationId) {}
