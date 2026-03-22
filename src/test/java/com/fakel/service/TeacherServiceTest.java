package com.fakel.service;

import com.fakel.dto.UpdateTeacherProfileRequest;
import com.fakel.model.Teacher;
import com.fakel.model.User;
import com.fakel.repository.TeacherRepository;
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
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private TeacherService teacherService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<Teacher> teacherCaptor;

    private User testUser;
    private Teacher testTeacher;
    private UpdateTeacherProfileRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("teacher");
        testUser.setMail("old@mail.com");
        testUser.setPhoneNumber("+375291234567");

        testTeacher = new Teacher();
        testTeacher.setUserId(1L);
        testTeacher.setUser(testUser);
        testTeacher.setQualification("Доцент");
        testTeacher.setPost("Заведующий кафедрой");

        validRequest = new UpdateTeacherProfileRequest();
        validRequest.setMail("new@mail.com");
        validRequest.setPhoneNumber("+375337654321");
        validRequest.setQualification("Профессор");
        validRequest.setPost("Декан");

        lenient().when(userDetails.getUsername()).thenReturn("teacher");
    }

    @Test
    void updateProfile_WithAllFields_ShouldUpdateAll() {
        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(userRepository.existsByMail("new@mail.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+375337654321")).thenReturn(false);

        teacherService.updateProfile(userDetails, validRequest);

        verify(userRepository, times(1)).save(userCaptor.capture());
        verify(teacherRepository, times(1)).save(teacherCaptor.capture());

        User savedUser = userCaptor.getValue();
        Teacher savedTeacher = teacherCaptor.getValue();

        assertEquals("new@mail.com", savedUser.getMail());
        assertEquals("+375337654321", savedUser.getPhoneNumber());
        assertEquals("Профессор", savedTeacher.getQualification());
        assertEquals("Декан", savedTeacher.getPost());
    }

    @Test
    void updateProfile_WithOnlyEmail_ShouldUpdateEmail() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setMail("new@mail.com");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(userRepository.existsByMail("new@mail.com")).thenReturn(false);

        teacherService.updateProfile(userDetails, request);

        verify(userRepository, times(1)).save(userCaptor.capture());
        verify(teacherRepository, times(1)).save(any());

        User savedUser = userCaptor.getValue();
        assertEquals("new@mail.com", savedUser.getMail());
        assertEquals("+375291234567", savedUser.getPhoneNumber());
    }

    @Test
    void updateProfile_WithOnlyPhone_ShouldUpdatePhone() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setPhoneNumber("+375337654321");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(userRepository.existsByPhoneNumber("+375337654321")).thenReturn(false);

        teacherService.updateProfile(userDetails, request);

        verify(userRepository, times(1)).save(userCaptor.capture());
        verify(teacherRepository, times(1)).save(any());

        User savedUser = userCaptor.getValue();
        assertEquals("old@mail.com", savedUser.getMail());
        assertEquals("+375337654321", savedUser.getPhoneNumber());
    }

    @Test
    void updateProfile_WithOnlyQualification_ShouldUpdateQualification() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setQualification("Профессор");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        teacherService.updateProfile(userDetails, request);

        verify(userRepository, times(1)).save(any());
        verify(teacherRepository, times(1)).save(teacherCaptor.capture());

        Teacher savedTeacher = teacherCaptor.getValue();
        assertEquals("Профессор", savedTeacher.getQualification());
        assertEquals("Заведующий кафедрой", savedTeacher.getPost());
    }

    @Test
    void updateProfile_WithOnlyPost_ShouldUpdatePost() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setPost("Декан");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        teacherService.updateProfile(userDetails, request);

        verify(userRepository, times(1)).save(any());
        verify(teacherRepository, times(1)).save(teacherCaptor.capture());

        Teacher savedTeacher = teacherCaptor.getValue();
        assertEquals("Доцент", savedTeacher.getQualification());
        assertEquals("Декан", savedTeacher.getPost());
    }

    @Test
    void updateProfile_WithNullUserDetails_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.updateProfile(null, validRequest)
        );

        assertEquals("Данные пользователя не могут быть пустыми", exception.getMessage());
        verify(teacherRepository, never()).findByUserLogin(anyString());
    }

    @Test
    void updateProfile_WithNullRequest_ShouldThrowException() {
        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.updateProfile(userDetails, null)
        );

        assertEquals("Запрос не может быть пустым", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateProfile_WhenTeacherNotFound_ShouldThrowException() {
        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> teacherService.updateProfile(userDetails, validRequest)
        );

        assertEquals("Преподаватель не найден", exception.getMessage());
        verify(userRepository, never()).existsByMail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_WhenUserNotFound_ShouldThrowException() {
        testTeacher.setUser(null);
        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> teacherService.updateProfile(userDetails, validRequest)
        );

        assertEquals("У преподавателя отсутствуют данные пользователя", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithEmailTooLong_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setMail("a".repeat(257) + "@mail.com");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.updateProfile(userDetails, request)
        );

        assertEquals("Email не может быть длиннее 256 символов", exception.getMessage());
        verify(userRepository, never()).existsByMail(anyString());
    }

    @Test
    void updateProfile_WithInvalidEmailFormat_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setMail("invalid-email");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.updateProfile(userDetails, request)
        );

        assertEquals("Некорректный формат email", exception.getMessage());
        verify(userRepository, never()).existsByMail(anyString());
    }

    @Test
    void updateProfile_WithExistingEmail_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setMail("existing@mail.com");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(userRepository.existsByMail("existing@mail.com")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> teacherService.updateProfile(userDetails, request)
        );

        assertEquals("Email уже используется", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithInvalidPhoneFormat_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setPhoneNumber("123456789");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.updateProfile(userDetails, request)
        );

        assertEquals("Телефон должен быть в формате +375XXXXXXXXX", exception.getMessage());
        verify(userRepository, never()).existsByPhoneNumber(anyString());
    }

    @Test
    void updateProfile_WithExistingPhone_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setPhoneNumber("+375291111111");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(userRepository.existsByPhoneNumber("+375291111111")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> teacherService.updateProfile(userDetails, request)
        );

        assertEquals("Телефон уже используется", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithQualificationTooLong_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setQualification("a".repeat(65));

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.updateProfile(userDetails, request)
        );

        assertEquals("Квалификация не может быть длиннее 64 символов", exception.getMessage());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithQualificationInvalidCharacters_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setQualification("Профессор@#$");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.updateProfile(userDetails, request)
        );

        assertEquals("Квалификация содержит недопустимые символы", exception.getMessage());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithPostTooLong_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setPost("a".repeat(65));

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.updateProfile(userDetails, request)
        );

        assertEquals("Должность не может быть длиннее 64 символов", exception.getMessage());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithPostInvalidCharacters_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setPost("Декан@#$");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.updateProfile(userDetails, request)
        );

        assertEquals("Должность содержит недопустимые символы", exception.getMessage());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithSameEmail_ShouldNotCheckExistence() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setMail("old@mail.com");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        teacherService.updateProfile(userDetails, request);

        verify(userRepository, never()).existsByMail(anyString());
        verify(userRepository, never()).save(any());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateProfile_WithSamePhone_ShouldNotCheckExistence() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setPhoneNumber("+375291234567");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        teacherService.updateProfile(userDetails, request);

        verify(userRepository, never()).existsByPhoneNumber(anyString());
        verify(userRepository, never()).save(any());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void isValidEmail_WithValidEmail_ShouldReturnTrue() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setMail("valid@mail.com");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(userRepository.existsByMail("valid@mail.com")).thenReturn(false);

        assertDoesNotThrow(() -> teacherService.updateProfile(userDetails, request));
    }

    @Test
    void isValidEmail_WithInvalidEmail_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setMail("invalid-email");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        assertThrows(IllegalArgumentException.class,
                () -> teacherService.updateProfile(userDetails, request));
    }

    @Test
    void isValidPhone_WithValidPhone_ShouldReturnTrue() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setPhoneNumber("+375331234567");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(userRepository.existsByPhoneNumber("+375331234567")).thenReturn(false);

        assertDoesNotThrow(() -> teacherService.updateProfile(userDetails, request));
    }

    @Test
    void isValidPhone_WithInvalidPhone_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setPhoneNumber("123456789");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        assertThrows(IllegalArgumentException.class,
                () -> teacherService.updateProfile(userDetails, request));
    }

    @Test
    void isValidText_WithValidText_ShouldReturnTrue() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setQualification("Профессор, д.т.н.");
        request.setPost("Зав. кафедрой (ИУС)");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        assertDoesNotThrow(() -> teacherService.updateProfile(userDetails, request));
    }

    @Test
    void isValidText_WithInvalidText_ShouldThrowException() {
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest();
        request.setQualification("Профессор@#$");

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        assertThrows(IllegalArgumentException.class,
                () -> teacherService.updateProfile(userDetails, request));
    }
}