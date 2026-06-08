package com.example.meteonode.controller;

import com.example.meteonode.model.dto.request.UpdateAlarmRuleRequest;
import com.example.meteonode.model.dto.response.AlarmRuleDTO;
import com.example.meteonode.model.dto.request.CreateAlarmRuleRequest;
import com.example.meteonode.service.application.AlarmRuleManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Alarm Rules", description = "Threshold-based alarm rules per sensor metric. All endpoints require ADMIN or OPERATOR role. A rule fires an AlarmNotification when a measurement breaches minValue/maxValue and the cooldown has elapsed.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/alarm-rules")
@RequiredArgsConstructor
public class AlarmRuleController {

    private final AlarmRuleManagementService alarmRuleManagementService;

    @Operation(summary = "List all alarm rules")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule list"),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @GetMapping
    public ResponseEntity<List<AlarmRuleDTO>> getAll() {
        return ResponseEntity.ok(alarmRuleManagementService.findAll());
    }

    @Operation(summary = "List rules for a sensor")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule list"),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sensor not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @GetMapping("/sensor/{sensorId}")
    public ResponseEntity<List<AlarmRuleDTO>> getBySensor(@PathVariable Integer sensorId) {
        return ResponseEntity.ok(alarmRuleManagementService.findBySensorId(sensorId));
    }

    @Operation(summary = "Create an alarm rule", description = "Defines a threshold rule on a sensor metric. Optional cooldownSeconds (default 0) prevents notification floods.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rule created"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sensor not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PostMapping
    public ResponseEntity<AlarmRuleDTO> create(@Valid @RequestBody CreateAlarmRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alarmRuleManagementService.create(request));
    }

    @Operation(summary = "Update an alarm rule", description = "Replaces threshold values, severity, cooldown, and name. Does not change the active/inactive state.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule updated"),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Rule not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PutMapping("/{id}")
    public ResponseEntity<AlarmRuleDTO> update(@PathVariable Integer id,
                                               @RequestBody UpdateAlarmRuleRequest request) {
        return ResponseEntity.ok(alarmRuleManagementService.update(id, request));
    }

    @Operation(summary = "Toggle rule active/inactive", description = "Flips isActive. Inactive rules are skipped during alarm evaluation.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule toggled"),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Rule not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<AlarmRuleDTO> toggle(@PathVariable Integer id) {
        return ResponseEntity.ok(alarmRuleManagementService.toggle(id));
    }

    @Operation(summary = "Delete an alarm rule")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Rule not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        alarmRuleManagementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
