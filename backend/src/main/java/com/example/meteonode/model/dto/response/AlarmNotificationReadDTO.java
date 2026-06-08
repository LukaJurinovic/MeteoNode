package com.example.meteonode.model.dto.response;

import java.time.Instant;

public record AlarmNotificationReadDTO(
        Integer notificationId,
        Integer userId,
        Instant readAt
) {}
