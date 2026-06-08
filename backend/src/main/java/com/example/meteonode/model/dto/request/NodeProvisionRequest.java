package com.example.meteonode.model.dto.request;

import com.example.meteonode.model.enums.SensorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record NodeProvisionRequest(@NotBlank String serialNumber, @NotEmpty List<SensorType> sensors) {}
