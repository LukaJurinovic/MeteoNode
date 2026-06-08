package com.example.meteonode.repository;

import com.example.meteonode.model.entity.WeatherStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherStationRepository extends JpaRepository<WeatherStation, Integer> {
}
