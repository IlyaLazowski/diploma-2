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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrainingServiceTest {

    // Внутренний класс для GrantedAuthority
    private static class SimpleGrantedAuthority implements GrantedAuthority {
        private final String authority;

        public SimpleGrantedAuthority(String authority) {
            this.authority = authority;
        }

        @Override
        public String getAuthority() {
            return authority;
        }
    }

    @Mock
    private TrainingRepository trainingRepository;

    @Mock
    private CadetRepository cadetRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private ExerciseCatalogRepository exerciseCatalogRepository;

    @Mock
    private ExerciseParameterRepository exerciseParameterRepository;

    @Mock
    private ExercisesInTrainingRepository exercisesInTrainingRepository;

    @Mock
    private ApproachRepository approachRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private TrainingService trainingService;

    @Captor
    private ArgumentCaptor<Training> trainingCaptor;

    @Captor
    private ArgumentCaptor<ExercisesInTraining> exerciseCaptor;

    @Captor
    private ArgumentCaptor<Approach> approachCaptor;

    private Cadet testCadet;
    private Teacher testTeacher;
    private Training testTraining;
    private ExercisesInTraining testExercise;
    private Approach testApproach;
    private ExerciseCatalog testExerciseCatalog;
    private ExerciseParameter testParameter;
    private MeasurementUnit testUnit;
    private Group testGroup;
    private University testUniversity;
    private User testUser;
    private LocalDate now;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();

        // Создаем университет
        testUniversity = new University();
        testUniversity.setId(1L);
        testUniversity.setCode("ВУЗ");

        // Создаем группу
        testGroup = new Group();
        testGroup.setId(1L);
        testGroup.setNumber("21-ВП");
        testGroup.setUniversity(testUniversity);

        // Создаем пользователя
        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("cadet");
        testUser.setUniversity(testUniversity);

        // Создаем курсанта
        testCadet = new Cadet();
        testCadet.setUserId(1L);
        testCadet.setUser(testUser);
        testCadet.setGroupId(1L);

        // Создаем преподавателя
        User teacherUser = new User();
        teacherUser.setId(2L);
        teacherUser.setLogin("teacher");
        teacherUser.setUniversity(testUniversity);

        testTeacher = new Teacher();
        testTeacher.setUserId(2L);
        testTeacher.setUser(teacherUser);

        // Создаем единицу измерения
        testUnit = new MeasurementUnit();
        testUnit.setId(1L);
        testUnit.setCode("кг");

        // Создаем параметр упражнения
        testParameter = new ExerciseParameter();
        testParameter.setId(1L);
        testParameter.setCode("вес");
        testParameter.setMeasurementUnit(testUnit);
        testParameter.setDefaultIntValue(new BigDecimal("100"));

        // Создаем каталог упражнений
        testExerciseCatalog = new ExerciseCatalog();
        testExerciseCatalog.setId(1L);
        testExerciseCatalog.setCode("BENCH_PRESS");
        testExerciseCatalog.setDescription("Жим лежа");
        testExerciseCatalog.setType("Сила");

        List<ExerciseParameter> params = new ArrayList<>();
        params.add(testParameter);
        testExerciseCatalog.setParameters(params);

        // Создаем подход
        testApproach = new Approach();
        testApproach.setId(1L);
        testApproach.setApproachNumber((short) 1);
        testApproach.setExerciseParameter(testParameter);
        testApproach.setValue(new BigDecimal("100"));

        // Создаем упражнение в тренировке
        testExercise = new ExercisesInTraining();
        testExercise.setId(1L);
        testExercise.setExerciseCatalog(testExerciseCatalog);
        testExercise.setRestPeriod((short) 60);

        List<Approach> approaches = new ArrayList<>();
        approaches.add(testApproach);
        testExercise.setApproaches(approaches);

        // Создаем тренировку
        testTraining = new Training();
        testTraining.setId(1L);
        testTraining.setCadetId(1L);
        testTraining.setDate(now.minusDays(1));
        testTraining.setCurrentWeight(new BigDecimal("75.5"));
        testTraining.setRestPeriod((short) 120);
        testTraining.setType("Сила");

        List<ExercisesInTraining> exercises = new ArrayList<>();
        exercises.add(testExercise);
        testTraining.setExercises(exercises);

        // Настройка связей
        testApproach.setExerciseInTraining(testExercise);
        testExercise.setTraining(testTraining);

        // Настройка UserDetails для курсанта
        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        lenient().when(userDetails.getAuthorities()).thenAnswer(invocation ->
                List.of(new SimpleGrantedAuthority("ROLE_CADET"))
        );
    }

    // ============= ТЕСТЫ ДЛЯ getTrainings =============

    @Test
    void getTrainings_WithNoFilters_ShouldReturnAllTrainings() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findByCadetId(eq(1L), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<TrainingDto> result = trainingService.getTrainings(
                userDetails, null, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(trainingRepository, times(1)).findByCadetId(1L, pageable);
    }

    @Test
    void getTrainings_WithDate_ShouldReturnFilteredByDate() {
        // Given
        LocalDate date = now.minusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findByCadetIdAndDate(eq(1L), eq(date), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<TrainingDto> result = trainingService.getTrainings(
                userDetails, date, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(trainingRepository, times(1)).findByCadetIdAndDate(1L, date, pageable);
    }

    @Test
    void getTrainings_WithDateRange_ShouldReturnFilteredByDateRange() {
        // Given
        LocalDate from = now.minusDays(10);
        LocalDate to = now;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findByCadetIdAndDateBetween(eq(1L), eq(from), eq(to), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<TrainingDto> result = trainingService.getTrainings(
                userDetails, null, from, to, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(trainingRepository, times(1)).findByCadetIdAndDateBetween(1L, from, to, pageable);
    }

    @Test
    void getTrainings_WithType_ShouldReturnFilteredByType() {
        // Given
        String type = "Сила";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findByCadetIdAndType(eq(1L), eq(type), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<TrainingDto> result = trainingService.getTrainings(
                userDetails, null, null, null, type, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(trainingRepository, times(1)).findByCadetIdAndType(1L, type, pageable);
    }

    @Test
    void getTrainings_WithTypeAndDateRange_ShouldReturnFiltered() {
        // Given
        String type = "Сила";
        LocalDate from = now.minusDays(10);
        LocalDate to = now;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findByCadetIdAndTypeAndDateBetween(eq(1L), eq(type), eq(from), eq(to), eq(pageable)))
                .thenReturn(expectedPage);

        // When
        Page<TrainingDto> result = trainingService.getTrainings(
                userDetails, null, from, to, type, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(trainingRepository, times(1)).findByCadetIdAndTypeAndDateBetween(1L, type, from, to, pageable);
    }

    @Test
    void getTrainings_WithInvalidDateRange_ShouldThrowException() {
        // Given
        LocalDate from = now;
        LocalDate to = now.minusDays(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getTrainings(userDetails, null, from, to, null, pageable));
    }

    @Test
    void getTrainings_WithInvalidType_ShouldThrowException() {
        // Given
        String type = "Неверный тип";
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getTrainings(userDetails, null, null, null, type, pageable));
    }

    @Test
    void getTrainings_WithNullUserDetails_ShouldThrowException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getTrainings(null, null, null, null, null, pageable));
    }

    @Test
    void getTrainings_WithNullPageable_ShouldThrowException() {
        // Given
        assertThrows(NullPointerException.class,
                () -> trainingService.getTrainings(userDetails, null, null, null, null, null));
    }

    @Test
    void getTrainings_WhenCadetNotFound_ShouldThrowException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> trainingService.getTrainings(userDetails, null, null, null, null, pageable));
    }

    // ============= ТЕСТЫ ДЛЯ getTrainingById =============

    @Test
    void getTrainingById_WithValidId_ShouldReturnTraining() {
        // Given
        Long trainingId = 1L;

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        // When
        TrainingDto result = trainingService.getTrainingById(userDetails, trainingId);

        // Then
        assertNotNull(result);
        assertEquals(trainingId, result.getId());
        assertEquals(now.minusDays(1), result.getDate());
        assertEquals("Сила", result.getType());
    }

    @Test
    void getTrainingById_WithInvalidId_ShouldThrowException() {
        // Given
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getTrainingById(userDetails, null));
    }

    @Test
    void getTrainingById_WhenTrainingNotFound_ShouldThrowException() {
        // Given
        Long trainingId = 999L;

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> trainingService.getTrainingById(userDetails, trainingId));
    }

    @Test
    void getTrainingById_WhenNoAccess_ShouldThrowException() {
        // Given
        Long trainingId = 1L;
        testTraining.setCadetId(2L); // другая тренировка

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> trainingService.getTrainingById(userDetails, trainingId));
    }

    // ============= ТЕСТЫ ДЛЯ updateTraining =============

    @Test
    void updateTraining_WithValidData_ShouldUpdateTraining() {
        // Given
        Long trainingId = 1L;
        UpdateTrainingRequest request = new UpdateTrainingRequest();
        request.setDate(now.minusDays(2));
        request.setCurrentWeight(new BigDecimal("76.0"));
        request.setRestPeriod(180);

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        // When
        trainingService.updateTraining(userDetails, trainingId, request);

        // Then
        verify(trainingRepository, times(1)).save(trainingCaptor.capture());
        Training savedTraining = trainingCaptor.getValue();
        assertEquals(now.minusDays(2), savedTraining.getDate());
        assertEquals(new BigDecimal("76.0"), savedTraining.getCurrentWeight());
        assertEquals((short) 180, savedTraining.getRestPeriod());
    }

    @Test
    void updateTraining_WithFutureDate_ShouldThrowException() {
        // Given
        Long trainingId = 1L;
        UpdateTrainingRequest request = new UpdateTrainingRequest();
        request.setDate(now.plusDays(1));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.updateTraining(userDetails, trainingId, request));
    }

    @Test
    void updateTraining_WithInvalidWeight_ShouldThrowException() {
        // Given
        Long trainingId = 1L;
        UpdateTrainingRequest request = new UpdateTrainingRequest();
        request.setCurrentWeight(new BigDecimal("200")); // > 180

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.updateTraining(userDetails, trainingId, request));
    }

    @Test
    void updateTraining_WithInvalidRestPeriod_ShouldThrowException() {
        // Given
        Long trainingId = 1L;
        UpdateTrainingRequest request = new UpdateTrainingRequest();
        request.setRestPeriod(3601); // > 3600

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.updateTraining(userDetails, trainingId, request));
    }

    @Test
    void updateTraining_WithExerciseUpdate_ShouldUpdateExercise() {
        // Given
        Long trainingId = 1L;

        UpdateTrainingRequest request = new UpdateTrainingRequest();
        UpdateTrainingRequest.UpdateExerciseRequest exerciseReq =
                new UpdateTrainingRequest.UpdateExerciseRequest();
        exerciseReq.setExerciseInTrainingId(1L);
        exerciseReq.setRestPeriod(90);

        UpdateTrainingRequest.UpdateApproachRequest approachReq =
                new UpdateTrainingRequest.UpdateApproachRequest();
        approachReq.setApproachId(1L);
        approachReq.setValue(new BigDecimal("110"));

        List<UpdateTrainingRequest.UpdateApproachRequest> approachList = new ArrayList<>();
        approachList.add(approachReq);
        exerciseReq.setApproaches(approachList);

        List<UpdateTrainingRequest.UpdateExerciseRequest> exerciseList = new ArrayList<>();
        exerciseList.add(exerciseReq);
        request.setExercises(exerciseList);

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));
        when(exercisesInTrainingRepository.findById(1L)).thenReturn(Optional.of(testExercise));
        when(approachRepository.findById(1L)).thenReturn(Optional.of(testApproach));

        // When
        trainingService.updateTraining(userDetails, trainingId, request);

        // Then
        verify(approachRepository, times(1)).save(any(Approach.class));
        verify(exercisesInTrainingRepository, times(1)).save(any(ExercisesInTraining.class));
        verify(trainingRepository, times(1)).save(any(Training.class));
    }

    // ============= ТЕСТЫ ДЛЯ deleteTraining =============

    @Test
    void deleteTraining_WithValidId_ShouldDelete() {
        // Given
        Long trainingId = 1L;

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.existsByIdAndCadetId(trainingId, 1L)).thenReturn(true);
        doNothing().when(trainingRepository).deleteById(trainingId);

        // When
        trainingService.deleteTraining(userDetails, trainingId);

        // Then
        verify(trainingRepository, times(1)).deleteById(trainingId);
    }

    @Test
    void deleteTraining_WithInvalidUserRole_ShouldThrowException() {
        // Given
        Long trainingId = 1L;
        lenient().when(userDetails.getAuthorities()).thenAnswer(invocation ->
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );

        // When & Then
        assertThrows(RuntimeException.class,
                () -> trainingService.deleteTraining(userDetails, trainingId));
    }

    @Test
    void deleteTraining_WhenTrainingNotBelongToCadet_ShouldThrowException() {
        // Given
        Long trainingId = 1L;

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.existsByIdAndCadetId(trainingId, 1L)).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class,
                () -> trainingService.deleteTraining(userDetails, trainingId));
    }

    // ============= ТЕСТЫ ДЛЯ getExercisesByType =============

    @Test
    void getExercisesByType_WithValidType_ShouldReturnExercises() {
        // Given
        String type = "Сила";
        List<ExerciseCatalog> exercises = new ArrayList<>();
        exercises.add(testExerciseCatalog);

        when(exerciseCatalogRepository.findByType(type)).thenReturn(exercises);

        // When
        List<ExerciseCatalogDto> result = trainingService.getExercisesByType(type);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("BENCH_PRESS", result.get(0).getCode());
    }

    @Test
    void getExercisesByType_WithNullType_ShouldThrowException() {
        // Given
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getExercisesByType(null));
    }

    @Test
    void getExercisesByType_WithInvalidType_ShouldThrowException() {
        // Given
        String type = "Неверный тип";

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getExercisesByType(type));
    }

    @Test
    void getExercisesByType_WhenNoExercises_ShouldReturnEmptyList() {
        // Given
        String type = "Сила";
        when(exerciseCatalogRepository.findByType(type)).thenReturn(new ArrayList<>());

        // When
        List<ExerciseCatalogDto> result = trainingService.getExercisesByType(type);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ============= ТЕСТЫ ДЛЯ getExerciseWithDefaults =============

    @Test
    void getExerciseWithDefaults_WithValidCode_ShouldReturnExercise() {
        // Given
        String code = "BENCH_PRESS";

        when(exerciseCatalogRepository.findByCodeWithParameters(code))
                .thenReturn(Optional.of(testExerciseCatalog));

        // When
        ExerciseCatalogDto result = trainingService.getExerciseWithDefaults(code);

        // Then
        assertNotNull(result);
        assertEquals("BENCH_PRESS", result.getCode());
        assertEquals("Жим лежа", result.getDescription());
        assertEquals("Сила", result.getType());
        assertEquals(1, result.getDefaultParameters().size());
    }

    @Test
    void getExerciseWithDefaults_WithEmptyCode_ShouldThrowException() {
        // Given
        String code = "   ";

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getExerciseWithDefaults(code));
    }

    @Test
    void getExerciseWithDefaults_WhenExerciseNotFound_ShouldThrowException() {
        // Given
        String code = "NON_EXISTENT";

        when(exerciseCatalogRepository.findByCodeWithParameters(code))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> trainingService.getExerciseWithDefaults(code));
    }

    // ============= ТЕСТЫ ДЛЯ getTrainingsByCadetId (для преподавателя) =============

    @Test
    void getTrainingsByCadetId_WithValidData_ShouldReturnTrainings() {
        // Given
        Long cadetId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        // Переключаем на преподавателя
        lenient().when(userDetails.getUsername()).thenReturn("teacher");
        lenient().when(userDetails.getAuthorities()).thenAnswer(invocation ->
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(trainingRepository.findByCadetId(eq(cadetId), eq(pageable))).thenReturn(expectedPage);

        // When
        Page<TrainingDto> result = trainingService.getTrainingsByCadetId(
                userDetails, cadetId, null, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getTrainingsByCadetId_WhenNoAccess_ShouldThrowException() {
        // Given
        Long cadetId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        University differentUniversity = new University();
        differentUniversity.setId(2L);
        testGroup.setUniversity(differentUniversity);

        // Переключаем на преподавателя
        lenient().when(userDetails.getUsername()).thenReturn("teacher");
        lenient().when(userDetails.getAuthorities()).thenAnswer(invocation ->
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> trainingService.getTrainingsByCadetId(
                        userDetails, cadetId, null, null, null, null, pageable));
    }

    // ============= ТЕСТЫ ДЛЯ isValidTrainingType =============

    @Test
    void isValidTrainingType_WithValidTypes_ShouldReturnTrue() {
        assertDoesNotThrow(() -> trainingService.getExercisesByType("Сила"));
        assertDoesNotThrow(() -> trainingService.getExercisesByType("Скорость"));
        assertDoesNotThrow(() -> trainingService.getExercisesByType("Выносливость"));
    }

    // ============= ТЕСТЫ ДЛЯ convertToDto =============

    @Test
    void convertToDto_WithNullTraining_ShouldReturnNull() {
        // Этот метод приватный, тестируем через публичные методы
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> trainingService.getTrainingById(userDetails, 999L));
    }
}