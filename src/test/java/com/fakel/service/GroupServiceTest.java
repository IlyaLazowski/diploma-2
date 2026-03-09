package com.fakel.service;

import com.fakel.dto.GroupDto;
import com.fakel.model.Group;
import com.fakel.model.Teacher;
import com.fakel.model.University;
import com.fakel.model.User;
import com.fakel.repository.CadetRepository;
import com.fakel.repository.GroupRepository;
import com.fakel.repository.GroupTeacherRepository;
import com.fakel.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private CadetRepository cadetRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private GroupTeacherRepository groupTeacherRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private GroupService groupService;

    private Teacher testTeacher;
    private Group testGroup1;
    private Group testGroup2;
    private Group testGroup3;
    private University testUniversity;
    private LocalDate now;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();

        // Создаем университет
        testUniversity = new University();
        testUniversity.setId(1L);
        testUniversity.setCode("ВУЗ");

        // Создаем преподавателя
        User teacherUser = new User();
        teacherUser.setId(1L);
        teacherUser.setLogin("teacher");
        teacherUser.setUniversity(testUniversity);

        testTeacher = new Teacher();
        testTeacher.setUserId(1L);
        testTeacher.setUser(teacherUser);

        // Создаем группы
        testGroup1 = new Group();
        testGroup1.setId(1L);
        testGroup1.setNumber("21-ВП-1");
        testGroup1.setFoundationDate(now.minusYears(2));
        testGroup1.setUniversity(testUniversity);

        testGroup2 = new Group();
        testGroup2.setId(2L);
        testGroup2.setNumber("21-ВП-2");
        testGroup2.setFoundationDate(now.minusYears(1));
        testGroup2.setUniversity(testUniversity);

        testGroup3 = new Group();
        testGroup3.setId(3L);
        testGroup3.setNumber("22-ВП-1");
        testGroup3.setFoundationDate(now);
        testGroup3.setUniversity(testUniversity);

        // Настройка UserDetails - БЕЗ lenient, все моки будут в тестах
        when(userDetails.getUsername()).thenReturn("teacher");
    }

    // ============= ТЕСТЫ ДЛЯ getGroupsWithFilters =============

    @Test
    void getGroupsWithFilters_AllGroups_NoYear_ShouldReturnAll() {
        // Given
        Integer year = null;
        boolean onlyMyGroups = false;
        Pageable pageable = PageRequest.of(0, 10);
        List<Group> groups = List.of(testGroup1, testGroup2, testGroup3);
        Page<Group> expectedPage = new PageImpl<>(groups);

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupRepository.findByUniversityIdOrderByFoundationDateDescNumberAsc(
                eq(1L), eq(pageable))).thenReturn(expectedPage);
        when(cadetRepository.countByGroupId(anyLong())).thenReturn(5L);

        // When
        Page<GroupDto> result = groupService.getGroupsWithFilters(
                userDetails, year, onlyMyGroups, pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());

        verify(groupRepository, times(1))
                .findByUniversityIdOrderByFoundationDateDescNumberAsc(1L, pageable);
    }

    @Test
    void getGroupsWithFilters_AllGroups_WithYear_ShouldReturnFiltered() {
        // Given
        Integer year = now.getYear();
        boolean onlyMyGroups = false;
        Pageable pageable = PageRequest.of(0, 10);
        List<Group> groups = List.of(testGroup3);
        Page<Group> expectedPage = new PageImpl<>(groups);

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupRepository.findByUniversityIdAndFoundationDateBetween(
                eq(1L), any(LocalDate.class), any(LocalDate.class), eq(pageable)))
                .thenReturn(expectedPage);
        when(cadetRepository.countByGroupId(anyLong())).thenReturn(5L);

        // When
        Page<GroupDto> result = groupService.getGroupsWithFilters(
                userDetails, year, onlyMyGroups, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getGroupsWithFilters_MyGroups_NoYear_ShouldReturnMyGroups() {
        // Given
        Integer year = null;
        boolean onlyMyGroups = true;
        Pageable pageable = PageRequest.of(0, 10);
        List<Group> myGroups = List.of(testGroup1, testGroup2);

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupTeacherRepository.findGroupsByTeacherId(1L)).thenReturn(myGroups);
        when(cadetRepository.countByGroupId(anyLong())).thenReturn(5L);

        // When
        Page<GroupDto> result = groupService.getGroupsWithFilters(
                userDetails, year, onlyMyGroups, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getGroupsWithFilters_MyGroups_WithYear_ShouldReturnFilteredMyGroups() {
        // Given
        Integer year = now.minusYears(1).getYear();
        boolean onlyMyGroups = true;
        Pageable pageable = PageRequest.of(0, 10);
        List<Group> allMyGroups = List.of(testGroup1, testGroup2, testGroup3);

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupTeacherRepository.findGroupsByTeacherId(1L)).thenReturn(allMyGroups);
        when(cadetRepository.countByGroupId(anyLong())).thenReturn(5L);

        // When
        Page<GroupDto> result = groupService.getGroupsWithFilters(
                userDetails, year, onlyMyGroups, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("21-ВП-2", result.getContent().get(0).getNumber());
    }

    @Test
    void getGroupsWithFilters_MyGroups_Empty_ShouldReturnEmptyPage() {
        // Given
        Integer year = null;
        boolean onlyMyGroups = true;
        Pageable pageable = PageRequest.of(0, 10);

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupTeacherRepository.findGroupsByTeacherId(1L)).thenReturn(new ArrayList<>());

        // When
        Page<GroupDto> result = groupService.getGroupsWithFilters(
                userDetails, year, onlyMyGroups, pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getGroupsWithFilters_InvalidYear_ShouldThrowException() {
        // Given
        Integer year = 1999;
        boolean onlyMyGroups = false;
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> groupService.getGroupsWithFilters(userDetails, year, onlyMyGroups, pageable));
    }

    @Test
    void getGroupsWithFilters_NullUserDetails_ShouldThrowException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> groupService.getGroupsWithFilters(null, null, false, pageable));
    }

    @Test
    void getGroupsWithFilters_NullPageable_ShouldThrowException() {
        // Given
        assertThrows(NullPointerException.class,
                () -> groupService.getGroupsWithFilters(userDetails, null, false, null));
    }

    @Test
    void getGroupsWithFilters_TeacherNotFound_ShouldThrowException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> groupService.getGroupsWithFilters(userDetails, null, false, pageable));
    }

    // ============= ТЕСТЫ ДЛЯ getGroupById =============

    @Test
    void getGroupById_ValidId_ShouldReturnGroup() {
        // Given
        Long groupId = 1L;

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));
        when(cadetRepository.countByGroupId(groupId)).thenReturn(10L);

        // When
        GroupDto result = groupService.getGroupById(groupId, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(groupId, result.getId());
        assertEquals("21-ВП-1", result.getNumber());
        assertEquals(10, result.getStudentCount());
    }

    @Test
    void getGroupById_InvalidId_ShouldThrowException() {
        // Given
        assertThrows(IllegalArgumentException.class,
                () -> groupService.getGroupById(null, userDetails));
    }

    @Test
    void getGroupById_GroupNotFound_ShouldThrowException() {
        // Given
        Long groupId = 999L;

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> groupService.getGroupById(groupId, userDetails));
    }

    @Test
    void getGroupById_NoAccess_ShouldThrowException() {
        // Given
        Long groupId = 1L;
        University differentUniversity = new University();
        differentUniversity.setId(2L);
        testGroup1.setUniversity(differentUniversity);

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(testGroup1));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> groupService.getGroupById(groupId, userDetails));
    }

    // ============= ТЕСТЫ ДЛЯ getGroupByNumber =============

    @Test
    void getGroupByNumber_ValidNumber_ShouldReturnGroup() {
        // Given
        String number = "21-ВП-1";

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupRepository.findByNumber(number)).thenReturn(Optional.of(testGroup1));
        when(cadetRepository.countByGroupId(1L)).thenReturn(10L);

        // When
        GroupDto result = groupService.getGroupByNumber(number, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(number, result.getNumber());
        assertEquals(10, result.getStudentCount());
    }

    @Test
    void getGroupByNumber_WithSpaces_ShouldTrimAndReturnGroup() {
        // Given
        String number = "  21-ВП-1  ";

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupRepository.findByNumber("21-ВП-1")).thenReturn(Optional.of(testGroup1));
        when(cadetRepository.countByGroupId(1L)).thenReturn(10L);

        // When
        GroupDto result = groupService.getGroupByNumber(number, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("21-ВП-1", result.getNumber());
    }

    @Test
    void getGroupByNumber_EmptyNumber_ShouldThrowException() {
        // Given
        String number = "   ";

        assertThrows(IllegalArgumentException.class,
                () -> groupService.getGroupByNumber(number, userDetails));
    }

    @Test
    void getGroupByNumber_TooShortNumber_ShouldThrowException() {
        // Given
        String number = "A";

        assertThrows(IllegalArgumentException.class,
                () -> groupService.getGroupByNumber(number, userDetails));
    }

    @Test
    void getGroupByNumber_InvalidCharacters_ShouldThrowException() {
        // Given
        String number = "21@#$";

        assertThrows(IllegalArgumentException.class,
                () -> groupService.getGroupByNumber(number, userDetails));
    }

    @Test
    void getGroupByNumber_GroupNotFound_ShouldThrowException() {
        // Given
        String number = "99-99";

        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupRepository.findByNumber(number)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> groupService.getGroupByNumber(number, userDetails));
    }

    // ============= ТЕСТЫ ДЛЯ getStatistics =============

    @Test
    void getStatistics_ShouldReturnNull() {
        // Given
        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));

        // When
        Object result = groupService.getStatistics(userDetails, null, false);

        // Then
        assertNull(result);
    }

    @Test
    void getStatistics_InvalidYear_ShouldThrowException() {
        // Given
        Integer year = 1999;

        assertThrows(IllegalArgumentException.class,
                () -> groupService.getStatistics(userDetails, year, false));
    }

    // ============= ТЕСТЫ ДЛЯ convertToDto =============

    @Test
    void convertToDto_WithNullGroup_ShouldReturnNull() {
        // Given
        when(teacherRepository.findByUserLogin("teacher")).thenReturn(Optional.of(testTeacher));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup1));
        when(cadetRepository.countByGroupId(1L)).thenThrow(new RuntimeException("DB error"));

        // When
        GroupDto result = groupService.getGroupById(1L, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getStudentCount());
    }
}