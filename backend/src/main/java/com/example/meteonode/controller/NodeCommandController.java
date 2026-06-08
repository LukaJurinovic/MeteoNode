package com.example.meteonode.controller;

import com.example.meteonode.model.dto.request.IssueCommandRequest;
import com.example.meteonode.model.dto.request.SetIntervalRequest;
import com.example.meteonode.model.dto.response.NodeCommandDTO;
import com.example.meteonode.service.application.NodeCommandManagementService;
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

@Tag(name = "Node Commands", description = "Issue commands to IoT nodes. Commands are queued as PENDING and delivered to the node via the gateway's 30-second poll cycle. Requires ADMIN or OPERATOR. PENDING commands expire automatically if not delivered.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/nodes/{nodeId}")
@RequiredArgsConstructor
public class NodeCommandController {

    private final NodeCommandManagementService nodeCommandManagementService;

    @Operation(summary = "Issue a command to a node", description = "Creates a PENDING command (REQUEST_READINGS or REBOOT). The gateway picks it up on its next poll and forwards it via SMS.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Command queued"),
        @ApiResponse(responseCode = "400", description = "Invalid command type", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Node not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PostMapping("/commands")
    public ResponseEntity<NodeCommandDTO> issueCommand(@PathVariable Integer nodeId,
                                                       @Valid @RequestBody IssueCommandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nodeCommandManagementService.issueCommand(nodeId, request.command()));
    }

    @Operation(summary = "Set reporting interval", description = "Issues a SET_INTERVAL command to the node via the gateway. The new interval is persisted to EEPROM on the node and survives reboots. Range: 10–86400 seconds.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "SET_INTERVAL command queued"),
        @ApiResponse(responseCode = "400", description = "Interval out of range (10–86400 s)", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Node not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PostMapping("/interval")
    public ResponseEntity<NodeCommandDTO> setInterval(@PathVariable Integer nodeId,
                                                      @Valid @RequestBody SetIntervalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nodeCommandManagementService.issueSetInterval(nodeId, request.intervalSeconds()));
    }

}
