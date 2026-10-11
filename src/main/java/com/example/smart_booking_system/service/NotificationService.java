package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.response.notification.NotificationResponseDTO;
import com.example.smart_booking_system.entity.Notification;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.NotificationType;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.i18n.Messages;
import com.example.smart_booking_system.i18n.UserLocales;
import com.example.smart_booking_system.repository.NotificationRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * In-app notifications. Senders pass an i18n key prefix + arguments: "notification.booking.created"
 * resolves "notification.booking.created.title" / ".message" from i18n/messages*.properties.
 * The key and arguments are stored, so the inbox is rendered in the reader's current language;
 * title/message also keep a copy in the recipient's preferred language (and are the only text for
 * notifications created before keys existed).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Messages messages;
    private final UserLocales userLocales;
    private final ObjectMapper objectMapper;

    // --- ĐỊNH NGHĨA SCOPE ---
    private static final List<NotificationType> CUSTOMER_TYPES = Arrays.asList(
            NotificationType.GENERAL, NotificationType.ACCOUNT_UPDATE, NotificationType.SECURITY_ALERT,
            NotificationType.BOOKING_SUCCESS, NotificationType.BOOKING_CANCELLED, NotificationType.BOOKING_FAILED,
            NotificationType.PAYMENT_SUCCESS, NotificationType.REFUND_PROCESSED, NotificationType.PROMOTION,
            NotificationType.REMINDER_CHECKIN, NotificationType.APPROVAL, NotificationType.REJECTION
    );

    private static final List<NotificationType> OWNER_TYPES = Arrays.asList(
            NotificationType.BOOKING_RECEIVED, NotificationType.BOOKING_CANCELLED_BY_GUEST,
            NotificationType.REVENUE_REPORT, NotificationType.PROPERTY_SUSPENDED, NotificationType.PAYOUT_SUCCESS,
            NotificationType.ROOM_SUSPENDED,
            NotificationType.SYSTEM
    );

    private static final List<NotificationType> ADMIN_TYPES = Arrays.asList(
            NotificationType.ADMIN_NEW_OWNER_REGISTRATION,
            NotificationType.ADMIN_NEW_PROPERTY_SUBMISSION,
            NotificationType.ADMIN_NEW_REFUND_REQUEST
    );

    // --- PHẦN 1: LOGIC ĐỌC (GET) ---

    public Page<NotificationResponseDTO> getUserNotifications(String userId, String scope, Pageable pageable) {
        List<NotificationType> targetTypes = getTypesByScope(scope);
        return notificationRepository.findByUserIdAndTypeInOrderByCreatedAtDesc(userId, targetTypes, pageable)
                .map(this::convertToDTO);
    }

    public long getUnreadCount(String userId, String scope) {
        return notificationRepository.countByUserIdAndIsReadFalseAndTypeIn(userId, getTypesByScope(scope));
    }

    @Transactional
    public void markAllAsRead(String userId, String scope) {
        notificationRepository.markAllAsReadByType(userId, getTypesByScope(scope));
    }

    @Transactional
    public void markAsRead(Long id, String userId) {
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("error.notification.notFound");
        }
        notificationRepository.markAsRead(id, userId);
    }

    // --- PHẦN 2: LOGIC GHI (CREATE/SEND) ---

    /**
     * @param key  key prefix, e.g. "notification.booking.created" (".title" / ".message" are appended)
     * @param args MessageFormat arguments ({0}, {1}, …); strings and numbers are stored as JSON
     */
    @Transactional
    public void sendNotification(String userId, NotificationType type, String relatedEntityId, String key, Object... args) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound"));
        notificationRepository.save(build(user, type, relatedEntityId, key, args));
    }

    /** Same notification for every admin (each rendered in that admin's language). */
    @Transactional
    public void sendToAllAdmins(NotificationType type, String relatedEntityId, String key, Object... args) {
        List<User> admins = userRepository.findByRoleName("ADMIN");
        if (admins.isEmpty()) {
            log.warn("No admin account to notify about {}", key);
            return;
        }
        notificationRepository.saveAll(admins.stream()
                .map(admin -> build(admin, type, relatedEntityId, key, args))
                .collect(Collectors.toList()));
    }

    private Notification build(User user, NotificationType type, String relatedEntityId, String key, Object[] args) {
        Locale locale = userLocales.of(user);
        return Notification.builder()
                .user(user)
                .titleKey(key + ".title")
                .messageKey(key + ".message")
                .params(toJson(args))
                .title(messages.t(locale, key + ".title", args))
                .message(messages.t(locale, key + ".message", args))
                .type(type)
                .isRead(false)
                .relatedEntityId(relatedEntityId)
                .build();
    }

    // --- HELPER METHODS ---

    private List<NotificationType> getTypesByScope(String scope) {
        if ("OWNER".equalsIgnoreCase(scope)) return OWNER_TYPES;
        if ("CUSTOMER".equalsIgnoreCase(scope)) return CUSTOMER_TYPES;
        if ("ADMIN".equalsIgnoreCase(scope)) return ADMIN_TYPES;
        return Arrays.asList(NotificationType.values());
    }

    private NotificationResponseDTO convertToDTO(Notification n) {
        Object[] args = fromJson(n.getParams());
        // Render in the reader's language (Accept-Language) when the key is known; else the stored copy
        String title = n.getTitleKey() != null ? messages.t(n.getTitleKey(), args) : n.getTitle();
        String message = n.getMessageKey() != null ? messages.t(n.getMessageKey(), args) : n.getMessage();
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .title(title)
                .message(message)
                .type(n.getType())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .relatedEntityId(n.getRelatedEntityId())
                .build();
    }

    private String toJson(Object[] args) {
        if (args == null || args.length == 0) return null;
        try {
            return objectMapper.writeValueAsString(args);
        } catch (JsonProcessingException e) {
            log.warn("Cannot serialise notification params", e);
            return null;
        }
    }

    private Object[] fromJson(String json) {
        if (json == null || json.isBlank()) return new Object[0];
        try {
            return objectMapper.readValue(json, Object[].class);
        } catch (JsonProcessingException e) {
            log.warn("Cannot read notification params {}", json, e);
            return new Object[0];
        }
    }
}
