package com.example.meteonode.service.application;

import com.example.meteonode.model.dto.response.StationOverviewDTO;
import com.example.meteonode.model.dto.response.WeatherStationDTO;
import com.example.meteonode.model.dto.request.CreateWeatherStationRequest;
import com.example.meteonode.model.enums.Status;
import com.example.meteonode.service.domain.UserService;
import com.example.meteonode.service.domain.WeatherStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationManagementService {

    private final WeatherStationService weatherStationService;
    private final UserService userService;
    private final StationOverviewService stationOverviewService;

    @Transactional(readOnly = true)
    public List<WeatherStationDTO> getAll() {
        return weatherStationService.findAll();
    }

    @Transactional
    public WeatherStationDTO create(CreateWeatherStationRequest request, String ownerUsername) {
        var owner = userService.getUserByUsername(ownerUsername);
        return weatherStationService.create(request, owner.id());
    }

    @Transactional
    public WeatherStationDTO update(Integer id, CreateWeatherStationRequest request) {
        return weatherStationService.update(id, request);
    }

    @Transactional
    public WeatherStationDTO updateStatus(Integer id, Status status) {
        return weatherStationService.updateStatus(id, status);
    }

    @Transactional
    public void delete(Integer id) {
        weatherStationService.delete(id);
    }

    @Transactional(readOnly = true)
    public StationOverviewDTO getOverview(Integer id) {
        return stationOverviewService.getOverview(id);
    }
}
