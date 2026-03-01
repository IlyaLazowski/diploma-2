package com.fakel.service;
import java.util.Map;
import java.util.stream.Collectors;
import com.fakel.dto.StandardResultDto;
import com.fakel.dto.ControlSummaryDto;
import com.fakel.dto.GroupDto;
import com.fakel.model.*;
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
import java.util.List;
import java.util.stream.Collectors;

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

        // Получаем все группы преподавателя
        List<Group> allMyGroups = groupTeacherRepository.findGroupsByTeacherId(teacherId);

        // Фильтруем по университету
        List<Group> myUniversityGroups = allMyGroups.stream()
                .filter(g -> g.getUniversity().getId().equals(universityId))
                .collect(Collectors.toList());

        // Фильтруем по году, если указан
        if (year != null) {
            myUniversityGroups = myUniversityGroups.stream()
                    .filter(g -> g.getFoundationDate().getYear() == year)
                    .collect(Collectors.toList());
        }

        // Сортируем (сначала новые, потом по номеру)
        myUniversityGroups.sort((g1, g2) -> {
            int dateCompare = g2.getFoundationDate().compareTo(g1.getFoundationDate());
            if (dateCompare != 0) return dateCompare;
            return g1.getNumber().compareTo(g2.getNumber());
        });

        // Ручная пагинация
        int start = (int) pageable.getOffset();
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
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Группа не найдена с id: " + id));

        checkAccess(userDetails, group);
        return convertToDto(group);
    }

    /**
     * GET /api/groups/number/{number}
     */
    public GroupDto getGroupByNumber(String number, UserDetails userDetails) {
        Group group = groupRepository.findByNumber(number)
                .orElseThrow(() -> new RuntimeException("Группа не найдена с номером: " + number));

        checkAccess(userDetails, group);
        return convertToDto(group);
    }

    /**
     * Проверка доступа преподавателя к группе
     */
    private void checkAccess(UserDetails userDetails, Group group) {
        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
            throw new RuntimeException("Нет доступа к этой группе");
        }
    }

    /**
     * Преобразование Group → GroupDto
     */
    private GroupDto convertToDto(Group group) {
        Integer studentCount = cadetRepository.countByGroupId(group.getId()).intValue();

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
        // TODO: добавить статистику по группам
        return null;
    }



}