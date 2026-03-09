package com.fakel.service;

import com.fakel.dto.UpdateCadetProfileRequest;
import com.fakel.model.Cadet;
import com.fakel.model.User;
import com.fakel.repository.CadetRepository;
import com.fakel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CadetServiceTest {

    @Mock
    private CadetRepository cadetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private CadetService cadetService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<Cadet> cadetCaptor;

    private User testUser;
    private Cadet testCadet;
    private UpdateCadetProfileRequest validRequest;

    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя
        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("test_cadet");
        testUser.setMail("old@mail.com");
        testUser.setPhoneNumber("+375291234567");

        // Создаем тестового курсанта
        testCadet = new Cadet();
        testCadet.setUserId(1L);
        testCadet.setUser(testUser);
        testCadet.setWeight(new BigDecimal("75.5"));

        // Создаем валидный запрос
        validRequest = new UpdateCadetProfileRequest();
        validRequest.setMail("new@mail.com");
        validRequest.setPhoneNumber("+375337654321");
        validRequest.setWeight(new BigDecimal("76.5"));

        // Настройка UserDetails
        lenient().when(userDetails.getUsername()).thenReturn("test_cadet");
    }

    // ============= ТЕСТЫ ДЛЯ updateProfile =============

    @Test
    void updateProfile_WithAllFields_ShouldUpdateAll() {
        // Given
        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));
        when(userRepository.existsByMail("new@mail.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+375337654321")).thenReturn(false);

        // When
        cadetService.updateProfile(userDetails, validRequest);

        // Then
        verify(userRepository, times(1)).save(userCaptor.capture());
        verify(cadetRepository, times(1)).save(cadetCaptor.capture());

        User savedUser = userCaptor.getValue();
        Cadet savedCadet = cadetCaptor.getValue();

        assertEquals("new@mail.com", savedUser.getMail());
        assertEquals("+375337654321", savedUser.getPhoneNumber());
        assertEquals(new BigDecimal("76.5"), savedCadet.getWeight());
    }

    @Test
    void updateProfile_WithOnlyEmail_ShouldUpdateEmail() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setMail("new@mail.com");

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));
        when(userRepository.existsByMail("new@mail.com")).thenReturn(false);

        // When
        cadetService.updateProfile(userDetails, request);

        // Then
        verify(userRepository, times(1)).save(userCaptor.capture());
        verify(cadetRepository, times(1)).save(any());

        User savedUser = userCaptor.getValue();
        assertEquals("new@mail.com", savedUser.getMail());
        assertEquals("+375291234567", savedUser.getPhoneNumber()); // не изменился
    }

    @Test
    void updateProfile_WithOnlyPhone_ShouldUpdatePhone() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setPhoneNumber("+375337654321");

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));
        when(userRepository.existsByPhoneNumber("+375337654321")).thenReturn(false);

        // When
        cadetService.updateProfile(userDetails, request);

        // Then
        verify(userRepository, times(1)).save(userCaptor.capture());
        verify(cadetRepository, times(1)).save(any());

        User savedUser = userCaptor.getValue();
        assertEquals("old@mail.com", savedUser.getMail()); // не изменился
        assertEquals("+375337654321", savedUser.getPhoneNumber());
    }

    @Test
    void updateProfile_WithOnlyWeight_ShouldUpdateWeight() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setWeight(new BigDecimal("76.5"));

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));

        // When
        cadetService.updateProfile(userDetails, request);

        // Then
        verify(userRepository, times(1)).save(any());
        verify(cadetRepository, times(1)).save(cadetCaptor.capture());

        Cadet savedCadet = cadetCaptor.getValue();
        assertEquals(new BigDecimal("76.5"), savedCadet.getWeight());
    }

    @Test
    void updateProfile_WithNoChanges_ShouldNotSave() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setMail("old@mail.com");
        request.setPhoneNumber("+375291234567");
        request.setWeight(new BigDecimal("75.5"));

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));

        // When
        cadetService.updateProfile(userDetails, request);

        // Then
        verify(userRepository, never()).save(any());
        verify(cadetRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithNullUserDetails_ShouldThrowException() {
        // Given
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cadetService.updateProfile(null, validRequest)
        );

        assertEquals("Данные пользователя не могут быть пустыми", exception.getMessage());
        verify(cadetRepository, never()).findByUserLogin(anyString());
    }

    @Test
    void updateProfile_WithNullRequest_ShouldThrowException() {
        // Given - НЕ нужно мокать cadetRepository.findByUserLogin()!

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cadetService.updateProfile(userDetails, null)
        );

        assertEquals("Запрос не может быть пустым", exception.getMessage());
        verify(cadetRepository, never()).findByUserLogin(anyString());
        verify(userRepository, never()).save(any());
        verify(cadetRepository, never()).save(any());
    }

    @Test
    void updateProfile_WhenCadetNotFound_ShouldThrowException() {
        // Given
        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cadetService.updateProfile(userDetails, validRequest)
        );

        assertEquals("Курсант не найден", exception.getMessage());
        verify(userRepository, never()).existsByMail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithEmailTooLong_ShouldThrowException() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setMail("a".repeat(257) + "@mail.com");

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cadetService.updateProfile(userDetails, request)
        );

        assertEquals("Email не может быть длиннее 256 символов", exception.getMessage());
        verify(userRepository, never()).existsByMail(anyString());
    }

    @Test
    void updateProfile_WithExistingEmail_ShouldThrowException() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setMail("existing@mail.com");

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));
        when(userRepository.existsByMail("existing@mail.com")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cadetService.updateProfile(userDetails, request)
        );

        assertEquals("Email уже используется", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithExistingPhone_ShouldThrowException() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setPhoneNumber("+375291111111");

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));
        when(userRepository.existsByPhoneNumber("+375291111111")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cadetService.updateProfile(userDetails, request)
        );

        assertEquals("Телефон уже используется", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithWeightBelowMin_ShouldThrowException() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setWeight(new BigDecimal("29.9"));

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cadetService.updateProfile(userDetails, request)
        );

        assertEquals("Вес должен быть от 30 до 180 кг", exception.getMessage());
        verify(cadetRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithWeightAboveMax_ShouldThrowException() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setWeight(new BigDecimal("180.1"));

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cadetService.updateProfile(userDetails, request)
        );

        assertEquals("Вес должен быть от 30 до 180 кг", exception.getMessage());
        verify(cadetRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithWeightTooPrecise_ShouldThrowException() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setWeight(new BigDecimal("75.1234"));

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cadetService.updateProfile(userDetails, request)
        );

        assertEquals("Вес может содержать не более 3 знаков после запятой", exception.getMessage());
        verify(cadetRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithSameEmail_ShouldNotCheckExistence() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setMail("old@mail.com");

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));

        // When
        cadetService.updateProfile(userDetails, request);

        // Then
        verify(userRepository, never()).existsByMail(anyString());
        verify(userRepository, never()).save(any());
        verify(cadetRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithSamePhone_ShouldNotCheckExistence() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setPhoneNumber("+375291234567");

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));

        // When
        cadetService.updateProfile(userDetails, request);

        // Then
        verify(userRepository, never()).existsByPhoneNumber(anyString());
        verify(userRepository, never()).save(any());
        verify(cadetRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithSameWeight_ShouldNotSave() {
        // Given
        UpdateCadetProfileRequest request = new UpdateCadetProfileRequest();
        request.setWeight(new BigDecimal("75.5"));

        when(cadetRepository.findByUserLogin("test_cadet")).thenReturn(Optional.of(testCadet));

        // When
        cadetService.updateProfile(userDetails, request);

        // Then
        verify(userRepository, never()).save(any());
        verify(cadetRepository, never()).save(any());
    }

    // ============= ТЕСТЫ ДЛЯ getCadetByLogin =============

    @Test
    void getCadetByLogin_WithValidLogin_ShouldReturnCadet() {
        // Given
        String login = "test_cadet";
        when(cadetRepository.findByUserLogin(login)).thenReturn(Optional.of(testCadet));

        // When
        Cadet result = cadetService.getCadetByLogin(login);

        // Then
        assertNotNull(result);
        assertEquals(testCadet.getUserId(), result.getUserId());
        assertEquals(testCadet.getWeight(), result.getWeight());
        verify(cadetRepository, times(1)).findByUserLogin(login);
    }

    @Test
    void getCadetByLogin_WithNullLogin_ShouldThrowException() {
        // Given
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cadetService.getCadetByLogin(null)
        );

        assertEquals("Логин не может быть пустым", exception.getMessage());
        verify(cadetRepository, never()).findByUserLogin(anyString());
    }

    @Test
    void getCadetByLogin_WithEmptyLogin_ShouldThrowException() {
        // Given
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cadetService.getCadetByLogin("   ")
        );

        assertEquals("Логин не может быть пустым", exception.getMessage());
        verify(cadetRepository, never()).findByUserLogin(anyString());
    }

    @Test
    void getCadetByLogin_WhenCadetNotFound_ShouldThrowException() {
        // Given
        String login = "nonexistent";
        when(cadetRepository.findByUserLogin(login)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cadetService.getCadetByLogin(login)
        );

        assertEquals("Курсант не найден", exception.getMessage());
        verify(cadetRepository, times(1)).findByUserLogin(login);
    }

    // ============= ТЕСТЫ ДЛЯ isValidWeight =============

    @Test
    void isValidWeight_WithValidWeight_ShouldReturnTrue() {
        // Given
        BigDecimal weight = new BigDecimal("75.5");

        // When
        boolean result = cadetService.isValidWeight(weight);

        // Then
        assertTrue(result);
    }

    @Test
    void isValidWeight_WithMinWeight_ShouldReturnTrue() {
        // Given
        BigDecimal weight = new BigDecimal("30.0");

        // When
        boolean result = cadetService.isValidWeight(weight);

        // Then
        assertTrue(result);
    }

    @Test
    void isValidWeight_WithMaxWeight_ShouldReturnTrue() {
        // Given
        BigDecimal weight = new BigDecimal("180.0");

        // When
        boolean result = cadetService.isValidWeight(weight);

        // Then
        assertTrue(result);
    }

    @Test
    void isValidWeight_WithWeightBelowMin_ShouldReturnFalse() {
        // Given
        BigDecimal weight = new BigDecimal("29.9");

        // When
        boolean result = cadetService.isValidWeight(weight);

        // Then
        assertFalse(result);
    }

    @Test
    void isValidWeight_WithWeightAboveMax_ShouldReturnFalse() {
        // Given
        BigDecimal weight = new BigDecimal("180.1");

        // When
        boolean result = cadetService.isValidWeight(weight);

        // Then
        assertFalse(result);
    }

    @Test
    void isValidWeight_WithTooPreciseWeight_ShouldReturnFalse() {
        // Given
        BigDecimal weight = new BigDecimal("75.1234");

        // When
        boolean result = cadetService.isValidWeight(weight);

        // Then
        assertFalse(result);
    }

    @Test
    void isValidWeight_WithNullWeight_ShouldReturnFalse() {
        // Given
        // When
        boolean result = cadetService.isValidWeight(null);

        // Then
        assertFalse(result);
    }
}