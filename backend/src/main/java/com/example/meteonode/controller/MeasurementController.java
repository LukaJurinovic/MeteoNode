package com.example.meteonode.controller;

import com.example.meteonode.model.dto.response.MeasurementDTO;
import com.example.meteonode.model.enums.Metric;
import com.example.meteonode.service.domain.MeasurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Tag(name = "Measurements", description = "Sensor measurement queries. All endpoints require a valid JWT. History endpoint returns a paginated Spring Page")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/measurements")
@RequiredArgsConstructor
public class MeasurementController {

    private final MeasurementService measurementService;

    @Operation(summary = "Latest reading per metric for a sensor")
    @ApiResponse(responseCode = "200", description = "Latest measurements (empty if no data yet)")
    @GetMapping("/latest/all")
    public ResponseEntity<List<MeasurementDTO>> getLatestAll(
            @Parameter(description = "Sensor ID") @RequestParam Integer sensorId) {
        return ResponseEntity.ok(measurementService.findLatestPerMetricBySensorId(sensorId));
    }

    @Operation(summary = "Latest reading for a specific metric")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Latest measurement"),
        @ApiResponse(responseCode = "204", description = "No data yet for this sensor/metric combination", content = @Content)
    })
    @GetMapping("/latest")
    public ResponseEntity<MeasurementDTO> getLatest(
            @Parameter(description = "Sensor ID") @RequestParam Integer sensorId,
            @Parameter(description = "Metric enum value") @RequestParam Metric metric) {
        return measurementService.findLatest(sensorId, metric)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Historical measurements (paginated)", description = "Sorted ascending by measuredAt. Dates must be ISO-8601 UTC, e.g. 2025-05-01T00:00:00Z.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated measurement history"),
        @ApiResponse(responseCode = "400", description = "Invalid date format", content = @Content)
    })
    @GetMapping("/history")
    public ResponseEntity<Page<MeasurementDTO>> getHistory(
            @Parameter(description = "Sensor ID") @RequestParam Integer sensorId,
            @Parameter(description = "Metric enum value") @RequestParam Metric metric,
            @Parameter(description = "Range start, ISO-8601 UTC (e.g. 2025-05-01T00:00:00Z)") @RequestParam String from,
            @Parameter(description = "Range end, ISO-8601 UTC") @RequestParam String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("measuredAt").ascending());
        return ResponseEntity.ok(measurementService.findByRange(sensorId, metric, Instant.parse(from), Instant.parse(to), pageable));
    }
}
