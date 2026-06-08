package com.example.meteonode.controller;

import com.example.meteonode.model.dto.response.SystemInfoDTO;
import com.example.meteonode.service.application.SystemInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System", description = "Public system information. No authentication required.")
@RestController
@RequestMapping("/api/info")
@RequiredArgsConstructor
public class SystemController {

    private final SystemInfoService systemInfoService;

    @Operation(summary = "System summary", description = "Total station and node counts for the login page.")
    @ApiResponse(responseCode = "200", description = "System counts")
    @GetMapping
    public ResponseEntity<SystemInfoDTO> getInfo() {
        return ResponseEntity.ok(systemInfoService.getSystemInfo());
    }
}
