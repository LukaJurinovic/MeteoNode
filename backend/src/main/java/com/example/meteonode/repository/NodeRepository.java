package com.example.meteonode.repository;

import com.example.meteonode.model.entity.Node;
import com.example.meteonode.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NodeRepository extends JpaRepository<Node, Integer> {
    List<Node> findByStationId(Integer stationId);
    List<Node> findByStationIsNull();
    Optional<Node> findBySerialNumber(String serialNumber);
    List<Node> findByStatusAndLastSeenBefore(Status status, Instant threshold);
}
