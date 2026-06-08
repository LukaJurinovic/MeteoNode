package com.example.meteonode.repository;

import com.example.meteonode.model.entity.AlarmNotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AlarmNotificationReadRepository extends JpaRepository<AlarmNotificationRead, AlarmNotificationRead.Id> {
    boolean existsByIdNotificationIdAndIdUserId(Integer notificationId, Integer userId);

    Optional<AlarmNotificationRead> findByIdNotificationIdAndIdUserId(Integer notificationId, Integer userId);

    @Query("SELECT r.id.notificationId FROM AlarmNotificationRead r WHERE r.id.userId = :userId AND r.id.notificationId IN :notificationIds")
    Set<Integer> findReadNotificationIds(@Param("userId") Integer userId, @Param("notificationIds") List<Integer> notificationIds);
}
