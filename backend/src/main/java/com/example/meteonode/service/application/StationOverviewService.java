package com.example.meteonode.service.application;

import com.example.meteonode.model.dto.response.MeasurementDTO;
import com.example.meteonode.model.dto.response.NodeDTO;
import com.example.meteonode.model.dto.response.SensorDTO;
import com.example.meteonode.model.dto.response.StationOverviewDTO;
import com.example.meteonode.model.enums.Status;
import com.example.meteonode.service.domain.MeasurementService;
import com.example.meteonode.service.domain.NodeService;
import com.example.meteonode.service.domain.SensorService;
import com.example.meteonode.service.domain.WeatherStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StationOverviewService {

    private final WeatherStationService weatherStationService;
    private final NodeService nodeService;
    private final SensorService sensorService;
    private final MeasurementService measurementService;

    @Transactional(readOnly = true)
    public StationOverviewDTO getOverview(Integer stationId) {
        var station = weatherStationService.findById(stationId);
        var nodes = nodeService.findByStationId(stationId);

        var nodeCount = nodes.size();
        var onlineNodes = (int) nodes.stream().filter(n -> n.status() == Status.ONLINE).count();

        var nodeIds = nodes.stream().map(NodeDTO::id).toList();
        var allSensors = sensorService.findActiveByNodeIds(nodeIds);

        var sensorIds = allSensors.stream().map(SensorDTO::id).toList();
        var latestMeasurements = measurementService.findLatestPerMetricBySensorIds(sensorIds);

        var nodeById = nodes.stream().collect(Collectors.toMap(NodeDTO::id, n -> n));
        var sensorById = allSensors.stream().collect(Collectors.toMap(SensorDTO::id, s -> s));

        var readings = new ArrayList<StationOverviewDTO.SensorReading>();
        for (MeasurementDTO m : latestMeasurements) {
            var sensor = sensorById.get(m.sensorId());
            if (sensor == null) continue;
            var node = nodeById.get(sensor.nodeId());
            if (node == null) continue;

            readings.add(new StationOverviewDTO.SensorReading(
                    sensor.id(), node.id(), node.displayName(),
                    m.metric(), m.value(), m.measuredAt()));
        }

        return new StationOverviewDTO(
                stationId, station.name(), station.status(),
                nodeCount, onlineNodes, readings);
    }
}
