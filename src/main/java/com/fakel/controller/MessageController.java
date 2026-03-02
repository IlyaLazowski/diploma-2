package com.fakel.controller;

import com.fakel.dto.*;
import com.fakel.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MessageController {

    @Autowired
    private MessageService messageService;

    // ============= ДЛЯ ПРЕПОДАВАТЕЛЯ =============

    /**
     * POST /api/teacher/messages
     * Отправка сообщения курсанту (только преподаватель)
     */
    @PostMapping("/teacher/messages")
    @PreAuthorize("hasRole('TEACHER')")
    public MessageDto sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SendMessageRequest request) {
        return messageService.sendMessage(userDetails, request);
    }

    /**
     * GET /api/teacher/messages/sent
     * История отправленных сообщений преподавателя
     */
    @GetMapping("/teacher/messages/sent")
    @PreAuthorize("hasRole('TEACHER')")
    public Page<MessageDto> getSentMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort sort = Sort.by("sentAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return messageService.getSentMessages(userDetails, dateFrom, dateTo, unreadOnly, pageable);
    }

    /**
     * GET /api/teacher/messages/{id}/read-status
     * Проверить прочитано ли сообщение
     */
    @GetMapping("/teacher/messages/{id}/read-status")
    @PreAuthorize("hasRole('TEACHER')")
    public Boolean isMessageRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return messageService.isMessageRead(id, userDetails);
    }

    /**
     * DELETE /api/teacher/messages
     * Удалить отправленные сообщения
     */
    @DeleteMapping("/teacher/messages")
    @PreAuthorize("hasRole('TEACHER')")
    public void deleteSentMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeleteMessagesRequest request) {
        messageService.deleteSentMessages(userDetails, request);
    }

    /**
     * GET /api/teacher/messages/stats
     * Статистика по отправленным сообщениям
     */
    @GetMapping("/teacher/messages/stats")
    @PreAuthorize("hasRole('TEACHER')")
    public Map<String, Object> getSentMessagesStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return messageService.getSentMessagesStats(userDetails);
    }

    // ============= ДЛЯ КУРСАНТА =============

    /**
     * GET /api/cadet/messages
     * Получить все сообщения курсанта (с фильтрацией)
     */
    @GetMapping("/cadet/messages")
    @PreAuthorize("hasRole('CADET')")
    public Page<MessageDto> getMyMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort sort = Sort.by("sentAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return messageService.getMyMessages(userDetails, dateFrom, dateTo, unreadOnly, pageable);
    }

    /**
     * GET /api/cadet/messages/unread/count
     * Количество непрочитанных сообщений
     */
    @GetMapping("/cadet/messages/unread/count")
    @PreAuthorize("hasRole('CADET')")
    public Long getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        return messageService.getUnreadCount(userDetails);
    }

    /**
     * GET /api/cadet/messages/{id}
     * Получить конкретное сообщение
     */
    @GetMapping("/cadet/messages/{id}")
    @PreAuthorize("hasRole('CADET')")
    public MessageDto getMessageById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return messageService.getMessageById(userDetails, id);
    }

    /**
     * PATCH /api/cadet/messages/status
     * Пометить сообщения как прочитанные/непрочитанные
     */
    @PatchMapping("/cadet/messages/status")
    @PreAuthorize("hasRole('CADET')")
    public void updateMessagesStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateMessageStatusRequest request) {
        messageService.updateMessagesStatus(userDetails, request);
    }

    /**
     * DELETE /api/cadet/messages
     * Удалить сообщения
     */
    @DeleteMapping("/cadet/messages")
    @PreAuthorize("hasRole('CADET')")
    public void deleteMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeleteMessagesRequest request) {
        messageService.deleteMessages(userDetails, request);
    }
}