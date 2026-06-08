package com.example.meteonode.service.application;

import com.example.meteonode.model.dto.request.MeasurementBatchRequest;
import com.example.meteonode.service.domain.MeasurementService;
import com.example.meteonode.service.domain.NodeService;
import com.example.meteonode.service.domain.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeasurementIngestionService {

    private final MeasurementService measurementService;
    private final NodeService nodeService;
    private final SensorService sensorService;
    private final AlarmEvaluationService alarmEvaluationService;

    @Transactional
    public void ingest(MeasurementBatchRequest request) {
        nodeService.findById(request.nodeId());
        nodeService.updateLastSeen(request.nodeId());

        for (var entry : request.measurements()) {
            sensorService.validateBelongsToNode(entry.sensorId(), request.nodeId());

            var saved = measurementService.create(
                    entry.sensorId(), entry.metric(), entry.value(), entry.measuredAt());
            alarmEvaluationService.evaluate(saved);
        }
    }
}
