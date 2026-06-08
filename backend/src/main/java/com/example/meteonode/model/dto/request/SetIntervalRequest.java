package com.example.meteonode.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetIntervalRequest(@NotNull @Min(10) @Max(86400) Integer intervalSeconds) {}
