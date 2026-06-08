package com.example.meteonode.service.domain;

import com.example.meteonode.mapper.AlarmNotificationReadMapper;
import com.example.meteonode.model.dto.response.AlarmNotificationReadDTO;
import com.example.meteonode.model.entity.AlarmNotificationRead;
import com.example.meteonode.repository.AlarmNotificationReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlarmNotificationReadService {

    private final AlarmNotificationReadRepository alarmNotificationReadRepository;
    private final AlarmNotificationReadMapper alarmNotificationReadMapper;

    @Transactional(readOnly = true)
    public boolean isRead(Integer notificationId, Integer userId) {
        return alarmNotificationReadRepository.existsByIdNotificationIdAndIdUserId(notificationId, userId);
    }

    @Transactional(readOnly = true)
    public AlarmNotificationReadDTO findByIds(Integer notificationId, Integer userId) {
        return alarmNotificationReadRepository
                .findByIdNotificationIdAndIdUserId(notificationId, userId)
                .map(alarmNotificationReadMapper::toDTO)
                .orElseThrow();
    }

    @Transactional(readOnly = true)
    public Set<Integer> findReadNotificationIds(Integer userId, List<Integer> notificationIds) {
        if (notificationIds.isEmpty()) return Set.of();
        return alarmNotificationReadRepository.findReadNotificationIds(userId, notificationIds);
    }

    @Transactional
    public AlarmNotificationReadDTO save(AlarmNotificationRead read) {
        return alarmNotificationReadMapper.toDTO(alarmNotificationReadRepository.save(read));
    }
}
