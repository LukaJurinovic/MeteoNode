package com.example.meteonode.service.application;

import com.example.meteonode.model.dto.response.AlarmNotificationDTO;
import com.example.meteonode.model.dto.response.AlarmNotificationReadDTO;
import com.example.meteonode.model.entity.AlarmNotification;
import com.example.meteonode.model.entity.AlarmNotificationRead;
import com.example.meteonode.model.entity.User;
import com.example.meteonode.service.domain.AlarmNotificationReadService;
import com.example.meteonode.service.domain.AlarmNotificationService;
import com.example.meteonode.service.domain.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AlarmNotificationService alarmNotificationService;
    private final AlarmNotificationReadService alarmNotificationReadService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<AlarmNotificationDTO> getForUser(String username) {
        var user = userService.getUserByUsername(username);
        var notifications = alarmNotificationService.findByUserId(user.id());
        var ids = notifications.stream().map(AlarmNotificationDTO::id).toList();
        var readIds = alarmNotificationReadService.findReadNotificationIds(user.id(), ids);
        return notifications.stream()
                .map(n -> n.withRead(readIds.contains(n.id())))
                .toList();
    }

    @Transactional
    public AlarmNotificationReadDTO markAsRead(Integer notificationId, String username) {
        alarmNotificationService.findById(notificationId);
        var user = userService.getUserByUsername(username);

        if (alarmNotificationReadService.isRead(notificationId, user.id())) {
            return alarmNotificationReadService.findByIds(notificationId, user.id());
        }

        var read = new AlarmNotificationRead();
        read.setId(new AlarmNotificationRead.Id());

        var notifRef = new AlarmNotification();
        notifRef.setId(notificationId);
        read.setNotification(notifRef);

        var userRef = new User();
        userRef.setId(user.id());
        read.setUser(userRef);

        return alarmNotificationReadService.save(read);
    }
}
