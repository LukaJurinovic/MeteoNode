package com.example.meteonode.service.domain;

import com.example.meteonode.exception.ResourceNotFoundException;
import com.example.meteonode.mapper.AlarmNotificationMapper;
import com.example.meteonode.model.dto.response.AlarmNotificationDTO;
import com.example.meteonode.model.entity.AlarmNotification;
import com.example.meteonode.model.entity.AlarmRule;
import com.example.meteonode.model.entity.Measurement;
import com.example.meteonode.model.entity.User;
import com.example.meteonode.repository.AlarmNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmNotificationService {

    private final AlarmNotificationRepository alarmNotificationRepository;
    private final AlarmNotificationMapper alarmNotificationMapper;

    @Transactional(readOnly = true)
    public AlarmNotificationDTO findById(Integer id) {
        return alarmNotificationMapper.toDTO(
                alarmNotificationRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Alarm notification not found: " + id))
        );
    }

    @Transactional(readOnly = true)
    public List<AlarmNotificationDTO> findByUserId(Integer userId) {
        return alarmNotificationRepository.findByNotifiedUserId(userId).stream()
                .map(alarmNotificationMapper::toDTO)
                .toList();
    }

    @Transactional
    public AlarmNotificationDTO create(Integer ruleId, Long measurementId, Integer userId, String message) {
        var ruleRef = new AlarmRule();
        ruleRef.setId(ruleId);

        var measurementRef = new Measurement();
        measurementRef.setId(measurementId);

        var ownerRef = new User();
        ownerRef.setId(userId);

        var notification = new AlarmNotification();
        notification.setRule(ruleRef);
        notification.setMeasurement(measurementRef);
        notification.setNotifiedUser(ownerRef);
        notification.setMessage(message);

        return alarmNotificationMapper.toDTO(alarmNotificationRepository.save(notification));
    }
}
