package com.example.meteonode.controller;

import com.example.meteonode.model.dto.response.AlarmNotificationDTO;
import com.example.meteonode.model.dto.response.AlarmNotificationReadDTO;
import com.example.meteonode.service.application.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Alarm Notifications", description = "Notifications generated when a measurement breaches an alarm rule. Each user sees only their own notifications.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class AlarmNotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get my notifications", description = "Ordered by most recent first.")
    @ApiResponse(responseCode = "200", description = "Notification list")
    @GetMapping
    public ResponseEntity<List<AlarmNotificationDTO>> getMyNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getForUser(authentication.getName()));
    }

    @Operation(summary = "Mark notification as read", description = "Idempotent — calling again on an already-read notification is a no-op.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Read receipt recorded"),
        @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content)
    })
    @PostMapping("/{id}/read")
    public ResponseEntity<AlarmNotificationReadDTO> markAsRead(@PathVariable Integer id,
                                                               Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAsRead(id, authentication.getName()));
    }
}
