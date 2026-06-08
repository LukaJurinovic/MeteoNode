package com.example.meteonode.service.domain;

import com.example.meteonode.mapper.WeatherStationMapper;
import com.example.meteonode.model.dto.request.CreateWeatherStationRequest;
import com.example.meteonode.model.dto.response.WeatherStationDTO;
import com.example.meteonode.model.entity.User;
import com.example.meteonode.model.entity.WeatherStation;
import com.example.meteonode.model.enums.Status;
import com.example.meteonode.repository.WeatherStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.meteonode.exception.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherStationService {

    private final WeatherStationRepository weatherStationRepository;
    private final WeatherStationMapper weatherStationMapper;

    @Transactional(readOnly = true)
    public WeatherStationDTO findById(Integer id) {
        return weatherStationMapper.toDTO(getById(id));
    }

    @Transactional(readOnly = true)
    public long count() {
        return weatherStationRepository.count();
    }

    @Transactional(readOnly = true)
    public boolean existsById(Integer id) {
        return weatherStationRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public List<WeatherStationDTO> findAll() {
        return weatherStationRepository.findAll().stream()
                .map(weatherStationMapper::toDTO)
                .toList();
    }

    @Transactional
    public WeatherStationDTO create(CreateWeatherStationRequest request, Integer ownerId) {
        var ownerRef = new User();
        ownerRef.setId(ownerId);

        var station = new WeatherStation();
        station.setName(request.name());
        station.setLocationLat(request.locationLat());
        station.setLocationLon(request.locationLon());
        station.setOwner(ownerRef);

        return weatherStationMapper.toDTO(weatherStationRepository.save(station));
    }

    @Transactional
    public WeatherStationDTO updateStatus(Integer id, Status status) {
        var station = getById(id);
        station.setStatus(status);
        return weatherStationMapper.toDTO(station);
    }

    @Transactional
    public WeatherStationDTO update(Integer id, CreateWeatherStationRequest request) {
        var station = getById(id);
        station.setName(request.name());
        station.setLocationLat(request.locationLat());
        station.setLocationLon(request.locationLon());
        return weatherStationMapper.toDTO(station);
    }

    @Transactional
    public void delete(Integer id) {
        weatherStationRepository.delete(getById(id));
    }

    private WeatherStation getById(Integer id) {
        return weatherStationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Weather station not found: " + id));
    }
}
