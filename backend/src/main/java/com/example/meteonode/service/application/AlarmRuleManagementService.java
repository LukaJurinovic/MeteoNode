package com.example.meteonode.service.application;

import com.example.meteonode.model.dto.request.CreateAlarmRuleRequest;
import com.example.meteonode.model.dto.request.UpdateAlarmRuleRequest;
import com.example.meteonode.model.dto.response.AlarmRuleDTO;
import com.example.meteonode.service.domain.AlarmRuleService;
import com.example.meteonode.service.domain.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmRuleManagementService {

    private final AlarmRuleService alarmRuleService;
    private final SensorService sensorService;

    @Transactional(readOnly = true)
    public List<AlarmRuleDTO> findAll() {
        return alarmRuleService.findAll();
    }

    @Transactional(readOnly = true)
    public List<AlarmRuleDTO> findBySensorId(Integer sensorId) {
        return alarmRuleService.findBySensorId(sensorId);
    }

    @Transactional
    public AlarmRuleDTO create(CreateAlarmRuleRequest request) {
        sensorService.findById(request.sensorId());
        return alarmRuleService.create(request);
    }

    @Transactional
    public AlarmRuleDTO update(Integer id, UpdateAlarmRuleRequest request) {
        return alarmRuleService.update(id, request);
    }

    @Transactional
    public AlarmRuleDTO toggle(Integer id) {
        return alarmRuleService.toggle(id);
    }

    @Transactional
    public void delete(Integer id) {
        alarmRuleService.delete(id);
    }
}
