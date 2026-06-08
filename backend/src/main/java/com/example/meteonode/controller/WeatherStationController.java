package com.example.meteonode.controller;

import com.example.meteonode.model.dto.request.UpdateStatusRequest;
import com.example.meteonode.model.dto.response.StationOverviewDTO;
import com.example.meteonode.model.dto.response.WeatherStationDTO;
import com.example.meteonode.model.dto.request.CreateWeatherStationRequest;
import com.example.meteonode.service.application.StationManagementService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Weather Stations", description = "CRUD for weather stations. Create requires ADMIN; update, delete, and status changes require ADMIN or OPERATOR.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class WeatherStationController {

    private final StationManagementService stationManagementService;

    @Operation(summary = "List all stations")
    @ApiResponse(responseCode = "200", description = "Station list")
    @GetMapping
    public ResponseEntity<List<WeatherStationDTO>> getAll() {
        return ResponseEntity.ok(stationManagementService.getAll());
    }

    @Operation(summary = "Create a station")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Station created"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "403", description = "Not ADMIN", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<WeatherStationDTO> create(@Valid @RequestBody CreateWeatherStationRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stationManagementService.create(request, authentication.getName()));
    }

    @Operation(summary = "Update a station")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Station updated"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Station not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PutMapping("/{id}")
    public ResponseEntity<WeatherStationDTO> update(@PathVariable Integer id,
                                                    @Valid @RequestBody CreateWeatherStationRequest request) {
        return ResponseEntity.ok(stationManagementService.update(id, request));
    }

    @Operation(summary = "Set station status", description = "Toggles between ONLINE and OFFLINE.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Station not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<WeatherStationDTO> updateStatus(@PathVariable Integer id,
                                                          @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(stationManagementService.updateStatus(id, request.status()));
    }

    @Operation(summary = "Get station overview", description = "Returns aggregated data: latest per-metric readings from all sensors, node count, and online node count.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Overview data"),
        @ApiResponse(responseCode = "404", description = "Station not found", content = @Content)
    })
    @GetMapping("/{id}/overview")
    public ResponseEntity<StationOverviewDTO> getOverview(@PathVariable Integer id) {
        return ResponseEntity.ok(stationManagementService.getOverview(id));
    }

    @Operation(summary = "Delete a station", description = "Cascades to nodes, sensors, measurements, alarm rules, and notifications.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
        @ApiResponse(responseCode = "404", description = "Station not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        stationManagementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
