package com.example.meteonode.scheduler;

import com.example.meteonode.service.domain.NodeCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class NodeCommandExpiryScheduler {

    private final NodeCommandService nodeCommandService;

    @Value("${meteonode.command.expiry-hours:24}")
    private long expiryHours;

    @Scheduled(fixedDelayString = "${meteonode.command.expiry-check-ms:3600000}")
    public void expireStaleCommands() {
        Instant threshold = Instant.now().minus(expiryHours, ChronoUnit.HOURS);
        int count = nodeCommandService.expireStaleCommands(threshold);
        if (count > 0) {
            log.info("Expired {} stale node command(s)", count);
        }
    }
}
