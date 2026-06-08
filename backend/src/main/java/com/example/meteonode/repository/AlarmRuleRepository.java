package com.example.meteonode.repository;

import com.example.meteonode.model.entity.AlarmRule;
import com.example.meteonode.model.enums.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AlarmRuleRepository extends JpaRepository<AlarmRule, Integer> {
    List<AlarmRule> findBySensorId(Integer sensorId);
    List<AlarmRule> findBySensorIdAndMetricAndIsActiveTrue(Integer sensorId, Metric metric);

    @Modifying
    @Query("UPDATE AlarmRule r SET r.lastFiredAt = :now WHERE r.id = :id " +
           "AND (r.lastFiredAt IS NULL OR r.lastFiredAt <= :threshold)")
    int fireIfReady(@Param("id") Integer id, @Param("now") Instant now, @Param("threshold") Instant threshold);
}
