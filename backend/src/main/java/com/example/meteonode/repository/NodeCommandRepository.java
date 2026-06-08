package com.example.meteonode.repository;

import com.example.meteonode.model.entity.NodeCommand;
import com.example.meteonode.model.enums.NodeCommandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NodeCommandRepository extends JpaRepository<NodeCommand, Integer> {
    List<NodeCommand> findByStatusAndCreatedAtBefore(NodeCommandStatus status, Instant createdAt);
    boolean existsByIdAndNode_SerialNumber(Integer id, String serialNumber);

    @Query("""
            SELECT c FROM NodeCommand c
            WHERE c.node.serialNumber = :serialNumber
              AND c.status = :status
              AND (c.lastSentAt IS NULL OR c.lastSentAt < :resendCutoff)
            """)
    List<NodeCommand> findDeliverable(@Param("serialNumber") String serialNumber,
                                      @Param("status") NodeCommandStatus status,
                                      @Param("resendCutoff") Instant resendCutoff);
}
