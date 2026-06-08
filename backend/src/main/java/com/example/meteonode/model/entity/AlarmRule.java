package com.example.meteonode.model.entity;

import com.example.meteonode.model.enums.Metric;
import com.example.meteonode.model.enums.Severity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "alarm_rules")
@Getter
@Setter
public class AlarmRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Metric metric;

    @Column(name = "threshold_min", precision = 12, scale = 4)
    private BigDecimal thresholdMin;

    @Column(name = "threshold_max", precision = 12, scale = 4)
    private BigDecimal thresholdMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.WARNING;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "cooldown_seconds", nullable = false)
    private Integer cooldownSeconds = 0;

    @Column(name = "last_fired_at")
    private Instant lastFiredAt;
}
