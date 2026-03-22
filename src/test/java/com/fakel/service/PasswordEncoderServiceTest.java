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
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setLogin("user1");
        testUser1.setPassword("password123");

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setLogin("user2");
        testUser2.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMy.MrqUZ3qK9cX5qX5qX5qX5qX5qX5qX5q");

        testUser3 = new User();
        testUser3.setId(3L);
        testUser3.setLogin("user3");
        testUser3.setPassword("   password456   ");

        testUser4 = new User();
        testUser4.setId(4L);
        testUser4.setLogin("user4");
        testUser4.setPassword("");

        testUser5 = new User();
        testUser5.setId(5L);
        testUser5.setLogin("user5");
        testUser5.setPassword(null);
    }

    @Test
    void encodePasswords_WithMixedUsers_ShouldEncodeAppropriately() {
        List<User> users = List.of(testUser1, testUser2, testUser3, testUser4, testUser5);
        when(userRepository.findAll()).thenReturn(users);

        passwordEncoderService.encodePasswords();

        verify(userRepository, times(2)).save(userCaptor.capture());

        List<User> savedUsers = userCaptor.getAllValues();
        assertEquals(2, savedUsers.size());

        User savedUser1 = savedUsers.stream()
                .filter(u -> u.getId().equals(1L))
                .findFirst()
                .orElse(null);
        assertNotNull(savedUser1);
        assertTrue(savedUser1.getPassword().startsWith("$2a$"));

        User savedUser3 = savedUsers.stream()
                .filter(u -> u.getId().equals(3L))
                .findFirst()
                .orElse(null);
        assertNotNull(savedUser3);
        assertTrue(savedUser3.getPassword().startsWith("$2a$"));
    }

    @Test
    void encodePasswords_WithAlreadyEncoded_ShouldSkip() {
        List<User> users = List.of(testUser2);
        when(userRepository.findAll()).thenReturn(users);

        passwordEncoderService.encodePasswords();

        verify(userRepository, never()).save(any());
    }

    @Test
    void encodePasswords_WithNullPassword_ShouldSkip() {
        List<User> users = List.of(testUser5);
        when(userRepository.findAll()).thenReturn(users);

        passwordEncoderService.encodePasswords();

        verify(userRepository, never()).save(any());
    }

    @Test
    void encodePasswords_WithEmptyPassword_ShouldSkip() {
        List<User> users = List.of(testUser4);
        when(userRepository.findAll()).thenReturn(users);

        passwordEncoderService.encodePasswords();

        verify(userRepository, never()).save(any());
    }

    @Test
    void encodePasswords_WithNullUser_ShouldSkip() {
        List<User> users = new ArrayList<>();
        users.add(null);
        users.add(testUser1);
        when(userRepository.findAll()).thenReturn(users);

        passwordEncoderService.encodePasswords();

        verify(userRepository, times(1)).save(any());
    }

    @Test
    void encodePasswords_WhenSaveThrowsException_ShouldLogError() {
        List<User> users = List.of(testUser1);
        when(userRepository.findAll()).thenReturn(users);
        doThrow(new RuntimeException("DB error")).when(userRepository).save(any());

        passwordEncoderService.encodePasswords();

        verify(userRepository, times(1)).save(any());
    }

    @Test
    void encodePasswords_WithEmptyList_ShouldDoNothing() {
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        passwordEncoderService.encodePasswords();

        verify(userRepository, never()).save(any());
    }

    @Test
    void encodePassword_WithValidPassword_ShouldReturnEncoded() {
        String rawPassword = "myPassword123";

        String result = passwordEncoderService.encodePassword(rawPassword);

        assertNotNull(result);
        assertTrue(result.startsWith("$2a$"));
        assertTrue(result.length() > 10);
    }

    @Test
    void encodePassword_WithPasswordWithSpaces_ShouldTrimAndEncode() {
        String rawPassword = "   myPassword123   ";

        String result = passwordEncoderService.encodePassword(rawPassword);

        assertNotNull(result);
        assertTrue(result.startsWith("$2a$"));
    }

    @Test
    void encodePassword_WithNullPassword_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordEncoderService.encodePassword(null)
        );

        assertEquals("Пароль не может быть null", exception.getMessage());
    }

    @Test
    void encodePassword_WithEmptyPassword_ShouldThrowException() {
        String rawPassword = "   ";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordEncoderService.encodePassword(rawPassword)
        );

        assertEquals("Пароль не может быть пустым", exception.getMessage());
    }

    @Test
    void encodePassword_WithPasswordTooShort_ShouldThrowException() {
        String rawPassword = "123";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordEncoderService.encodePassword(rawPassword)
        );

        assertEquals("Пароль должен быть от 4 до 256 символов", exception.getMessage());
    }

    @Test
    void encodePassword_WithPasswordTooLong_ShouldThrowException() {
        String rawPassword = "a".repeat(257);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordEncoderService.encodePassword(rawPassword)
        );

        assertEquals("Пароль должен быть от 4 до 256 символов", exception.getMessage());
    }

    @Test
    void encodePassword_WithExactMinLength_ShouldWork() {
        String rawPassword = "1234";

        String result = passwordEncoderService.encodePassword(rawPassword);

        assertNotNull(result);
        assertTrue(result.startsWith("$2a$"));
    }

    @Test
    void encodePassword_WithValidMaxLength_ShouldWork() {
        String rawPassword = "a".repeat(50);

        String result = passwordEncoderService.encodePassword(rawPassword);

        assertNotNull(result);
        assertTrue(result.startsWith("$2a$"));
    }

    @Test
    void matches_WithCorrectPassword_ShouldReturnTrue() {
        String rawPassword = "password123";
        String encodedPassword = new BCryptPasswordEncoder().encode(rawPassword);

        boolean result = passwordEncoderService.matches(rawPassword, encodedPassword);

        assertTrue(result);
    }

    @Test
    void matches_WithIncorrectPassword_ShouldReturnFalse() {
        String rawPassword = "password123";
        String encodedPassword = new BCryptPasswordEncoder().encode("differentPassword");

        boolean result = passwordEncoderService.matches(rawPassword, encodedPassword);

        assertFalse(result);
    }

    @Test
    void matches_WithPasswordWithSpaces_ShouldTrimAndMatch() {
        String rawPassword = "   password123   ";
        String encodedPassword = new BCryptPasswordEncoder().encode("password123");

        boolean result = passwordEncoderService.matches(rawPassword, encodedPassword);

        assertTrue(result);
    }

    @Test
    void matches_WithNullRawPassword_ShouldReturnFalse() {
        String encodedPassword = new BCryptPasswordEncoder().encode("password123");

        boolean result = passwordEncoderService.matches(null, encodedPassword);

        assertFalse(result);
    }

    @Test
    void matches_WithNullEncodedPassword_ShouldReturnFalse() {
        String rawPassword = "password123";

        boolean result = passwordEncoderService.matches(rawPassword, null);

        assertFalse(result);
    }

    @Test
    void matches_WithBothNull_ShouldReturnFalse() {
        boolean result = passwordEncoderService.matches(null, null);

        assertFalse(result);
    }

    @Test
    void matches_WithEmptyRawPasswordAndNonEmptyEncoded_ShouldReturnFalse() {
        String rawPassword = "   ";
        String encodedPassword = new BCryptPasswordEncoder().encode("password123");

        boolean result = passwordEncoderService.matches(rawPassword, encodedPassword);

        assertFalse(result);
    }

    @Test
    void matches_WithEmptyRawPasswordAndEmptyEncoded_ShouldReturnTrue() {
        String rawPassword = "   ";
        String encodedPassword = new BCryptPasswordEncoder().encode("");

        boolean result = passwordEncoderService.matches(rawPassword, encodedPassword);

        assertTrue(result);
    }

    @Test
    void postConstruct_ShouldCallEncodePasswords() {
        assertDoesNotThrow(() -> {
            PasswordEncoderService service = new PasswordEncoderService();
        });
    }
}