package com.example.meteonode.model.dto.request;

import com.example.meteonode.model.enums.NodeCommandType;
import jakarta.validation.constraints.NotNull;

public record IssueCommandRequest(@NotNull NodeCommandType command) {}
