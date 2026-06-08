package com.example.meteonode.service.application;

import com.example.meteonode.model.dto.response.SystemInfoDTO;
import com.example.meteonode.service.domain.NodeService;
import com.example.meteonode.service.domain.WeatherStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemInfoService {

    private final WeatherStationService weatherStationService;
    private final NodeService nodeService;

    @Transactional(readOnly = true)
    public SystemInfoDTO getSystemInfo() {
        return new SystemInfoDTO(weatherStationService.count(), nodeService.count());
    }
}
