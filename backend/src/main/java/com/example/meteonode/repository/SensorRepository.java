package com.example.meteonode.repository;

import com.example.meteonode.model.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Integer> {
    List<Sensor> findByNodeId(Integer nodeId);
    List<Sensor> findByNodeIdAndIsActiveTrue(Integer nodeId);
    List<Sensor> findByNodeIdInAndIsActiveTrue(List<Integer> nodeIds);
    Optional<Sensor> findByIdAndNodeId(Integer id, Integer nodeId);
}
