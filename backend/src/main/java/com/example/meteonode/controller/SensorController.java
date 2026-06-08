package com.example.meteonode.controller;

import com.example.meteonode.model.dto.response.SensorDTO;
import com.example.meteonode.service.domain.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Sensors", description = "Sensor management. Sensors are created automatically when a node registers via the gateway.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;

    @Operation(summary = "List sensors by node")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sensor list"),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Node not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @GetMapping("/node/{nodeId}")
    public ResponseEntity<List<SensorDTO>> getByNode(@PathVariable Integer nodeId) {
        return ResponseEntity.ok(sensorService.findByNodeId(nodeId));
    }

    @Operation(summary = "Deactivate a sensor", description = "Inactive sensors are excluded from alarm evaluation and their measurements are no longer ingested. Does not delete historical data.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sensor deactivated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sensor not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        sensorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Activate a sensor", description = "Re-enables a previously deactivated sensor for alarm evaluation and measurement ingestion.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sensor activated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sensor not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Integer id) {
        sensorService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a sensor", description = "Requires ADMIN. Cascades to measurements and alarm rules for this sensor.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sensor not found", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        sensorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
