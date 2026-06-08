package com.example.meteonode.model.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "alarm_notification_reads")
@Getter
@Setter
public class AlarmNotificationRead {

    @EmbeddedId
    private Id id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("notificationId")
    @JoinColumn(name = "notification_id")
    private AlarmNotification notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "read_at", nullable = false, updatable = false)
    private Instant readAt;

    @PrePersist
    void prePersist() {
        this.readAt = Instant.now();
    }

    @Embeddable
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class Id implements Serializable {

        @Column(name = "notification_id")
        private Integer notificationId;

        @Column(name = "user_id")
        private Integer userId;
    }
}
