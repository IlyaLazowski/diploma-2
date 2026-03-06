package com.fakel.service;

import java.util.List;
import java.util.stream.Collectors;

import com.fakel.dto.GroupDto;
import com.fakel.model.Group;
import com.fakel.model.Teacher;
import com.fakel.repository.CadetRepository;
import com.fakel.repository.GroupRepository;
import com.fakel.repository.GroupTeacherRepository;
import com.fakel.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private CadetRepository cadetRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private GroupTeacherRepository groupTeacherRepository;

    /**
     * GET /api/groups?page=0&size=10
     * GET /api/groups?year=2023
     * GET /api/groups?onlyMyGroups=true
     * GET /api/groups?year=2023&onlyMyGroups=true
     */
    public Page<GroupDto> getGroupsWithFilters(
            UserDetails userDetails,
            Integer year,
            boolean onlyMyGroups,
            Pageable pageable) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (pageable == null) {
            throw new IllegalArgumentException("Параметры пагинации не могут быть пустыми");
        }

        if (year != null && (year < 2000 || year > LocalDate.now().getYear() + 5)) {
            throw new IllegalArgumentException("Некорректный год. Допустимый диапазон: 2000-" + (LocalDate.now().getYear() + 5));
        }

        // Находим преподавателя
        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Long universityId = teacher.getUniversity().getId();
        Long teacherId = teacher.getUserId();

        // Выбираем режим
        if (onlyMyGroups) {
            return getMyGroupsWithFilter(teacherId, universityId, year, pageable);
        } else {
            return getAllUniversityGroupsWithFilter(universityId, year, pageable);
        }
    }

    /**
     * Все группы университета (с фильтром по году)
     */
    private Page<GroupDto> getAllUniversityGroupsWithFilter(
            Long universityId, Integer year, Pageable pageable) {

        if (universityId == null || universityId <= 0) {
            throw new IllegalArgumentException("ID университета должен быть положительным числом");
        }

        Page<Group> groups;

        if (year != null) {
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);
            groups = groupRepository.findByUniversityIdAndFoundationDateBetween(
                    universityId, start, end, pageable);
        } else {
            groups = groupRepository.findByUniversityIdOrderByFoundationDateDescNumberAsc(
                    universityId, pageable);
        }

        return groups.map(this::convertToDto);
    }

    /**
     * Только группы, где преподает (с фильтром по году)
     */
    private Page<GroupDto> getMyGroupsWithFilter(
            Long teacherId, Long universityId, Integer year, Pageable pageable) {

        if (teacherId == null || teacherId <= 0) {
            throw new IllegalArgumentException("ID преподавателя должен быть положительным числом");
        }

        if (universityId == null || universityId <= 0) {
            throw new IllegalArgumentException("ID университета должен быть положительным числом");
        }

        // Получаем все группы преподавателя
        List<Group> allMyGroups = groupTeacherRepository.findGroupsByTeacherId(teacherId);

        if (allMyGroups == null) {
            allMyGroups = List.of();
        }

        // Фильтруем по университету
        List<Group> myUniversityGroups = allMyGroups.stream()
                .filter(g -> g != null && g.getUniversity() != null &&
                        g.getUniversity().getId().equals(universityId))
                .collect(Collectors.toList());

        // Фильтруем по году, если указан
        if (year != null) {
            myUniversityGroups = myUniversityGroups.stream()
                    .filter(g -> g.getFoundationDate() != null &&
                            g.getFoundationDate().getYear() == year)
                    .collect(Collectors.toList());
        }

        // Сортируем (сначала новые, потом по номеру)
        myUniversityGroups.sort((g1, g2) -> {
            if (g1.getFoundationDate() == null && g2.getFoundationDate() == null) return 0;
            if (g1.getFoundationDate() == null) return 1;
            if (g2.getFoundationDate() == null) return -1;

            int dateCompare = g2.getFoundationDate().compareTo(g1.getFoundationDate());
            if (dateCompare != 0) return dateCompare;

            if (g1.getNumber() == null && g2.getNumber() == null) return 0;
            if (g1.getNumber() == null) return 1;
            if (g2.getNumber() == null) return -1;
            return g1.getNumber().compareTo(g2.getNumber());
        });

        // Ручная пагинация
        int start = (int) pageable.getOffset();
        if (start >= myUniversityGroups.size()) {
            return new PageImpl<>(List.of(), pageable, myUniversityGroups.size());
        }

        int end = Math.min((start + pageable.getPageSize()), myUniversityGroups.size());

        List<GroupDto> pageContent = myUniversityGroups.subList(start, end)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, myUniversityGroups.size());
    }

    /**
     * GET /api/groups/{id}
     */
    public GroupDto getGroupById(Long id, UserDetails userDetails) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID группы должен быть положительным числом");
        }

        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Группа не найдена с id: " + id));

        checkAccess(userDetails, group);
        return convertToDto(group);
    }

    /**
     * GET /api/groups/number/{number}
     */
    public GroupDto getGroupByNumber(String number, UserDetails userDetails) {

        if (number == null || number.trim().isEmpty()) {
            throw new IllegalArgumentException("Номер группы не может быть пустым");
        }

        if (number.length() < 2 || number.length() > 32) {
            throw new IllegalArgumentException("Номер группы должен быть от 2 до 32 символов");
        }

        if (!number.matches("^[А-Яа-яЁёA-Za-z0-9\\-]+$")) {
            throw new IllegalArgumentException("Номер группы содержит недопустимые символы");
        }

        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        Group group = groupRepository.findByNumber(number.trim())
                .orElseThrow(() -> new RuntimeException("Группа не найдена с номером: " + number));

        checkAccess(userDetails, group);
        return convertToDto(group);
    }

    /**
     * Проверка доступа преподавателя к группе
     */
    private void checkAccess(UserDetails userDetails, Group group) {

        if (group == null) {
            throw new IllegalArgumentException("Группа не может быть null");
        }

        if (group.getUniversity() == null) {
            throw new RuntimeException("У группы не указан университет");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        if (teacher.getUniversity() == null) {
            throw new RuntimeException("У преподавателя не указан университет");
        }

        if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
            throw new RuntimeException("Нет доступа к этой группе");
        }
    }

    /**
     * Преобразование Group → GroupDto
     */
    private GroupDto convertToDto(Group group) {
        if (group == null) {
            return null;
        }

        Integer studentCount = 0;
        try {
            studentCount = cadetRepository.countByGroupId(group.getId()).intValue();
        } catch (Exception e) {
            // Если ошибка подсчета, оставляем 0
        }

        return new GroupDto(
                group.getId(),
                group.getNumber(),
                group.getFoundationDate(),
                group.getUniversity() != null ? group.getUniversity().getCode() : "Неизвестно",
                group.getUniversity() != null ? group.getUniversity().getId() : null,
                studentCount
        );
    }

    /**
     * GET /api/groups/statistics?year=2023&onlyMyGroups=true
     */
    public Object getStatistics(UserDetails userDetails, Integer year, boolean onlyMyGroups) {

        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (year != null && (year < 2000 || year > LocalDate.now().getYear() + 5)) {
            throw new IllegalArgumentException("Некорректный год. Допустимый диапазон: 2000-" + (LocalDate.now().getYear() + 5));
        }

        // TODO: добавить статистику по группам
        return null;
    }
}