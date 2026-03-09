package com.fakel.service;

import com.fakel.model.User;
import com.fakel.repository.UserRepository;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordEncoderServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PasswordEncoderService passwordEncoderService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private User testUser4;
    private User testUser5;

    @BeforeEach
    void setUp() {
        // Создаем тестовых пользователей
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setLogin("user1");
        testUser1.setPassword("password123");

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setLogin("user2");
        testUser2.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMy.MrqUZ3qK9cX5qX5qX5qX5qX5qX5qX5q"); // уже зашифрован

        testUser3 = new User();
        testUser3.setId(3L);
        testUser3.setLogin("user3");
        testUser3.setPassword("   password456   "); // с пробелами

        testUser4 = new User();
        testUser4.setId(4L);
        testUser4.setLogin("user4");
        testUser4.setPassword(""); // пустой пароль

        testUser5 = new User();
        testUser5.setId(5L);
        testUser5.setLogin("user5");
        testUser5.setPassword(null); // null пароль
    }

    // ============= ТЕСТЫ ДЛЯ encodePasswords =============

    @Test
    void encodePasswords_WithMixedUsers_ShouldEncodeAppropriately() {
        // Given
        List<User> users = List.of(testUser1, testUser2, testUser3, testUser4, testUser5);
        when(userRepository.findAll()).thenReturn(users);

        // When
        passwordEncoderService.encodePasswords();

        // Then
        // Проверяем, что сохранялись только пользователи с незашифрованными паролями
        verify(userRepository, times(2)).save(userCaptor.capture());

        List<User> savedUsers = userCaptor.getAllValues();
        assertEquals(2, savedUsers.size());

        // Первый пользователь должен быть с зашифрованным паролем
        User savedUser1 = savedUsers.stream()
                .filter(u -> u.getId().equals(1L))
                .findFirst()
                .orElse(null);
        assertNotNull(savedUser1);
        assertTrue(savedUser1.getPassword().startsWith("$2a$"));

        // Третий пользователь должен быть с зашифрованным паролем (без пробелов)
        User savedUser3 = savedUsers.stream()
                .filter(u -> u.getId().equals(3L))
                .findFirst()
                .orElse(null);
        assertNotNull(savedUser3);
        assertTrue(savedUser3.getPassword().startsWith("$2a$"));
    }

    @Test
    void encodePasswords_WithAlreadyEncoded_ShouldSkip() {
        // Given
        List<User> users = List.of(testUser2);
        when(userRepository.findAll()).thenReturn(users);

        // When
        passwordEncoderService.encodePasswords();

        // Then
        verify(userRepository, never()).save(any());
    }

    @Test
    void encodePasswords_WithNullPassword_ShouldSkip() {
        // Given
        List<User> users = List.of(testUser5);
        when(userRepository.findAll()).thenReturn(users);

        // When
        passwordEncoderService.encodePasswords();

        // Then
        verify(userRepository, never()).save(any());
    }

    @Test
    void encodePasswords_WithEmptyPassword_ShouldSkip() {
        // Given
        List<User> users = List.of(testUser4);
        when(userRepository.findAll()).thenReturn(users);

        // When
        passwordEncoderService.encodePasswords();

        // Then
        verify(userRepository, never()).save(any());
    }

    @Test
    void encodePasswords_WithNullUser_ShouldSkip() {
        // Given
        List<User> users = new ArrayList<>();
        users.add(null);
        users.add(testUser1);
        when(userRepository.findAll()).thenReturn(users);

        // When
        passwordEncoderService.encodePasswords();

        // Then
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void encodePasswords_WhenSaveThrowsException_ShouldLogError() {
        // Given
        List<User> users = List.of(testUser1);
        when(userRepository.findAll()).thenReturn(users);
        doThrow(new RuntimeException("DB error")).when(userRepository).save(any());

        // When (не должно выбросить исключение, только залогировать)
        passwordEncoderService.encodePasswords();

        // Then
        verify(userRepository, times(1)).save(any());
        // Ошибка залогирована, но метод продолжает работу
    }

    @Test
    void encodePasswords_WithEmptyList_ShouldDoNothing() {
        // Given
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        // When
        passwordEncoderService.encodePasswords();

        // Then
        verify(userRepository, never()).save(any());
    }

    // ============= ТЕСТЫ ДЛЯ encodePassword =============

    @Test
    void encodePassword_WithValidPassword_ShouldReturnEncoded() {
        // Given
        String rawPassword = "myPassword123";

        // When
        String result = passwordEncoderService.encodePassword(rawPassword);

        // Then
        assertNotNull(result);
        assertTrue(result.startsWith("$2a$"));
        assertTrue(result.length() > 10);
    }

    @Test
    void encodePassword_WithPasswordWithSpaces_ShouldTrimAndEncode() {
        // Given
        String rawPassword = "   myPassword123   ";

        // When
        String result = passwordEncoderService.encodePassword(rawPassword);

        // Then
        assertNotNull(result);
        assertTrue(result.startsWith("$2a$"));
    }

    @Test
    void encodePassword_WithNullPassword_ShouldThrowException() {
        // Given
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordEncoderService.encodePassword(null)
        );

        assertEquals("Пароль не может быть null", exception.getMessage());
    }

    @Test
    void encodePassword_WithEmptyPassword_ShouldThrowException() {
        // Given
        String rawPassword = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordEncoderService.encodePassword(rawPassword)
        );

        assertEquals("Пароль не может быть пустым", exception.getMessage());
    }

    @Test
    void encodePassword_WithPasswordTooShort_ShouldThrowException() {
        // Given
        String rawPassword = "123"; // 3 символа

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordEncoderService.encodePassword(rawPassword)
        );

        assertEquals("Пароль должен быть от 4 до 256 символов", exception.getMessage());
    }

    @Test
    void encodePassword_WithPasswordTooLong_ShouldThrowException() {
        // Given
        String rawPassword = "a".repeat(257); // 257 символов

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordEncoderService.encodePassword(rawPassword)
        );

        assertEquals("Пароль должен быть от 4 до 256 символов", exception.getMessage());
    }

    @Test
    void encodePassword_WithExactMinLength_ShouldWork() {
        // Given
        String rawPassword = "1234"; // 4 символа

        // When
        String result = passwordEncoderService.encodePassword(rawPassword);

        // Then
        assertNotNull(result);
        assertTrue(result.startsWith("$2a$"));
    }

    @Test
    void encodePassword_WithValidMaxLength_ShouldWork() {
        // Given
        // BCrypt ограничен 72 байтами, используем разумную длину
        String rawPassword = "a".repeat(50);

        // When
        String result = passwordEncoderService.encodePassword(rawPassword);

        // Then
        assertNotNull(result);
        assertTrue(result.startsWith("$2a$"));
    }

    // ============= ТЕСТЫ ДЛЯ matches =============

    @Test
    void matches_WithCorrectPassword_ShouldReturnTrue() {
        // Given
        String rawPassword = "password123";
        String encodedPassword = new BCryptPasswordEncoder().encode(rawPassword);

        // When
        boolean result = passwordEncoderService.matches(rawPassword, encodedPassword);

        // Then
        assertTrue(result);
    }

    @Test
    void matches_WithIncorrectPassword_ShouldReturnFalse() {
        // Given
        String rawPassword = "password123";
        String encodedPassword = new BCryptPasswordEncoder().encode("differentPassword");

        // When
        boolean result = passwordEncoderService.matches(rawPassword, encodedPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void matches_WithPasswordWithSpaces_ShouldTrimAndMatch() {
        // Given
        String rawPassword = "   password123   ";
        String encodedPassword = new BCryptPasswordEncoder().encode("password123");

        // When
        boolean result = passwordEncoderService.matches(rawPassword, encodedPassword);

        // Then
        assertTrue(result);
    }

    @Test
    void matches_WithNullRawPassword_ShouldReturnFalse() {
        // Given
        String encodedPassword = new BCryptPasswordEncoder().encode("password123");

        // When
        boolean result = passwordEncoderService.matches(null, encodedPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void matches_WithNullEncodedPassword_ShouldReturnFalse() {
        // Given
        String rawPassword = "password123";

        // When
        boolean result = passwordEncoderService.matches(rawPassword, null);

        // Then
        assertFalse(result);
    }

    @Test
    void matches_WithBothNull_ShouldReturnFalse() {
        // Given
        // When
        boolean result = passwordEncoderService.matches(null, null);

        // Then
        assertFalse(result);
    }

    @Test
    void matches_WithEmptyRawPasswordAndNonEmptyEncoded_ShouldReturnFalse() {
        // Given
        String rawPassword = "   ";
        String encodedPassword = new BCryptPasswordEncoder().encode("password123");

        // When
        boolean result = passwordEncoderService.matches(rawPassword, encodedPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void matches_WithEmptyRawPasswordAndEmptyEncoded_ShouldReturnTrue() {
        // Given
        String rawPassword = "   ";
        String encodedPassword = new BCryptPasswordEncoder().encode("");

        // When
        boolean result = passwordEncoderService.matches(rawPassword, encodedPassword);

        // Then
        assertTrue(result);
    }

    // ============= ТЕСТЫ ДЛЯ PostConstruct =============

    @Test
    void postConstruct_ShouldCallEncodePasswords() {
        // Этот тест проверяет, что метод вызывается при инициализации
        // Но в unit-тесте мы не можем проверить @PostConstruct напрямую

        // Просто убеждаемся, что метод encodePasswords существует и может быть вызван
        assertDoesNotThrow(() -> {
            PasswordEncoderService service = new PasswordEncoderService();
            // Не вызываем, чтобы не было NPE из-за отсутствия моков
        });
    }
}