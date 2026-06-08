package com.example.meteonode.repository;

import com.example.meteonode.model.entity.Gateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GatewayRepository extends JpaRepository<Gateway, Integer> {
    Optional<Gateway> findByApiKey(String apiKey);
}
