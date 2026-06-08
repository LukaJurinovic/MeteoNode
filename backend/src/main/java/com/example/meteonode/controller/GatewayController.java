package com.example.meteonode.controller;

import com.example.meteonode.model.dto.request.MeasurementBatchRequest;
import com.example.meteonode.model.dto.request.NodeProvisionRequest;
import com.example.meteonode.model.dto.response.NodeCommandDTO;
import com.example.meteonode.model.dto.response.NodeProvisionResponse;
import com.example.meteonode.service.application.GatewayOperationService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Gateway (IoT)", description = "Endpoints called exclusively by the ESP32 gateway firmware. Authentication uses the X-Api-Key header scoped to a weather station — not JWT.")
@SecurityRequirement(name = "apiKey")
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayOperationService gatewayOperationService;

    @Operation(summary = "Register / re-register a node", description = "Idempotent. Creates the node and one sensor row per declared sensor type on first call; returns existing IDs on subsequent calls.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Node already registered — existing IDs returned"),
        @ApiResponse(responseCode = "201", description = "Node provisioned — new nodeId and per-sensor IDs returned"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid or missing API key", content = @Content)
    })
    @PostMapping("/nodes/register")
    public ResponseEntity<NodeProvisionResponse> register(@Valid @RequestBody NodeProvisionRequest request) {
        NodeProvisionResponse response = gatewayOperationService.provision(request);
        return ResponseEntity.status(response.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(response);
    }

    @Operation(summary = "Ingest a measurement batch", description = "Accepts a batch of readings from one node (up to 5 metrics per SMS cycle). Triggers inline alarm rule evaluation — an AlarmNotification is created for any breached rule that is not within its cooldown window.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Measurements stored", content = @Content),
        @ApiResponse(responseCode = "400", description = "Validation error or sensor does not belong to node", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid or missing API key", content = @Content),
        @ApiResponse(responseCode = "404", description = "Node or sensor not found", content = @Content)
    })
    @PostMapping("/measurements")
    public ResponseEntity<Void> ingest(@Valid @RequestBody MeasurementBatchRequest request) {
        gatewayOperationService.ingest(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Poll pending commands for a node")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pending command list (may be empty)"),
        @ApiResponse(responseCode = "401", description = "Invalid or missing API key", content = @Content),
        @ApiResponse(responseCode = "404", description = "Node not found", content = @Content)
    })
    @GetMapping("/nodes/{serialNumber}/commands/pending")
    public ResponseEntity<List<NodeCommandDTO>> getPendingCommands(@PathVariable String serialNumber) {
        return ResponseEntity.ok(gatewayOperationService.getPendingCommands(serialNumber));
    }

    @Operation(summary = "Confirm command delivery", description = "Called after the node ACKs the command via SMS (CONF:cmdId). Marks the command DELIVERED and records deliveredAt.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Command marked delivered", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid or missing API key", content = @Content),
        @ApiResponse(responseCode = "404", description = "Command not found for this node", content = @Content),
        @ApiResponse(responseCode = "409", description = "Command is not in PENDING status", content = @Content)
    })
    @PostMapping("/nodes/{serialNumber}/commands/{commandId}/confirm")
    public ResponseEntity<Void> confirmCommand(@PathVariable String serialNumber,
                                               @PathVariable Integer commandId) {
        gatewayOperationService.confirmDelivered(serialNumber, commandId);
        return ResponseEntity.noContent().build();
    }
}
