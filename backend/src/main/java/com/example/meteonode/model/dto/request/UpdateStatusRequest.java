package com.example.meteonode.model.dto.request;

import com.example.meteonode.model.enums.Status;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull Status status) {}
