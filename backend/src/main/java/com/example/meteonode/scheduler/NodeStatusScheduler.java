package com.example.meteonode.scheduler;

import com.example.meteonode.service.domain.NodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class NodeStatusScheduler {

    private final NodeService nodeService;

    @Value("${meteonode.node.offline-threshold-minutes:10}")
    private long offlineThresholdMinutes;

    @Scheduled(fixedDelayString = "${meteonode.node.check-interval-ms:60000}")
    public void markStaleNodesOffline() {
        Instant threshold = Instant.now().minus(offlineThresholdMinutes, ChronoUnit.MINUTES);
        int count = nodeService.markStaleNodesOffline(threshold);
        if (count > 0) {
            log.info("Marked {} node(s) as OFFLINE due to inactivity", count);
        }
    }
}
