package com.fakel.service;

import com.fakel.dto.UniversityDto;
import com.fakel.model.University;
import com.fakel.repository.UniversityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UniversityServiceTest {

    @Mock
    private UniversityRepository universityRepository;

    @InjectMocks
    private UniversityService universityService;

    private University university1;
    private University university2;
    private University university3;

    @BeforeEach
    void setUp() {
        // Создаем тестовые университеты
        university1 = new University();
        university1.setId(1L);
        university1.setCode("БГУ");
        university1.setMark(new BigDecimal("9.5"));

        university2 = new University();
        university2.setId(2L);
        university2.setCode("БНТУ");
        university2.setMark(new BigDecimal("8.7"));

        university3 = new University();
        university3.setId(3L);
        university3.setCode("БГУИР");
        university3.setMark(new BigDecimal("9.2"));
    }

    @Test
    void getAllUniversities_WithValidData_ShouldReturnList() {
        // Given
        List<University> universities = Arrays.asList(university1, university2, university3);
        when(universityRepository.findAllByOrderByMarkDesc()).thenReturn(universities);

        // When
        List<UniversityDto> result = universityService.getAllUniversities();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // Проверяем первый университет
        UniversityDto dto1 = result.get(0);
        assertEquals(1L, dto1.getId());
        assertEquals("БГУ", dto1.getName());
        assertEquals(new BigDecimal("9.5"), dto1.getMark());

        // Проверяем второй университет
        UniversityDto dto2 = result.get(1);
        assertEquals(2L, dto2.getId());
        assertEquals("БНТУ", dto2.getName());
        assertEquals(new BigDecimal("8.7"), dto2.getMark());

        // Проверяем третий университет
        UniversityDto dto3 = result.get(2);
        assertEquals(3L, dto3.getId());
        assertEquals("БГУИР", dto3.getName());
        assertEquals(new BigDecimal("9.2"), dto3.getMark());

        verify(universityRepository, times(1)).findAllByOrderByMarkDesc();
    }

    @Test
    void getAllUniversities_WithEmptyList_ShouldReturnEmptyList() {
        // Given
        when(universityRepository.findAllByOrderByMarkDesc()).thenReturn(new ArrayList<>());

        // When
        List<UniversityDto> result = universityService.getAllUniversities();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(universityRepository, times(1)).findAllByOrderByMarkDesc();
    }

    @Test
    void getAllUniversities_WithNullList_ShouldReturnEmptyList() {
        // Given
        when(universityRepository.findAllByOrderByMarkDesc()).thenReturn(null);

        // When
        List<UniversityDto> result = universityService.getAllUniversities();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(universityRepository, times(1)).findAllByOrderByMarkDesc();
    }

    @Test
    void getAllUniversities_WhenRepositoryThrowsException_ShouldReturnEmptyList() {
        // Given
        when(universityRepository.findAllByOrderByMarkDesc()).thenThrow(new RuntimeException("DB Error"));

        // When
        List<UniversityDto> result = universityService.getAllUniversities();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(universityRepository, times(1)).findAllByOrderByMarkDesc();
    }

    @Test
    void getAllUniversities_WithNullElementInList_ShouldSkipNullElement() {
        // Given
        List<University> universities = new ArrayList<>();
        universities.add(university1);
        universities.add(null);
        universities.add(university2);

        when(universityRepository.findAllByOrderByMarkDesc()).thenReturn(universities);

        // When
        List<UniversityDto> result = universityService.getAllUniversities();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // Проверяем, что null элемент пропущен
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());

        verify(universityRepository, times(1)).findAllByOrderByMarkDesc();
    }

    @Test
    void getAllUniversities_WithNullCode_ShouldUseEmptyString() {
        // Given
        university2.setCode(null);
        List<University> universities = Arrays.asList(university1, university2);

        when(universityRepository.findAllByOrderByMarkDesc()).thenReturn(universities);

        // When
        List<UniversityDto> result = universityService.getAllUniversities();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // Проверяем университет с null code
        UniversityDto dto2 = result.get(1);
        assertEquals(2L, dto2.getId());
        assertEquals("", dto2.getName()); // Должна быть пустая строка
        assertEquals(new BigDecimal("8.7"), dto2.getMark());

        verify(universityRepository, times(1)).findAllByOrderByMarkDesc();
    }

    @Test
    void getAllUniversities_WithNullMark_ShouldWork() {
        // Given
        university2.setMark(null);
        List<University> universities = Arrays.asList(university1, university2);

        when(universityRepository.findAllByOrderByMarkDesc()).thenReturn(universities);

        // When
        List<UniversityDto> result = universityService.getAllUniversities();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        UniversityDto dto2 = result.get(1);
        assertEquals(2L, dto2.getId());
        assertEquals("БНТУ", dto2.getName());
        assertNull(dto2.getMark()); // Mark может быть null

        verify(universityRepository, times(1)).findAllByOrderByMarkDesc();
    }

    @Test
    void getAllUniversities_ShouldMaintainOrder() {
        // Given
        // Проверяем, что порядок сохраняется (уже отсортировано по убыванию mark)
        List<University> universities = Arrays.asList(university1, university3, university2);
        when(universityRepository.findAllByOrderByMarkDesc()).thenReturn(universities);

        // When
        List<UniversityDto> result = universityService.getAllUniversities();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // Порядок должен быть таким же, как в репозитории
        assertEquals(1L, result.get(0).getId()); // БГУ (9.5)
        assertEquals(3L, result.get(1).getId()); // БГУИР (9.2)
        assertEquals(2L, result.get(2).getId()); // БНТУ (8.7)

        verify(universityRepository, times(1)).findAllByOrderByMarkDesc();
    }

    @Test
    void convertToDto_WithNullUniversity_ShouldReturnNull() {
        // Этот метод приватный, тестируем через getAllUniversities
        List<University> universities = new ArrayList<>();
        universities.add(null);

        when(universityRepository.findAllByOrderByMarkDesc()).thenReturn(universities);

        // When
        List<UniversityDto> result = universityService.getAllUniversities();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty()); // null элемент должен быть пропущен
    }

    @Test
    void convertToDto_WithNullCode_ShouldReturnDtoWithEmptyString() {
        // Тестируем через getAllUniversities
        university1.setCode(null);
        List<University> universities = Arrays.asList(university1);

        when(universityRepository.findAllByOrderByMarkDesc()).thenReturn(universities);

        // When
        List<UniversityDto> result = universityService.getAllUniversities();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("", result.get(0).getName());
    }

    @Test
    void getAllUniversities_WithLargeList_ShouldWork() {
        // Given
        List<University> largeList = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            University uni = new University();
            uni.setId((long) i);
            uni.setCode("Университет " + i);
            uni.setMark(new BigDecimal(i % 10 + "." + i % 10));
            largeList.add(uni);
        }

        when(universityRepository.findAllByOrderByMarkDesc()).thenReturn(largeList);

        // When
        List<UniversityDto> result = universityService.getAllUniversities();

        // Then
        assertNotNull(result);
        assertEquals(100, result.size());
        assertEquals("Университет 1", result.get(0).getName());
        assertEquals("Университет 100", result.get(99).getName());

        verify(universityRepository, times(1)).findAllByOrderByMarkDesc();
    }
}