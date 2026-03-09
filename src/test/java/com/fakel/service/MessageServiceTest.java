package com.fakel.service;

import com.fakel.dto.*;
import com.fakel.model.*;
import com.fakel.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private CadetRepository cadetRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private MessageService messageService;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    private Teacher testTeacher;
    private Cadet testCadet;
    private User teacherUser;
    private User cadetUser;
    private Message testMessage;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // Создаем пользователя-преподавателя
        teacherUser = new User();
        teacherUser.setId(1L);
        teacherUser.setLogin("teacher");
        teacherUser.setFirstName("Иван");
        teacherUser.setLastName("Петров");
        teacherUser.setPatronymic("Иванович");

        // Создаем преподавателя
        testTeacher = new Teacher();
        testTeacher.setUserId(1L);
        testTeacher.setUser(teacherUser);

        // Создаем пользователя-курсанта
        cadetUser = new User();
        cadetUser.setId(2L);
        cadetUser.setLogin("cadet");
        cadetUser.setFirstName("Сергей");
        cadetUser.setLastName("Иванов");
        cadetUser.setPatronymic("Петрович");

        // Создаем курсанта
        testCadet = new Cadet();
        testCadet.setUserId(2L);
        testCadet.setUser(cadetUser);

        // Создаем тестовое сообщение
        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setSender(teacherUser);
        testMessage.setRecipient(cadetUser);
        testMessage.setTopic("Тестовая тема");
        testMessage.setText("Тестовый текст сообщения");
        testMessage.setSentAt(now);
        testMessage.setIsReading(false);

        // Настройка UserDetails
        lenient().when(userDetails.getUsername()).thenReturn("teacher");
    }

    // ============= ТЕСТЫ ДЛЯ sendMessage =============

    @Test
    void sendMessage_WithValidData_ShouldSendMessage() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setRecipientId(2L);
        request.setTopic("Новая тема");
        request.setText("Новый текст");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(userRepository.findById(2L)).thenReturn(Optional.of(cadetUser));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(10L);
            return msg;
        });

        // When
        MessageDto result = messageService.sendMessage(userDetails, request);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Новая тема", result.getTopic());
        assertEquals("Новый текст", result.getText());
        assertEquals(1L, result.getSenderId());
        assertEquals(2L, result.getRecipientId());

        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void sendMessage_WithNullUserDetails_ShouldThrowException() {
        // Given
        SendMessageRequest request = new SendMessageRequest();

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendMessage(null, request));
    }

    @Test
    void sendMessage_WithNullRequest_ShouldThrowException() {
        // Given
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendMessage(userDetails, null));
    }

    @Test
    void sendMessage_WithInvalidRecipientId_ShouldThrowException() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setRecipientId(null);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendMessage(userDetails, request));
    }

    @Test
    void sendMessage_WithEmptyTopic_ShouldThrowException() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setRecipientId(2L);
        request.setTopic("   ");
        request.setText("Текст");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendMessage(userDetails, request));
    }

    @Test
    void sendMessage_WithTopicTooLong_ShouldThrowException() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setRecipientId(2L);
        request.setTopic("a".repeat(257));
        request.setText("Текст");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendMessage(userDetails, request));
    }

    @Test
    void sendMessage_WithEmptyText_ShouldThrowException() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setRecipientId(2L);
        request.setTopic("Тема");
        request.setText("   ");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendMessage(userDetails, request));
    }

    @Test
    void sendMessage_WithTextTooLong_ShouldThrowException() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setRecipientId(2L);
        request.setTopic("Тема");
        request.setText("a".repeat(10001));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendMessage(userDetails, request));
    }

    @Test
    void sendMessage_WhenTeacherNotFound_ShouldThrowException() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setRecipientId(2L);
        request.setTopic("Тема");
        request.setText("Текст");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> messageService.sendMessage(userDetails, request));
    }

    @Test
    void sendMessage_WhenRecipientNotFound_ShouldThrowException() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setRecipientId(999L);
        request.setTopic("Тема");
        request.setText("Текст");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> messageService.sendMessage(userDetails, request));
    }

    // ============= ТЕСТЫ ДЛЯ getMyMessages =============

    @Test
    void getMyMessages_AllMessages_ShouldReturnAll() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> messages = List.of(testMessage);
        Page<Message> expectedPage = new PageImpl<>(messages);

        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(messageRepository.findByRecipientIdOrderBySentAtDesc(eq(2L), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MessageDto> result = messageService.getMyMessages(
                userDetails, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getMyMessages_UnreadOnly_ShouldReturnUnread() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> messages = List.of(testMessage);
        Page<Message> expectedPage = new PageImpl<>(messages);

        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(messageRepository.findByRecipientIdAndIsReadingFalseOrderBySentAtDesc(eq(2L), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MessageDto> result = messageService.getMyMessages(
                userDetails, null, null, true, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getMyMessages_WithDateRange_ShouldReturnFiltered() {
        // Given
        LocalDate dateFrom = LocalDate.now().minusDays(10);
        LocalDate dateTo = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> messages = List.of(testMessage);
        Page<Message> expectedPage = new PageImpl<>(messages);

        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(messageRepository.findByRecipientIdAndSentAtBetweenOrderBySentAtDesc(
                eq(2L), any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MessageDto> result = messageService.getMyMessages(
                userDetails, dateFrom, dateTo, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getMyMessages_WithInvalidDateRange_ShouldThrowException() {
        // Given
        LocalDate dateFrom = LocalDate.now();
        LocalDate dateTo = LocalDate.now().minusDays(10);
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.getMyMessages(userDetails, dateFrom, dateTo, null, pageable));
    }

    @Test
    void getMyMessages_WithNullUserDetails_ShouldThrowException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.getMyMessages(null, null, null, null, pageable));
    }

    @Test
    void getMyMessages_WithNullPageable_ShouldThrowException() {
        // Given
        // В сервисе сначала используется pageable.getPageNumber(), поэтому NullPointerException
        assertThrows(NullPointerException.class,
                () -> messageService.getMyMessages(userDetails, null, null, null, null));
    }

    // ============= ТЕСТЫ ДЛЯ getMessageById =============

    @Test
    void getMessageById_ValidId_ShouldReturnMessage() {
        // Given
        Long messageId = 1L;

        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));

        // When
        MessageDto result = messageService.getMessageById(userDetails, messageId);

        // Then
        assertNotNull(result);
        assertEquals(messageId, result.getId());
        assertEquals("Тестовая тема", result.getTopic());
    }

    @Test
    void getMessageById_InvalidId_ShouldThrowException() {
        // Given
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.getMessageById(userDetails, null));
    }

    @Test
    void getMessageById_MessageNotFound_ShouldThrowException() {
        // Given
        Long messageId = 999L;

        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> messageService.getMessageById(userDetails, messageId));
    }

    @Test
    void getMessageById_NoAccess_ShouldThrowException() {
        // Given
        Long messageId = 1L;
        User differentUser = new User();
        differentUser.setId(3L);
        testMessage.setRecipient(differentUser);

        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> messageService.getMessageById(userDetails, messageId));
    }

    // ============= ТЕСТЫ ДЛЯ updateMessagesStatus =============

    @Test
    void updateMessagesStatus_ValidRequest_ShouldUpdate() {
        // Given
        UpdateMessageStatusRequest request = new UpdateMessageStatusRequest();
        request.setMessageIds(List.of(1L, 2L));
        request.setIsReading(true);

        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(messageRepository.updateStatusForRecipient(anyList(), eq(2L), eq(true))).thenReturn(2);

        // When
        messageService.updateMessagesStatus(userDetails, request);

        // Then
        verify(messageRepository, times(1)).updateStatusForRecipient(anyList(), eq(2L), eq(true));
    }

    @Test
    void updateMessagesStatus_WithInvalidIds_ShouldThrowException() {
        // Given
        UpdateMessageStatusRequest request = new UpdateMessageStatusRequest();
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(null);
        request.setMessageIds(ids);
        request.setIsReading(true);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.updateMessagesStatus(userDetails, request));
    }

    @Test
    void updateMessagesStatus_WithNullRequest_ShouldThrowException() {
        // Given
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.updateMessagesStatus(userDetails, null));
    }

    @Test
    void updateMessagesStatus_WhenNotAllUpdated_ShouldThrowException() {
        // Given
        UpdateMessageStatusRequest request = new UpdateMessageStatusRequest();
        request.setMessageIds(List.of(1L, 2L));
        request.setIsReading(true);

        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(messageRepository.updateStatusForRecipient(anyList(), eq(2L), eq(true))).thenReturn(1); // только 1 из 2

        // When & Then
        assertThrows(RuntimeException.class,
                () -> messageService.updateMessagesStatus(userDetails, request));
    }

    // ============= ТЕСТЫ ДЛЯ deleteMessages =============

    @Test
    void deleteMessages_ValidRequest_ShouldDelete() {
        // Given
        DeleteMessagesRequest request = new DeleteMessagesRequest();
        request.setMessageIds(List.of(1L, 2L));

        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(messageRepository.deleteForRecipient(anyList(), eq(2L))).thenReturn(2);

        // When
        messageService.deleteMessages(userDetails, request);

        // Then
        verify(messageRepository, times(1)).deleteForRecipient(anyList(), eq(2L));
    }

    @Test
    void deleteMessages_WithInvalidIds_ShouldThrowException() {
        // Given
        DeleteMessagesRequest request = new DeleteMessagesRequest();
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(null);
        request.setMessageIds(ids);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> messageService.deleteMessages(userDetails, request));
    }

    @Test
    void deleteMessages_WhenNotAllDeleted_ShouldThrowException() {
        // Given
        DeleteMessagesRequest request = new DeleteMessagesRequest();
        request.setMessageIds(List.of(1L, 2L));

        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(messageRepository.deleteForRecipient(anyList(), eq(2L))).thenReturn(1); // только 1 из 2

        // When & Then
        assertThrows(RuntimeException.class,
                () -> messageService.deleteMessages(userDetails, request));
    }

    // ============= ТЕСТЫ ДЛЯ getSentMessages =============

    @Test
    void getSentMessages_AllMessages_ShouldReturnAll() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> messages = List.of(testMessage);
        Page<Message> expectedPage = new PageImpl<>(messages);

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(messageRepository.findBySenderIdOrderBySentAtDesc(eq(1L), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MessageDto> result = messageService.getSentMessages(
                userDetails, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getSentMessages_UnreadOnly_ShouldReturnUnread() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> messages = List.of(testMessage);
        Page<Message> expectedPage = new PageImpl<>(messages);

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(messageRepository.findBySenderIdAndIsReadingFalseOrderBySentAtDesc(eq(1L), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MessageDto> result = messageService.getSentMessages(
                userDetails, null, null, true, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getSentMessages_WithDateRange_ShouldReturnFiltered() {
        // Given
        LocalDate dateFrom = LocalDate.now().minusDays(10);
        LocalDate dateTo = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> messages = List.of(testMessage);
        Page<Message> expectedPage = new PageImpl<>(messages);

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(messageRepository.findBySenderIdAndSentAtBetweenOrderBySentAtDesc(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<MessageDto> result = messageService.getSentMessages(
                userDetails, dateFrom, dateTo, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ============= ТЕСТЫ ДЛЯ deleteSentMessages =============

    @Test
    void deleteSentMessages_ValidRequest_ShouldDelete() {
        // Given
        DeleteMessagesRequest request = new DeleteMessagesRequest();
        request.setMessageIds(List.of(1L, 2L));

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(messageRepository.deleteForSender(anyList(), eq(1L))).thenReturn(2);

        // When
        messageService.deleteSentMessages(userDetails, request);

        // Then
        verify(messageRepository, times(1)).deleteForSender(anyList(), eq(1L));
    }

    @Test
    void deleteSentMessages_WhenNotAllDeleted_ShouldThrowException() {
        // Given
        DeleteMessagesRequest request = new DeleteMessagesRequest();
        request.setMessageIds(List.of(1L, 2L));

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(messageRepository.deleteForSender(anyList(), eq(1L))).thenReturn(1); // только 1 из 2

        // When & Then
        assertThrows(RuntimeException.class,
                () -> messageService.deleteSentMessages(userDetails, request));
    }

    // ============= ТЕСТЫ ДЛЯ isMessageRead =============

    @Test
    void isMessageRead_ValidId_ShouldReturnStatus() {
        // Given
        Long messageId = 1L;

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));

        // When
        Boolean result = messageService.isMessageRead(messageId, userDetails);

        // Then
        assertFalse(result);
    }

    @Test
    void isMessageRead_WhenNotSender_ShouldThrowException() {
        // Given
        Long messageId = 1L;
        User differentUser = new User();
        differentUser.setId(3L);
        testMessage.setSender(differentUser);

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> messageService.isMessageRead(messageId, userDetails));
    }

    // ============= ТЕСТЫ ДЛЯ getSentMessagesStats =============

    @Test
    void getSentMessagesStats_ShouldReturnStats() {
        // Given
        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(messageRepository.countBySenderId(1L)).thenReturn(10L);
        when(messageRepository.countBySenderIdAndIsReadingTrue(1L)).thenReturn(6L);
        when(messageRepository.countBySenderIdAndIsReadingFalse(1L)).thenReturn(4L);

        // When
        Map<String, Object> result = messageService.getSentMessagesStats(userDetails);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.get("totalSent"));
        assertEquals(6L, result.get("read"));
        assertEquals(4L, result.get("unread"));
        assertEquals(60L, result.get("readPercentage")); // ← изменил на 60L
    }

    // ============= ТЕСТЫ ДЛЯ getUnreadCount =============

    @Test
    void getUnreadCount_ShouldReturnCount() {
        // Given
        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(messageRepository.countByRecipientIdAndIsReadingFalse(2L)).thenReturn(5L);

        // When
        Long result = messageService.getUnreadCount(userDetails);

        // Then
        assertEquals(5L, result);
    }
}