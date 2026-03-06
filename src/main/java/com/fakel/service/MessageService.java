package com.fakel.service;

import com.fakel.dto.*;
import com.fakel.model.*;
import com.fakel.repository.*;
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

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getRecipientId() == null || request.getRecipientId() <= 0) {
            throw new IllegalArgumentException("ID получателя должен быть положительным числом");
        }

        if (request.getTopic() == null || request.getTopic().trim().isEmpty()) {
            throw new IllegalArgumentException("Тема сообщения не может быть пустой");
        }

        if (request.getTopic().length() > 256) {
            throw new IllegalArgumentException("Тема сообщения не может быть длиннее 256 символов");
        }

        if (request.getText() == null || request.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Текст сообщения не может быть пустым");
        }

        if (request.getText().length() > 10000) {
            throw new IllegalArgumentException("Текст сообщения не может быть длиннее 10000 символов");
        }

        // Проверяем, что отправитель - преподаватель
        Teacher sender = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Только преподаватель может отправлять сообщения"));

        // Проверяем, что получатель существует
        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new RuntimeException("Получатель не найден"));

        // Создаем сообщение
        Message message = new Message();
        message.setSender(sender.getUser());
        message.setRecipient(recipient);
        message.setTopic(request.getTopic().trim());
        message.setText(request.getText().trim());
        message.setSentAt(LocalDateTime.now());
        message.setIsReading(false);

        Message saved = messageRepository.save(message);
        return convertToDto(saved);
    }

    // ============= ПОЛУЧЕНИЕ СООБЩЕНИЙ ДЛЯ КУРСАНТА =============

    @Transactional(readOnly = true)
    public Page<MessageDto> getMyMessages(UserDetails userDetails,
                                          LocalDate dateFrom,
                                          LocalDate dateTo,
                                          Boolean unreadOnly,
                                          Pageable pageable) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (pageable == null) {
            throw new IllegalArgumentException("Параметры пагинации не могут быть пустыми");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        Page<Message> messages;

        if (unreadOnly != null && unreadOnly) {
            // Только непрочитанные
            messages = messageRepository.findByRecipientIdAndIsReadingFalseOrderBySentAtDesc(
                    cadet.getUserId(), pageable);
        } else if (dateFrom != null && dateTo != null) {
            // Фильтр по дате
            LocalDateTime start = dateFrom.atStartOfDay();
            LocalDateTime end = dateTo.atTime(LocalTime.MAX);
            messages = messageRepository.findByRecipientIdAndSentAtBetweenOrderBySentAtDesc(
                    cadet.getUserId(), start, end, pageable);
        } else {
            // Все сообщения
            messages = messageRepository.findByRecipientIdOrderBySentAtDesc(cadet.getUserId(), pageable);
        }

        return messages.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public MessageDto getMessageById(UserDetails userDetails, Long messageId) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("ID сообщения должен быть положительным числом");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));

        if (!message.getRecipient().getId().equals(cadet.getUserId())) {
            throw new RuntimeException("Нет доступа к этому сообщению");
        }

        return convertToDto(message);
    }

    // ============= ПОМЕТИТЬ КАК ПРОЧИТАННОЕ/НЕПРОЧИТАННОЕ =============

    @Transactional
    public void updateMessagesStatus(UserDetails userDetails, UpdateMessageStatusRequest request) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getMessageIds() == null || request.getMessageIds().isEmpty()) {
            throw new IllegalArgumentException("Список ID сообщений не может быть пустым");
        }

        for (Long id : request.getMessageIds()) {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("ID сообщения должен быть положительным числом");
            }
        }

        if (request.getIsReading() == null) {
            throw new IllegalArgumentException("Статус прочтения не может быть null");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        int updated = messageRepository.updateStatusForRecipient(
                request.getMessageIds(), cadet.getUserId(), request.getIsReading());

        if (updated != request.getMessageIds().size()) {
            throw new RuntimeException("Некоторые сообщения не найдены или не принадлежат курсанту");
        }
    }

    // ============= УДАЛЕНИЕ СООБЩЕНИЙ =============

    @Transactional
    public void deleteMessages(UserDetails userDetails, DeleteMessagesRequest request) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getMessageIds() == null || request.getMessageIds().isEmpty()) {
            throw new IllegalArgumentException("Список ID сообщений не может быть пустым");
        }

        for (Long id : request.getMessageIds()) {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("ID сообщения должен быть положительным числом");
            }
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        int deleted = messageRepository.deleteForRecipient(request.getMessageIds(), cadet.getUserId());

        if (deleted != request.getMessageIds().size()) {
            throw new RuntimeException("Некоторые сообщения не найдены или не принадлежат курсанту");
        }
    }

    // ============= ДЛЯ ПРЕПОДАВАТЕЛЯ - ИСТОРИЯ ОТПРАВЛЕННЫХ =============

    @Transactional(readOnly = true)
    public Page<MessageDto> getSentMessages(UserDetails userDetails,
                                            LocalDate dateFrom,
                                            LocalDate dateTo,
                                            Boolean showOnlyUnread,
                                            Pageable pageable) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (pageable == null) {
            throw new IllegalArgumentException("Параметры пагинации не могут быть пустыми");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Page<Message> messages;

        if (dateFrom != null && dateTo != null) {
            LocalDateTime start = dateFrom.atStartOfDay();
            LocalDateTime end = dateTo.atTime(LocalTime.MAX);

            if (showOnlyUnread != null && showOnlyUnread) {
                // Только непрочитанные получателем
                messages = messageRepository.findBySenderIdAndSentAtBetweenAndIsReadingFalseOrderBySentAtDesc(
                        teacher.getUserId(), start, end, pageable);
            } else {
                messages = messageRepository.findBySenderIdAndSentAtBetweenOrderBySentAtDesc(
                        teacher.getUserId(), start, end, pageable);
            }
        } else {
            if (showOnlyUnread != null && showOnlyUnread) {
                messages = messageRepository.findBySenderIdAndIsReadingFalseOrderBySentAtDesc(
                        teacher.getUserId(), pageable);
            } else {
                messages = messageRepository.findBySenderIdOrderBySentAtDesc(teacher.getUserId(), pageable);
            }
        }

        return messages.map(this::convertToDto);
    }

    @Transactional
    public void deleteSentMessages(UserDetails userDetails, DeleteMessagesRequest request) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getMessageIds() == null || request.getMessageIds().isEmpty()) {
            throw new IllegalArgumentException("Список ID сообщений не может быть пустым");
        }

        for (Long id : request.getMessageIds()) {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("ID сообщения должен быть положительным числом");
            }
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        int deleted = messageRepository.deleteForSender(request.getMessageIds(), teacher.getUserId());

        if (deleted != request.getMessageIds().size()) {
            throw new RuntimeException("Некоторые сообщения не найдены или не принадлежат преподавателю");
        }
    }

    @Transactional(readOnly = true)
    public Boolean isMessageRead(Long messageId, UserDetails userDetails) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("ID сообщения должен быть положительным числом");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));

        if (!message.getSender().getId().equals(teacher.getUserId())) {
            throw new RuntimeException("Нет доступа к этому сообщению");
        }

        return message.getIsReading();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSentMessagesStats(UserDetails userDetails) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Long totalSent = messageRepository.countBySenderId(teacher.getUserId());
        Long readCount = messageRepository.countBySenderIdAndIsReadingTrue(teacher.getUserId());
        Long unreadCount = messageRepository.countBySenderIdAndIsReadingFalse(teacher.getUserId());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSent", totalSent);
        stats.put("read", readCount);
        stats.put("unread", unreadCount);
        stats.put("readPercentage", totalSent != null && totalSent > 0 ? (readCount * 100 / totalSent) : 0);

        return stats;
    }

    // ============= КОЛИЧЕСТВО НЕПРОЧИТАННЫХ =============

    @Transactional(readOnly = true)
    public Long getUnreadCount(UserDetails userDetails) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        return messageRepository.countByRecipientIdAndIsReadingFalse(cadet.getUserId());
    }

    // ============= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =============

    private MessageDto convertToDto(Message message) {
        if (message == null) {
            return null;
        }

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