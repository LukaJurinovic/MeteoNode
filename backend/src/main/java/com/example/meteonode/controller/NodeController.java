package com.example.meteonode.controller;

import com.example.meteonode.model.dto.request.AssignStationRequest;
import com.example.meteonode.model.dto.request.UpdateNodeRequest;
import com.example.meteonode.model.dto.request.UpdateStatusRequest;
import com.example.meteonode.model.dto.response.NodeDTO;
import com.example.meteonode.service.application.NodeManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Nodes", description = "IoT node management. Read access is open to all authenticated users. Write operations require ADMIN or OPERATOR.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeManagementService nodeManagementService;

    @Operation(summary = "List all nodes", description = "Returns all nodes in the system.")
    @ApiResponse(responseCode = "200", description = "Node list")
    @GetMapping
    public ResponseEntity<List<NodeDTO>> getAll() {
        return ResponseEntity.ok(nodeManagementService.getAll());
    }

    @Operation(summary = "List unassigned nodes", description = "Returns nodes that have not yet been assigned to a station.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unassigned node list"),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @GetMapping("/unassigned")
    public ResponseEntity<List<NodeDTO>> getUnassigned() {
        return ResponseEntity.ok(nodeManagementService.getUnassigned());
    }

    @Operation(summary = "Assign node to a station", description = "Assigns an unassigned node to the given station.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Node assigned"),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Node or station not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PatchMapping("/{id}/station")
    public ResponseEntity<NodeDTO> assignStation(@PathVariable Integer id,
                                                 @Valid @RequestBody AssignStationRequest request) {
        return ResponseEntity.ok(nodeManagementService.assignStation(id, request.stationId()));
    }

    @Operation(summary = "List nodes by station", description = "Returns all nodes belonging to the given station, including current status and last-seen timestamp.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Node list"),
        @ApiResponse(responseCode = "404", description = "Station not found", content = @Content)
    })
    @GetMapping("/station/{stationId}")
    public ResponseEntity<List<NodeDTO>> getByStation(@PathVariable Integer stationId) {
        return ResponseEntity.ok(nodeManagementService.getByStation(stationId));
    }

    @Operation(summary = "Update node metadata", description = "Updates mutable fields such as display name.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Node updated"),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Node not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PutMapping("/{id}")
    public ResponseEntity<NodeDTO> update(@PathVariable Integer id,
                                          @RequestBody UpdateNodeRequest request) {
        return ResponseEntity.ok(nodeManagementService.update(id, request));
    }

    @Operation(summary = "Set node status", description = "Manually overrides the status calculated by the scheduler.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Node not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<NodeDTO> updateStatus(@PathVariable Integer id,
                                                @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(nodeManagementService.updateStatus(id, request.status()));
    }

    @Operation(summary = "Unassign node from its station", description = "Removes the station association, node becomes unassigned.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Unassigned", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Node not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @DeleteMapping("/{id}/station")
    public ResponseEntity<Void> unassignStation(@PathVariable Integer id) {
        nodeManagementService.unassignStation(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a node", description = "Cascades to sensors, measurements, alarm rules, notifications, and commands.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Node not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        nodeManagementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
