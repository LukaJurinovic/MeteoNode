package com.example.meteonode.model.entity;

import com.example.meteonode.model.enums.SensorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sensors")
@Getter
@Setter
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 50)
    private SensorType sensorType;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
