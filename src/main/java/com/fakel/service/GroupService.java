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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class GroupService {

    private static final Logger log = LoggerFactory.getLogger(GroupService.class);

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

        log.info("Получение групп с фильтрами: user={}, year={}, onlyMyGroups={}, page={}, size={}",
                userDetails != null ? userDetails.getUsername() : null, year, onlyMyGroups,
                pageable.getPageNumber(), pageable.getPageSize());

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения групп с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (pageable == null) {
            log.warn("Параметры пагинации null");
            throw new IllegalArgumentException("Параметры пагинации не могут быть пустыми");
        }

        if (year != null && (year < 2000 || year > LocalDate.now().getYear() + 5)) {
            log.warn("Некорректный год: {}", year);
            throw new IllegalArgumentException("Некорректный год. Допустимый диапазон: 2000-" + (LocalDate.now().getYear() + 5));
        }

        // Находим преподавателя
        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        Long universityId = teacher.getUniversity().getId();
        Long teacherId = teacher.getUserId();

        log.debug("Преподаватель: ID={}, университет={}", teacherId, universityId);

        // Выбираем режим
        if (onlyMyGroups) {
            log.debug("Режим: только мои группы");
            return getMyGroupsWithFilter(teacherId, universityId, year, pageable);
        } else {
            log.debug("Режим: все группы университета");
            return getAllUniversityGroupsWithFilter(universityId, year, pageable);
        }
    }

    /**
     * Все группы университета (с фильтром по году)
     */
    private Page<GroupDto> getAllUniversityGroupsWithFilter(
            Long universityId, Integer year, Pageable pageable) {

        log.debug("Получение всех групп университета {} с фильтром по году {}", universityId, year);

        if (universityId == null || universityId <= 0) {
            log.warn("Некорректный ID университета: {}", universityId);
            throw new IllegalArgumentException("ID университета должен быть положительным числом");
        }

        Page<Group> groups;

        if (year != null) {
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);
            log.debug("Диапазон дат: {} - {}", start, end);
            groups = groupRepository.findByUniversityIdAndFoundationDateBetween(
                    universityId, start, end, pageable);
        } else {
            groups = groupRepository.findByUniversityIdOrderByFoundationDateDescNumberAsc(
                    universityId, pageable);
        }

        log.debug("Найдено {} групп", groups.getTotalElements());
        return groups.map(this::convertToDto);
    }

    /**
     * Только группы, где преподает (с фильтром по году)
     */
    private Page<GroupDto> getMyGroupsWithFilter(
            Long teacherId, Long universityId, Integer year, Pageable pageable) {

        log.debug("Получение групп преподавателя {} с фильтром по году {}", teacherId, year);

        if (teacherId == null || teacherId <= 0) {
            log.warn("Некорректный ID преподавателя: {}", teacherId);
            throw new IllegalArgumentException("ID преподавателя должен быть положительным числом");
        }

        if (universityId == null || universityId <= 0) {
            log.warn("Некорректный ID университета: {}", universityId);
            throw new IllegalArgumentException("ID университета должен быть положительным числом");
        }

        // Получаем все группы преподавателя
        List<Group> allMyGroups = groupTeacherRepository.findGroupsByTeacherId(teacherId);

        if (allMyGroups == null || allMyGroups.isEmpty()) {
            log.debug("Преподаватель {} не привязан ни к одной группе", teacherId);
            return new PageImpl<>(List.of(), pageable, 0);
        }

        log.debug("Всего групп у преподавателя: {}", allMyGroups.size());

        // Фильтруем по университету
        List<Group> myUniversityGroups = allMyGroups.stream()
                .filter(g -> g != null && g.getUniversity() != null &&
                        g.getUniversity().getId().equals(universityId))
                .collect(Collectors.toList());

        log.debug("После фильтрации по университету: {} групп", myUniversityGroups.size());

        // Фильтруем по году, если указан
        if (year != null) {
            myUniversityGroups = myUniversityGroups.stream()
                    .filter(g -> g.getFoundationDate() != null &&
                            g.getFoundationDate().getYear() == year)
                    .collect(Collectors.toList());
            log.debug("После фильтрации по году {}: {} групп", year, myUniversityGroups.size());
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
            log.debug("Start {} больше размера списка {}, возвращаем пустую страницу", start, myUniversityGroups.size());
            return new PageImpl<>(List.of(), pageable, myUniversityGroups.size());
        }

        int end = Math.min((start + pageable.getPageSize()), myUniversityGroups.size());
        log.debug("Пагинация: элементы с {} по {} из {}", start, end, myUniversityGroups.size());

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

        log.info("Получение группы по ID: {}, пользователь: {}", id,
                userDetails != null ? userDetails.getUsername() : null);

        if (id == null || id <= 0) {
            log.warn("Некорректный ID группы: {}", id);
            throw new IllegalArgumentException("ID группы должен быть положительным числом");
        }

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения группы с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        Group group = groupRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Группа не найдена с id: {}", id);
                    return new RuntimeException("Группа не найдена с id: " + id);
                });

        checkAccess(userDetails, group);
        log.debug("Группа найдена: номер={}, университет={}", group.getNumber(),
                group.getUniversity() != null ? group.getUniversity().getCode() : null);

        return convertToDto(group);
    }

    /**
     * GET /api/groups/number/{number}
     */
    public GroupDto getGroupByNumber(String number, UserDetails userDetails) {

        log.info("Получение группы по номеру: {}, пользователь: {}", number,
                userDetails != null ? userDetails.getUsername() : null);

        if (number == null || number.trim().isEmpty()) {
            log.warn("Номер группы пустой");
            throw new IllegalArgumentException("Номер группы не может быть пустым");
        }

        String trimmedNumber = number.trim();
        log.debug("Номер после trim: '{}'", trimmedNumber);

        if (trimmedNumber.length() < 2 || trimmedNumber.length() > 32) {
            log.warn("Некорректная длина номера: {} символов", trimmedNumber.length());
            throw new IllegalArgumentException("Номер группы должен быть от 2 до 32 символов");
        }

        if (!trimmedNumber.matches("^[А-Яа-яЁёA-Za-z0-9\\-]+$")) {
            log.warn("Номер содержит недопустимые символы: {}", trimmedNumber);
            throw new IllegalArgumentException("Номер группы содержит недопустимые символы");
        }

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения группы с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        Group group = groupRepository.findByNumber(trimmedNumber)
                .orElseThrow(() -> {
                    log.warn("Группа не найдена с номером: {}", number);
                    return new RuntimeException("Группа не найдена с номером: " + number);
                });

        checkAccess(userDetails, group);
        log.debug("Группа найдена: id={}, университет={}", group.getId(),
                group.getUniversity() != null ? group.getUniversity().getCode() : null);

        return convertToDto(group);
    }

    /**
     * Проверка доступа преподавателя к группе
     */
    private void checkAccess(UserDetails userDetails, Group group) {
        log.debug("Проверка доступа к группе для пользователя {}", userDetails.getUsername());

        if (group == null) {
            log.warn("Группа null при проверке доступа");
            throw new IllegalArgumentException("Группа не может быть null");
        }

        if (group.getUniversity() == null) {
            log.warn("У группы {} не указан университет", group.getId());
            throw new RuntimeException("У группы не указан университет");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        if (teacher.getUniversity() == null) {
            log.warn("У преподавателя {} не указан университет", teacher.getUserId());
            throw new RuntimeException("У преподавателя не указан университет");
        }

        if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
            log.warn("Нет доступа к группе {} для преподавателя {} (университеты: {} vs {})",
                    group.getId(), teacher.getUserId(),
                    group.getUniversity().getId(), teacher.getUniversity().getId());
            throw new RuntimeException("Нет доступа к этой группе");
        }

        log.debug("Доступ разрешен");
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
            log.trace("Количество курсантов в группе {}: {}", group.getId(), studentCount);
        } catch (Exception e) {
            log.warn("Ошибка при подсчете курсантов для группы {}: {}", group.getId(), e.getMessage());
        }

        String universityCode = group.getUniversity() != null ? group.getUniversity().getCode() : "Неизвестно";
        Long universityId = group.getUniversity() != null ? group.getUniversity().getId() : null;

        return new GroupDto(
                group.getId(),
                group.getNumber(),
                group.getFoundationDate(),
                universityCode,
                universityId,
                studentCount
        );
    }

    /**
     * GET /api/groups/statistics?year=2023&onlyMyGroups=true
     */
    public Object getStatistics(UserDetails userDetails, Integer year, boolean onlyMyGroups) {

        log.info("Получение статистики по группам: user={}, year={}, onlyMyGroups={}",
                userDetails != null ? userDetails.getUsername() : null, year, onlyMyGroups);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения статистики с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (year != null && (year < 2000 || year > LocalDate.now().getYear() + 5)) {
            log.warn("Некорректный год: {}", year);
            throw new IllegalArgumentException("Некорректный год. Допустимый диапазон: 2000-" + (LocalDate.now().getYear() + 5));
        }

        // TODO: добавить статистику по группам
        log.debug("Метод getStatistics еще не реализован");
        return null;
    }
}