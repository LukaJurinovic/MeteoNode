package com.example.meteonode.model.entity;

import com.example.meteonode.model.enums.NodeCommandStatus;
import com.example.meteonode.model.enums.NodeCommandType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "node_commands")
@Getter
@Setter
public class NodeCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeCommandType command;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeCommandStatus status = NodeCommandStatus.PENDING;

    @Column
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }
}
