package com.example.meteonode.repository;

import com.example.meteonode.model.entity.AlarmNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlarmNotificationRepository extends JpaRepository<AlarmNotification, Integer> {
    List<AlarmNotification> findByNotifiedUserId(Integer userId);
}
