package com.fakel.service;

import com.fakel.dto.*;
import com.fakel.model.*;
import com.fakel.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CadetRepository cadetRepository;

    // ============= ОТПРАВКА СООБЩЕНИЯ (ТОЛЬКО ПРЕПОДАВАТЕЛЬ) =============

    @Transactional
    public MessageDto sendMessage(UserDetails userDetails, SendMessageRequest request) {

        log.info("Отправка сообщения: отправитель={}, получатель={}, тема={}",
                userDetails != null ? userDetails.getUsername() : null,
                request != null ? request.getRecipientId() : null,
                request != null ? request.getTopic() : null);

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка отправки сообщения с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            log.warn("Попытка отправки сообщения с null request");
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getRecipientId() == null || request.getRecipientId() <= 0) {
            log.warn("Некорректный ID получателя: {}", request.getRecipientId());
            throw new IllegalArgumentException("ID получателя должен быть положительным числом");
        }

        if (request.getTopic() == null || request.getTopic().trim().isEmpty()) {
            log.warn("Тема сообщения пустая");
            throw new IllegalArgumentException("Тема сообщения не может быть пустой");
        }

        String trimmedTopic = request.getTopic().trim();
        if (trimmedTopic.length() > 256) {
            log.warn("Тема слишком длинная: {} символов", trimmedTopic.length());
            throw new IllegalArgumentException("Тема сообщения не может быть длиннее 256 символов");
        }

        if (request.getText() == null || request.getText().trim().isEmpty()) {
            log.warn("Текст сообщения пустой");
            throw new IllegalArgumentException("Текст сообщения не может быть пустым");
        }

        String trimmedText = request.getText().trim();
        if (trimmedText.length() > 10000) {
            log.warn("Текст слишком длинный: {} символов", trimmedText.length());
            throw new IllegalArgumentException("Текст сообщения не может быть длиннее 10000 символов");
        }

        // Проверяем, что отправитель - преподаватель
        Teacher sender = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Только преподаватель может отправлять сообщения");
                });

        // Проверяем, что получатель существует
        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> {
                    log.warn("Получатель не найден: {}", request.getRecipientId());
                    return new RuntimeException("Получатель не найден");
                });

        log.debug("Отправитель: {} (ID={}), получатель: {} (ID={})",
                sender.getUser().getLastName(), sender.getUserId(),
                recipient.getLastName(), recipient.getId());

        // Создаем сообщение
        Message message = new Message();
        message.setSender(sender.getUser());
        message.setRecipient(recipient);
        message.setTopic(trimmedTopic);
        message.setText(trimmedText);
        message.setSentAt(LocalDateTime.now());
        message.setIsReading(false);

        Message saved = messageRepository.save(message);
        log.info("Сообщение успешно отправлено, ID: {}", saved.getId());

        return convertToDto(saved);
    }

    // ============= ПОЛУЧЕНИЕ СООБЩЕНИЙ ДЛЯ КУРСАНТА =============

    @Transactional(readOnly = true)
    public Page<MessageDto> getMyMessages(UserDetails userDetails,
                                          LocalDate dateFrom,
                                          LocalDate dateTo,
                                          Boolean unreadOnly,
                                          Pageable pageable) {

        log.info("Получение сообщений курсанта: user={}, dateFrom={}, dateTo={}, unreadOnly={}, page={}, size={}",
                userDetails != null ? userDetails.getUsername() : null, dateFrom, dateTo, unreadOnly,
                pageable.getPageNumber(), pageable.getPageSize());

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения сообщений с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (pageable == null) {
            log.warn("Параметры пагинации null");
            throw new IllegalArgumentException("Параметры пагинации не могут быть пустыми");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            log.warn("Некорректный диапазон дат: dateFrom={} > dateTo={}", dateFrom, dateTo);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Курсант не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Курсант не найден");
                });

        log.debug("Курсант ID: {}", cadet.getUserId());

        Page<Message> messages;

        if (unreadOnly != null && unreadOnly) {
            log.debug("Режим: только непрочитанные");
            messages = messageRepository.findByRecipientIdAndIsReadingFalseOrderBySentAtDesc(
                    cadet.getUserId(), pageable);
        } else if (dateFrom != null && dateTo != null) {
            log.debug("Режим: фильтр по датам {} - {}", dateFrom, dateTo);
            LocalDateTime start = dateFrom.atStartOfDay();
            LocalDateTime end = dateTo.atTime(LocalTime.MAX);
            messages = messageRepository.findByRecipientIdAndSentAtBetweenOrderBySentAtDesc(
                    cadet.getUserId(), start, end, pageable);
        } else {
            log.debug("Режим: все сообщения");
            messages = messageRepository.findByRecipientIdOrderBySentAtDesc(cadet.getUserId(), pageable);
        }

        log.info("Найдено {} сообщений для курсанта {}", messages.getTotalElements(), cadet.getUserId());
        log.debug("Всего страниц: {}", messages.getTotalPages());

        return messages.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public MessageDto getMessageById(UserDetails userDetails, Long messageId) {

        log.info("Получение сообщения по ID: {}, пользователь: {}", messageId,
                userDetails != null ? userDetails.getUsername() : null);

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения сообщения с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (messageId == null || messageId <= 0) {
            log.warn("Некорректный ID сообщения: {}", messageId);
            throw new IllegalArgumentException("ID сообщения должен быть положительным числом");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Курсант не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Курсант не найден");
                });

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    log.warn("Сообщение не найдено: {}", messageId);
                    return new RuntimeException("Сообщение не найдено");
                });

        if (!message.getRecipient().getId().equals(cadet.getUserId())) {
            log.warn("Нет доступа к сообщению {} для курсанта {}", messageId, cadet.getUserId());
            throw new RuntimeException("Нет доступа к этому сообщению");
        }

        log.debug("Сообщение найдено: отправитель={}, тема={}, прочитано={}",
                message.getSender().getLastName(), message.getTopic(), message.getIsReading());

        return convertToDto(message);
    }

    // ============= ПОМЕТИТЬ КАК ПРОЧИТАННОЕ/НЕПРОЧИТАННОЕ =============

    @Transactional
    public void updateMessagesStatus(UserDetails userDetails, UpdateMessageStatusRequest request) {

        log.info("Обновление статуса сообщений: user={}, count={}, isReading={}",
                userDetails != null ? userDetails.getUsername() : null,
                request != null && request.getMessageIds() != null ? request.getMessageIds().size() : null,
                request != null ? request.getIsReading() : null);

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка обновления статуса с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            log.warn("Попытка обновления статуса с null request");
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getMessageIds() == null || request.getMessageIds().isEmpty()) {
            log.warn("Пустой список ID сообщений");
            throw new IllegalArgumentException("Список ID сообщений не может быть пустым");
        }

        for (Long id : request.getMessageIds()) {
            if (id == null || id <= 0) {
                log.warn("Некорректный ID сообщения в списке: {}", id);
                throw new IllegalArgumentException("ID сообщения должен быть положительным числом");
            }
        }

        if (request.getIsReading() == null) {
            log.warn("Статус прочтения null");
            throw new IllegalArgumentException("Статус прочтения не может быть null");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Курсант не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Курсант не найден");
                });

        log.debug("Обновление статуса для {} сообщений курсанта {}", request.getMessageIds().size(), cadet.getUserId());

        int updated = messageRepository.updateStatusForRecipient(
                request.getMessageIds(), cadet.getUserId(), request.getIsReading());

        if (updated != request.getMessageIds().size()) {
            log.warn("Обновлено только {} из {} сообщений", updated, request.getMessageIds().size());
            throw new RuntimeException("Некоторые сообщения не найдены или не принадлежат курсанту");
        }

        log.info("Статус успешно обновлен для {} сообщений", updated);
    }

    // ============= УДАЛЕНИЕ СООБЩЕНИЙ =============

    @Transactional
    public void deleteMessages(UserDetails userDetails, DeleteMessagesRequest request) {

        log.info("Удаление сообщений: user={}, count={}",
                userDetails != null ? userDetails.getUsername() : null,
                request != null && request.getMessageIds() != null ? request.getMessageIds().size() : null);

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка удаления сообщений с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            log.warn("Попытка удаления сообщений с null request");
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getMessageIds() == null || request.getMessageIds().isEmpty()) {
            log.warn("Пустой список ID сообщений для удаления");
            throw new IllegalArgumentException("Список ID сообщений не может быть пустым");
        }

        for (Long id : request.getMessageIds()) {
            if (id == null || id <= 0) {
                log.warn("Некорректный ID сообщения в списке для удаления: {}", id);
                throw new IllegalArgumentException("ID сообщения должен быть положительным числом");
            }
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Курсант не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Курсант не найден");
                });

        log.debug("Удаление {} сообщений курсанта {}", request.getMessageIds().size(), cadet.getUserId());

        int deleted = messageRepository.deleteForRecipient(request.getMessageIds(), cadet.getUserId());

        if (deleted != request.getMessageIds().size()) {
            log.warn("Удалено только {} из {} сообщений", deleted, request.getMessageIds().size());
            throw new RuntimeException("Некоторые сообщения не найдены или не принадлежат курсанту");
        }

        log.info("Успешно удалено {} сообщений", deleted);
    }

    // ============= ДЛЯ ПРЕПОДАВАТЕЛЯ - ИСТОРИЯ ОТПРАВЛЕННЫХ =============

    @Transactional(readOnly = true)
    public Page<MessageDto> getSentMessages(UserDetails userDetails,
                                            LocalDate dateFrom,
                                            LocalDate dateTo,
                                            Boolean showOnlyUnread,
                                            Pageable pageable) {

        log.info("Получение отправленных сообщений преподавателя: user={}, dateFrom={}, dateTo={}, showOnlyUnread={}, page={}, size={}",
                userDetails != null ? userDetails.getUsername() : null, dateFrom, dateTo, showOnlyUnread,
                pageable.getPageNumber(), pageable.getPageSize());

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения отправленных сообщений с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (pageable == null) {
            log.warn("Параметры пагинации null");
            throw new IllegalArgumentException("Параметры пагинации не могут быть пустыми");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            log.warn("Некорректный диапазон дат: dateFrom={} > dateTo={}", dateFrom, dateTo);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        log.debug("Преподаватель ID: {}", teacher.getUserId());

        Page<Message> messages;

        if (dateFrom != null && dateTo != null) {
            LocalDateTime start = dateFrom.atStartOfDay();
            LocalDateTime end = dateTo.atTime(LocalTime.MAX);
            log.debug("Диапазон дат: {} - {}", start, end);

            if (showOnlyUnread != null && showOnlyUnread) {
                log.debug("Режим: только непрочитанные получателем");
                messages = messageRepository.findBySenderIdAndSentAtBetweenAndIsReadingFalseOrderBySentAtDesc(
                        teacher.getUserId(), start, end, pageable);
            } else {
                log.debug("Режим: все отправленные");
                messages = messageRepository.findBySenderIdAndSentAtBetweenOrderBySentAtDesc(
                        teacher.getUserId(), start, end, pageable);
            }
        } else {
            if (showOnlyUnread != null && showOnlyUnread) {
                log.debug("Режим: только непрочитанные получателем");
                messages = messageRepository.findBySenderIdAndIsReadingFalseOrderBySentAtDesc(
                        teacher.getUserId(), pageable);
            } else {
                log.debug("Режим: все отправленные");
                messages = messageRepository.findBySenderIdOrderBySentAtDesc(teacher.getUserId(), pageable);
            }
        }

        log.info("Найдено {} отправленных сообщений для преподавателя {}", messages.getTotalElements(), teacher.getUserId());
        log.debug("Всего страниц: {}", messages.getTotalPages());

        return messages.map(this::convertToDto);
    }

    @Transactional
    public void deleteSentMessages(UserDetails userDetails, DeleteMessagesRequest request) {

        log.info("Удаление отправленных сообщений: user={}, count={}",
                userDetails != null ? userDetails.getUsername() : null,
                request != null && request.getMessageIds() != null ? request.getMessageIds().size() : null);

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка удаления отправленных сообщений с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            log.warn("Попытка удаления отправленных сообщений с null request");
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getMessageIds() == null || request.getMessageIds().isEmpty()) {
            log.warn("Пустой список ID сообщений для удаления");
            throw new IllegalArgumentException("Список ID сообщений не может быть пустым");
        }

        for (Long id : request.getMessageIds()) {
            if (id == null || id <= 0) {
                log.warn("Некорректный ID сообщения в списке: {}", id);
                throw new IllegalArgumentException("ID сообщения должен быть положительным числом");
            }
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        log.debug("Удаление {} отправленных сообщений преподавателя {}", request.getMessageIds().size(), teacher.getUserId());

        int deleted = messageRepository.deleteForSender(request.getMessageIds(), teacher.getUserId());

        if (deleted != request.getMessageIds().size()) {
            log.warn("Удалено только {} из {} сообщений", deleted, request.getMessageIds().size());
            throw new RuntimeException("Некоторые сообщения не найдены или не принадлежат преподавателю");
        }

        log.info("Успешно удалено {} отправленных сообщений", deleted);
    }

    @Transactional(readOnly = true)
    public Boolean isMessageRead(Long messageId, UserDetails userDetails) {

        log.info("Проверка статуса прочтения сообщения: messageId={}, user={}",
                messageId, userDetails != null ? userDetails.getUsername() : null);

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка проверки статуса с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (messageId == null || messageId <= 0) {
            log.warn("Некорректный ID сообщения: {}", messageId);
            throw new IllegalArgumentException("ID сообщения должен быть положительным числом");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    log.warn("Сообщение не найдено: {}", messageId);
                    return new RuntimeException("Сообщение не найдено");
                });

        if (!message.getSender().getId().equals(teacher.getUserId())) {
            log.warn("Нет доступа к сообщению {} для преподавателя {}", messageId, teacher.getUserId());
            throw new RuntimeException("Нет доступа к этому сообщению");
        }

        log.debug("Сообщение {} прочитано: {}", messageId, message.getIsReading());
        return message.getIsReading();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSentMessagesStats(UserDetails userDetails) {

        log.info("Получение статистики отправленных сообщений для пользователя: {}",
                userDetails != null ? userDetails.getUsername() : null);

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения статистики с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        log.debug("Получение статистики для преподавателя {}", teacher.getUserId());

        Long totalSent = messageRepository.countBySenderId(teacher.getUserId());
        Long readCount = messageRepository.countBySenderIdAndIsReadingTrue(teacher.getUserId());
        Long unreadCount = messageRepository.countBySenderIdAndIsReadingFalse(teacher.getUserId());

        log.debug("Статистика: всего={}, прочитано={}, непрочитано={}", totalSent, readCount, unreadCount);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSent", totalSent);
        stats.put("read", readCount);
        stats.put("unread", unreadCount);
        stats.put("readPercentage", totalSent != null && totalSent > 0 ? (readCount * 100 / totalSent) : 0);

        log.info("Статистика успешно получена");
        return stats;
    }

    // ============= КОЛИЧЕСТВО НЕПРОЧИТАННЫХ =============

    @Transactional(readOnly = true)
    public Long getUnreadCount(UserDetails userDetails) {

        log.info("Получение количества непрочитанных сообщений для пользователя: {}",
                userDetails != null ? userDetails.getUsername() : null);

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения количества непрочитанных с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Курсант не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Курсант не найден");
                });

        Long count = messageRepository.countByRecipientIdAndIsReadingFalse(cadet.getUserId());
        log.debug("Непрочитанных сообщений для курсанта {}: {}", cadet.getUserId(), count);

        return count;
    }

    // ============= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =============

    private MessageDto convertToDto(Message message) {
        if (message == null) {
            log.trace("Конвертация null сообщения в null DTO");
            return null;
        }

        log.trace("Конвертация сообщения ID {} в DTO", message.getId());

        String senderName = message.getSender().getLastName() + " " +
                message.getSender().getFirstName() + " " +
                (message.getSender().getPatronymic() != null ? message.getSender().getPatronymic() : "");

        String recipientName = message.getRecipient().getLastName() + " " +
                message.getRecipient().getFirstName() + " " +
                (message.getRecipient().getPatronymic() != null ? message.getRecipient().getPatronymic() : "");

        return new MessageDto(
                message.getId(),
                message.getSender().getId(),
                senderName.trim(),
                message.getRecipient().getId(),
                recipientName.trim(),
                message.getTopic(),
                message.getText(),
                message.getSentAt(),
                message.getIsReading()
        );
    }
}