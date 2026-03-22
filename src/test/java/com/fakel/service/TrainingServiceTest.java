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

        testUniversity = new University();
        testUniversity.setId(1L);
        testUniversity.setCode("ВУЗ");

        testGroup = new Group();
        testGroup.setId(1L);
        testGroup.setNumber("21-ВП");
        testGroup.setUniversity(testUniversity);

        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("cadet");
        testUser.setUniversity(testUniversity);

        testCadet = new Cadet();
        testCadet.setUserId(1L);
        testCadet.setUser(testUser);
        testCadet.setGroupId(1L);

        User teacherUser = new User();
        teacherUser.setId(2L);
        teacherUser.setLogin("teacher");
        teacherUser.setUniversity(testUniversity);

        testTeacher = new Teacher();
        testTeacher.setUserId(2L);
        testTeacher.setUser(teacherUser);

        testUnit = new MeasurementUnit();
        testUnit.setId(1L);
        testUnit.setCode("кг");

        testParameter = new ExerciseParameter();
        testParameter.setId(1L);
        testParameter.setCode("вес");
        testParameter.setMeasurementUnit(testUnit);
        testParameter.setDefaultIntValue(new BigDecimal("100"));

        testExerciseCatalog = new ExerciseCatalog();
        testExerciseCatalog.setId(1L);
        testExerciseCatalog.setCode("BENCH_PRESS");
        testExerciseCatalog.setDescription("Жим лежа");
        testExerciseCatalog.setType("Сила");

        List<ExerciseParameter> params = new ArrayList<>();
        params.add(testParameter);
        testExerciseCatalog.setParameters(params);

        testApproach = new Approach();
        testApproach.setId(1L);
        testApproach.setApproachNumber((short) 1);
        testApproach.setExerciseParameter(testParameter);
        testApproach.setValue(new BigDecimal("100"));

        testExercise = new ExercisesInTraining();
        testExercise.setId(1L);
        testExercise.setExerciseCatalog(testExerciseCatalog);
        testExercise.setRestPeriod((short) 60);

        List<Approach> approaches = new ArrayList<>();
        approaches.add(testApproach);
        testExercise.setApproaches(approaches);

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

        testApproach.setExerciseInTraining(testExercise);
        testExercise.setTraining(testTraining);

        lenient().when(userDetails.getUsername()).thenReturn("cadet");
        lenient().when(userDetails.getAuthorities()).thenAnswer(invocation ->
                List.of(new SimpleGrantedAuthority("ROLE_CADET"))
        );
    }

    @Test
    void getTrainings_WithNoFilters_ShouldReturnAllTrainings() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findByCadetId(eq(1L), eq(pageable))).thenReturn(expectedPage);

        Page<TrainingDto> result = trainingService.getTrainings(
                userDetails, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(trainingRepository, times(1)).findByCadetId(1L, pageable);
    }

    @Test
    void getTrainings_WithDate_ShouldReturnFilteredByDate() {
        LocalDate date = now.minusDays(1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findByCadetIdAndDate(eq(1L), eq(date), eq(pageable)))
                .thenReturn(expectedPage);

        Page<TrainingDto> result = trainingService.getTrainings(
                userDetails, date, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(trainingRepository, times(1)).findByCadetIdAndDate(1L, date, pageable);
    }

    @Test
    void getTrainings_WithDateRange_ShouldReturnFilteredByDateRange() {
        LocalDate from = now.minusDays(10);
        LocalDate to = now;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findByCadetIdAndDateBetween(eq(1L), eq(from), eq(to), eq(pageable)))
                .thenReturn(expectedPage);

        Page<TrainingDto> result = trainingService.getTrainings(
                userDetails, null, from, to, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(trainingRepository, times(1)).findByCadetIdAndDateBetween(1L, from, to, pageable);
    }

    @Test
    void getTrainings_WithType_ShouldReturnFilteredByType() {
        String type = "Сила";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findByCadetIdAndType(eq(1L), eq(type), eq(pageable)))
                .thenReturn(expectedPage);

        Page<TrainingDto> result = trainingService.getTrainings(
                userDetails, null, null, null, type, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(trainingRepository, times(1)).findByCadetIdAndType(1L, type, pageable);
    }

    @Test
    void getTrainings_WithTypeAndDateRange_ShouldReturnFiltered() {
        String type = "Сила";
        LocalDate from = now.minusDays(10);
        LocalDate to = now;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findByCadetIdAndTypeAndDateBetween(eq(1L), eq(type), eq(from), eq(to), eq(pageable)))
                .thenReturn(expectedPage);

        Page<TrainingDto> result = trainingService.getTrainings(
                userDetails, null, from, to, type, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(trainingRepository, times(1)).findByCadetIdAndTypeAndDateBetween(1L, type, from, to, pageable);
    }

    @Test
    void getTrainings_WithInvalidDateRange_ShouldThrowException() {
        LocalDate from = now;
        LocalDate to = now.minusDays(1);
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getTrainings(userDetails, null, from, to, null, pageable));
    }

    @Test
    void getTrainings_WithInvalidType_ShouldThrowException() {
        String type = "Неверный тип";
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getTrainings(userDetails, null, null, null, type, pageable));
    }

    @Test
    void getTrainings_WithNullUserDetails_ShouldThrowException() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getTrainings(null, null, null, null, null, pageable));
    }

    @Test
    void getTrainings_WithNullPageable_ShouldThrowException() {
        assertThrows(NullPointerException.class,
                () -> trainingService.getTrainings(userDetails, null, null, null, null, null));
    }

    @Test
    void getTrainings_WhenCadetNotFound_ShouldThrowException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> trainingService.getTrainings(userDetails, null, null, null, null, pageable));
    }

    @Test
    void getTrainingById_WithValidId_ShouldReturnTraining() {
        Long trainingId = 1L;

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        TrainingDto result = trainingService.getTrainingById(userDetails, trainingId);

        assertNotNull(result);
        assertEquals(trainingId, result.getId());
        assertEquals(now.minusDays(1), result.getDate());
        assertEquals("Сила", result.getType());
    }

    @Test
    void getTrainingById_WithInvalidId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getTrainingById(userDetails, null));
    }

    @Test
    void getTrainingById_WhenTrainingNotFound_ShouldThrowException() {
        Long trainingId = 999L;

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> trainingService.getTrainingById(userDetails, trainingId));
    }

    @Test
    void getTrainingById_WhenNoAccess_ShouldThrowException() {
        Long trainingId = 1L;
        testTraining.setCadetId(2L);

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        assertThrows(RuntimeException.class,
                () -> trainingService.getTrainingById(userDetails, trainingId));
    }

    @Test
    void updateTraining_WithValidData_ShouldUpdateTraining() {
        Long trainingId = 1L;
        UpdateTrainingRequest request = new UpdateTrainingRequest();
        request.setDate(now.minusDays(2));
        request.setCurrentWeight(new BigDecimal("76.0"));
        request.setRestPeriod(180);

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        trainingService.updateTraining(userDetails, trainingId, request);

        verify(trainingRepository, times(1)).save(trainingCaptor.capture());
        Training savedTraining = trainingCaptor.getValue();
        assertEquals(now.minusDays(2), savedTraining.getDate());
        assertEquals(new BigDecimal("76.0"), savedTraining.getCurrentWeight());
        assertEquals((short) 180, savedTraining.getRestPeriod());
    }

    @Test
    void updateTraining_WithFutureDate_ShouldThrowException() {
        Long trainingId = 1L;
        UpdateTrainingRequest request = new UpdateTrainingRequest();
        request.setDate(now.plusDays(1));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        assertThrows(IllegalArgumentException.class,
                () -> trainingService.updateTraining(userDetails, trainingId, request));
    }

    @Test
    void updateTraining_WithInvalidWeight_ShouldThrowException() {
        Long trainingId = 1L;
        UpdateTrainingRequest request = new UpdateTrainingRequest();
        request.setCurrentWeight(new BigDecimal("200"));

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        assertThrows(IllegalArgumentException.class,
                () -> trainingService.updateTraining(userDetails, trainingId, request));
    }

    @Test
    void updateTraining_WithInvalidRestPeriod_ShouldThrowException() {
        Long trainingId = 1L;
        UpdateTrainingRequest request = new UpdateTrainingRequest();
        request.setRestPeriod(3601);

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(trainingId)).thenReturn(Optional.of(testTraining));

        assertThrows(IllegalArgumentException.class,
                () -> trainingService.updateTraining(userDetails, trainingId, request));
    }

    @Test
    void updateTraining_WithExerciseUpdate_ShouldUpdateExercise() {
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

        trainingService.updateTraining(userDetails, trainingId, request);

        verify(approachRepository, times(1)).save(any(Approach.class));
        verify(exercisesInTrainingRepository, times(1)).save(any(ExercisesInTraining.class));
        verify(trainingRepository, times(1)).save(any(Training.class));
    }

    @Test
    void deleteTraining_WithValidId_ShouldDelete() {
        Long trainingId = 1L;

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.existsByIdAndCadetId(trainingId, 1L)).thenReturn(true);
        doNothing().when(trainingRepository).deleteById(trainingId);

        trainingService.deleteTraining(userDetails, trainingId);

        verify(trainingRepository, times(1)).deleteById(trainingId);
    }

    @Test
    void deleteTraining_WithInvalidUserRole_ShouldThrowException() {
        Long trainingId = 1L;
        lenient().when(userDetails.getAuthorities()).thenAnswer(invocation ->
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );

        assertThrows(RuntimeException.class,
                () -> trainingService.deleteTraining(userDetails, trainingId));
    }

    @Test
    void deleteTraining_WhenTrainingNotBelongToCadet_ShouldThrowException() {
        Long trainingId = 1L;

        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.existsByIdAndCadetId(trainingId, 1L)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> trainingService.deleteTraining(userDetails, trainingId));
    }

    @Test
    void getExercisesByType_WithValidType_ShouldReturnExercises() {
        String type = "Сила";
        List<ExerciseCatalog> exercises = new ArrayList<>();
        exercises.add(testExerciseCatalog);

        when(exerciseCatalogRepository.findByType(type)).thenReturn(exercises);

        List<ExerciseCatalogDto> result = trainingService.getExercisesByType(type);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("BENCH_PRESS", result.get(0).getCode());
    }

    @Test
    void getExercisesByType_WithNullType_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getExercisesByType(null));
    }

    @Test
    void getExercisesByType_WithInvalidType_ShouldThrowException() {
        String type = "Неверный тип";

        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getExercisesByType(type));
    }

    @Test
    void getExercisesByType_WhenNoExercises_ShouldReturnEmptyList() {
        String type = "Сила";
        when(exerciseCatalogRepository.findByType(type)).thenReturn(new ArrayList<>());

        List<ExerciseCatalogDto> result = trainingService.getExercisesByType(type);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getExerciseWithDefaults_WithValidCode_ShouldReturnExercise() {
        String code = "BENCH_PRESS";

        when(exerciseCatalogRepository.findByCodeWithParameters(code))
                .thenReturn(Optional.of(testExerciseCatalog));

        ExerciseCatalogDto result = trainingService.getExerciseWithDefaults(code);

        assertNotNull(result);
        assertEquals("BENCH_PRESS", result.getCode());
        assertEquals("Жим лежа", result.getDescription());
        assertEquals("Сила", result.getType());
        assertEquals(1, result.getDefaultParameters().size());
    }

    @Test
    void getExerciseWithDefaults_WithEmptyCode_ShouldThrowException() {
        String code = "   ";

        assertThrows(IllegalArgumentException.class,
                () -> trainingService.getExerciseWithDefaults(code));
    }

    @Test
    void getExerciseWithDefaults_WhenExerciseNotFound_ShouldThrowException() {
        String code = "NON_EXISTENT";

        when(exerciseCatalogRepository.findByCodeWithParameters(code))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> trainingService.getExerciseWithDefaults(code));
    }

    @Test
    void getTrainingsByCadetId_WithValidData_ShouldReturnTrainings() {
        Long cadetId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Training> expectedPage = new PageImpl<>(List.of(testTraining));

        lenient().when(userDetails.getUsername()).thenReturn("teacher");
        lenient().when(userDetails.getAuthorities()).thenAnswer(invocation ->
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(trainingRepository.findByCadetId(eq(cadetId), eq(pageable))).thenReturn(expectedPage);

        Page<TrainingDto> result = trainingService.getTrainingsByCadetId(
                userDetails, cadetId, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getTrainingsByCadetId_WhenNoAccess_ShouldThrowException() {
        Long cadetId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        University differentUniversity = new University();
        differentUniversity.setId(2L);
        testGroup.setUniversity(differentUniversity);

        lenient().when(userDetails.getUsername()).thenReturn("teacher");
        lenient().when(userDetails.getAuthorities()).thenAnswer(invocation ->
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        assertThrows(RuntimeException.class,
                () -> trainingService.getTrainingsByCadetId(
                        userDetails, cadetId, null, null, null, null, pageable));
    }

    @Test
    void isValidTrainingType_WithValidTypes_ShouldReturnTrue() {
        assertDoesNotThrow(() -> trainingService.getExercisesByType("Сила"));
        assertDoesNotThrow(() -> trainingService.getExercisesByType("Скорость"));
        assertDoesNotThrow(() -> trainingService.getExercisesByType("Выносливость"));
    }

    @Test
    void convertToDto_WithNullTraining_ShouldReturnNull() {
        when(cadetRepository.findByUserLogin("cadet")).thenReturn(Optional.of(testCadet));
        when(trainingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> trainingService.getTrainingById(userDetails, 999L));
    }
}