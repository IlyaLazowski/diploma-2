package com.fakel.service;

import com.fakel.dto.*;
import com.fakel.model.*;
import com.fakel.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
public class ControlService {

    @Autowired
    private StandardRepository standardRepository;

    @Autowired
    private StandardEvaluationService evaluationService;

    @Autowired
    private MarkCalculatorService markCalculatorService;

    @Autowired
    private ControlRepository controlRepository;

    @Autowired
    private ControlResultRepository controlResultRepository;

    @Autowired
    private InspectorRepository inspectorRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CadetRepository cadetRepository;

    @Autowired
    private ControlSummaryRepository controlSummaryRepository;

    @Autowired
    private ControlStandardRepository controlStandardRepository;

    // ============= ДЛЯ ПРЕПОДАВАТЕЛЯ =============

    public Page<ControlDto> getGroupControls(
            UserDetails userDetails,
            Long groupId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String type,
            Pageable pageable) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("ID группы должен быть положительным числом");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
            throw new RuntimeException("Нет доступа к этой группе");
        }

        Page<Control> controls = getFilteredControls(groupId, dateFrom, dateTo, type, pageable);
        return controls.map(this::convertToDto);
    }

    public ControlDto getControlById(Long controlId, UserDetails userDetails) {
        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (controlId == null || controlId <= 0) {
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> new RuntimeException("Контроль не найден"));

        if (!control.getGroup().getUniversity().getId().equals(teacher.getUniversity().getId())) {
            throw new RuntimeException("Нет доступа к этому контролю");
        }

        return convertToDto(control);
    }

    // ============= ДЛЯ КУРСАНТА =============

    public Page<ControlDto> getCadetControls(
            UserDetails userDetails,
            LocalDate dateFrom,
            LocalDate dateTo,
            String type,
            Pageable pageable) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        Long groupId = cadet.getGroupId();

        List<Control> allControls = controlRepository.findByGroupId(groupId);

        List<Control> filteredControls = allControls.stream()
                .filter(c -> filterByDate(c, dateFrom, dateTo))
                .filter(c -> filterByType(c, type))
                .collect(Collectors.toList());

        filteredControls.sort((c1, c2) -> c2.getDate().compareTo(c1.getDate()));

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredControls.size());

        List<ControlDto> pageContent = filteredControls.subList(start, end)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, filteredControls.size());
    }

    public ControlDto getCadetControlById(UserDetails userDetails, Long controlId) {
        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (controlId == null || controlId <= 0) {
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> new RuntimeException("Контроль не найден"));

        if (!control.getGroup().getId().equals(cadet.getGroupId())) {
            throw new RuntimeException("Нет доступа к этому контролю");
        }

        return convertToDto(control);
    }

    // ============= ВСПОМОГАТЕЛЬНЫЕ =============

    private Page<Control> getFilteredControls(
            Long groupId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String type,
            Pageable pageable) {

        if (type != null && dateFrom != null && dateTo != null) {
            return controlRepository.findByGroupIdAndTypeAndDateBetweenOrderByDateDesc(
                    groupId, type, dateFrom, dateTo, pageable);
        } else if (type != null) {
            return controlRepository.findByGroupIdAndTypeOrderByDateDesc(
                    groupId, type, pageable);
        } else if (dateFrom != null && dateTo != null) {
            return controlRepository.findByGroupIdAndDateBetweenOrderByDateDesc(
                    groupId, dateFrom, dateTo, pageable);
        } else {
            return controlRepository.findByGroupIdOrderByDateDesc(groupId, pageable);
        }
    }

    private boolean filterByDate(Control control, LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null && dateTo == null) return true;
        if (dateFrom != null && control.getDate().isBefore(dateFrom)) return false;
        if (dateTo != null && control.getDate().isAfter(dateTo)) return false;
        return true;
    }

    private boolean filterByType(Control control, String type) {
        if (type == null || type.isEmpty()) return true;
        return control.getType().equalsIgnoreCase(type);
    }

    private List<InspectorDto> getInspectorsForControl(Long controlId) {
        return inspectorRepository.findByControlId(controlId)
                .stream()
                .map(this::convertInspectorToDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить нормативы из номеров
     */
    private List<Standard> getStandardsFromNumbers(List<Short> numbers) {
        return numbers.stream()
                .map(num -> standardRepository.findFirstByNumber(num).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Получить результаты контроля для курсанта
     */
    public Page<ControlResultDto> getControlResultsForCadet(
            UserDetails userDetails,
            Long controlId,
            Pageable pageable) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (controlId == null || controlId <= 0) {
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> new RuntimeException("Контроль не найден"));

        if (!control.getGroup().getId().equals(cadet.getGroupId())) {
            throw new RuntimeException("Нет доступа к этому контролю");
        }

        Page<ControlResult> results = controlResultRepository.findByControlId(controlId, pageable);
        return results.map(this::convertToResultDto);
    }

    private ControlResultDto convertToResultDto(ControlResult result) {
        String cadetName = result.getCadet().getUser().getLastName() + " "
                + result.getCadet().getUser().getFirstName();

        return new ControlResultDto(
                result.getId(),
                result.getCadet().getUserId(),
                cadetName,
                result.getCadet().getMilitaryRank(),
                result.getStatus(),
                result.getStandard().getName(),
                result.getStandard().getNumber().intValue(),
                result.getMark()
        );
    }

    private InspectorDto convertInspectorToDto(Inspector inspector) {
        return new InspectorDto(
                inspector.getId(),
                inspector.getFullName(),
                inspector.getExternal(),
                inspector.getTeacher() != null ? inspector.getTeacher().getUserId() : null
        );
    }

    private ControlDto convertToDto(Control control) {
        Long studentsCount = controlResultRepository.countByControlId(control.getId());
        Long passedCount = controlResultRepository.countPassedByControlId(control.getId());
        Double averageMark = controlResultRepository.averageMarkByControlId(control.getId());

        List<InspectorDto> inspectors = getInspectorsForControl(control.getId());

        List<Short> numbers = controlStandardRepository.findStandardNumbersByControlId(control.getId());
        List<Standard> standards = getStandardsFromNumbers(numbers);

        List<StandardDto> standardDtos = standards.stream()
                .map(s -> new StandardDto(
                        s.getId(),
                        s.getNumber(),
                        s.getName(),
                        s.getMeasurementUnit().getCode(),
                        s.getCourse(),
                        s.getGrade(),
                        s.getTimeValue(),
                        s.getIntValue(),
                        s.getWeightCategory()
                ))
                .collect(Collectors.toList());

        return new ControlDto(
                control.getId(),
                control.getType(),
                control.getDate(),
                control.getGroup().getNumber(),
                control.getGroup().getId(),
                studentsCount != null ? studentsCount.intValue() : 0,
                passedCount != null ? passedCount.intValue() : 0,
                averageMark != null ? averageMark : 0.0,
                inspectors,
                inspectors.size(),
                standardDtos
        );
    }

    /**
     * Получить полные результаты контроля
     */
    public List<ControlSummaryDto> getControlFullResults(Long controlId, UserDetails userDetails) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (controlId == null || controlId <= 0) {
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CADET"))) {
            Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Курсант не найден"));
            Control control = controlRepository.findById(controlId)
                    .orElseThrow(() -> new RuntimeException("Контроль не найден"));
            if (!control.getGroup().getId().equals(cadet.getGroupId())) {
                throw new RuntimeException("Нет доступа к этому контролю");
            }
        } else {
            Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));
            Control control = controlRepository.findById(controlId)
                    .orElseThrow(() -> new RuntimeException("Контроль не найден"));

            if (!control.getGroup().getUniversity().getId().equals(teacher.getUniversity().getId())) {
                throw new RuntimeException("Нет доступа к этому контролю");
            }
        }

        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> new RuntimeException("Контроль не найден"));

        List<Cadet> allCadets = cadetRepository.findByGroupId(control.getGroup().getId());

        List<ControlResult> results = controlResultRepository.findByControlId(controlId);
        Map<Long, List<ControlResult>> resultsByCadet = results.stream()
                .collect(Collectors.groupingBy(r -> r.getCadet().getUserId()));

        List<ControlSummary> summaries = controlSummaryRepository.findByControlId(controlId);
        Map<Long, Short> finalMarks = summaries.stream()
                .collect(Collectors.toMap(
                        cs -> cs.getCadet().getUserId(),
                        ControlSummary::getFinalMark,
                        (existing, replacement) -> existing
                ));

        Map<Long, String> statusMap = new HashMap<>();
        for (ControlResult result : results) {
            statusMap.put(result.getCadet().getUserId(), result.getStatus());
        }

        List<ControlSummaryDto> result = new ArrayList<>();

        for (Cadet cadet : allCadets) {
            ControlSummaryDto dto = new ControlSummaryDto();
            dto.setCadetId(cadet.getUserId());
            dto.setFullName(cadet.getUser().getLastName() + " " + cadet.getUser().getFirstName());
            dto.setMilitaryRank(cadet.getMilitaryRank());
            dto.setPost(cadet.getPost());

            List<ControlResult> cadetResults = resultsByCadet.get(cadet.getUserId());

            if (cadetResults != null && !cadetResults.isEmpty()) {
                dto.setStatus(cadetResults.get(0).getStatus());

                List<StandardResultDto> standards = cadetResults.stream()
                        .map(r -> new StandardResultDto(
                                r.getStandard().getName(),
                                r.getStandard().getNumber().intValue(),
                                r.getMark()
                        ))
                        .collect(Collectors.toList());
                dto.setStandards(standards);

                dto.setFinalMark(finalMarks.get(cadet.getUserId()));
            } else {
                String status = statusMap.get(cadet.getUserId());
                dto.setStatus(status != null ? status : "Не явился");
                dto.setStandards(new ArrayList<>());
                dto.setFinalMark(null);
            }

            result.add(dto);
        }

        result.sort(Comparator.comparing(ControlSummaryDto::getFullName));

        return result;
    }

    /**
     * Отправка результатов по номерам нормативов
     */
    @Transactional
    public void submitRawResults(UserDetails userDetails, SubmitRawResultsRequest request) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getControlId() == null || request.getControlId() <= 0) {
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        if (request.getResults() == null || request.getResults().isEmpty()) {
            throw new IllegalArgumentException("Список результатов не может быть пустым");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Control control = controlRepository.findById(request.getControlId())
                .orElseThrow(() -> new RuntimeException("Контроль не найден"));

        if (!control.getGroup().getUniversity().getId().equals(teacher.getUniversity().getId())) {
            throw new RuntimeException("Нет доступа к этому контролю");
        }

        List<Short> expectedNumbers = controlStandardRepository.findStandardNumbersByControlId(control.getId());

        if (expectedNumbers.isEmpty()) {
            throw new RuntimeException("Для контроля не указаны нормативы");
        }

        // Список допустимых статусов
        List<String> validStatuses = Arrays.asList(
                "Присутствует", "Болен", "Командировка", "Гауптвахта"
        );

        List<ControlResult> allResults = new ArrayList<>();
        Map<Long, List<ControlResult>> resultsByCadet = new HashMap<>();
        Map<Long, Integer> coursesByCadet = new HashMap<>();

        for (CadetRawResultDto cadetRaw : request.getResults()) {

            if (cadetRaw.getCadetId() == null || cadetRaw.getCadetId() <= 0) {
                throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
            }

            Cadet cadet = cadetRepository.findById(cadetRaw.getCadetId())
                    .orElseThrow(() -> new RuntimeException("Курсант с ID " + cadetRaw.getCadetId() + " не найден"));

            // Проверка статуса
            if (cadetRaw.getStatus() != null && !validStatuses.contains(cadetRaw.getStatus())) {
                throw new IllegalArgumentException("Некорректный статус: " + cadetRaw.getStatus() +
                        ". Допустимые: " + validStatuses);
            }

            // Если нет результатов - создаем записи со статусом
            if (cadetRaw.getRawResults() == null || cadetRaw.getRawResults().isEmpty()) {
                for (Short number : expectedNumbers) {
                    Standard standard = standardRepository.findFirstByNumber(number)
                            .orElseThrow(() -> new RuntimeException("Норматив с номером " + number + " не найден"));

                    ControlResult statusResult = new ControlResult();
                    statusResult.setControl(control);
                    statusResult.setCadet(cadet);
                    statusResult.setStatus(cadetRaw.getStatus() != null ? cadetRaw.getStatus() : "Не явился");
                    statusResult.setStandard(standard);
                    statusResult.setMark(null);

                    ControlResult saved = controlResultRepository.save(statusResult);
                    allResults.add(saved);
                }
                continue;
            }

            coursesByCadet.put(cadet.getUserId(), cadet.getCourse().intValue());
            List<ControlResult> cadetResults = new ArrayList<>();
            Set<Short> submittedNumbers = new HashSet<>();

            for (StandardRawResultDto raw : cadetRaw.getRawResults()) {

                // Проверка номера норматива
                if (raw.getStandardNumber() == null || raw.getStandardNumber() <= 0 || raw.getStandardNumber() > 10000) {
                    throw new IllegalArgumentException("Номер норматива должен быть от 1 до 10000");
                }

                Standard standard = standardRepository.findFirstByNumber(raw.getStandardNumber())
                        .orElseThrow(() -> new RuntimeException(
                                "Норматив с номером " + raw.getStandardNumber() + " не найден"));

                if (!expectedNumbers.contains(raw.getStandardNumber())) {
                    throw new RuntimeException(
                            String.format("Норматив с номером %d не входит в список нормативов контроля %d",
                                    raw.getStandardNumber(), control.getId())
                    );
                }

                // Проверка на дубликаты
                if (!submittedNumbers.add(raw.getStandardNumber())) {
                    throw new RuntimeException("Обнаружен дубликат норматива с номером " + raw.getStandardNumber() +
                            " для курсанта " + cadet.getUserId());
                }

                // Проверка значений (одно из двух должно быть)
                if (raw.getTimeValue() == null && raw.getIntValue() == null) {
                    throw new IllegalArgumentException(
                            "Для норматива " + raw.getStandardNumber() + " должно быть указано либо время, либо количество"
                    );
                }

                Short mark = evaluationService.evaluateMark(
                        standard,
                        raw.getTimeValue(),
                        raw.getIntValue(),
                        cadet.getCourse().intValue()
                );

                ControlResult result = new ControlResult();
                result.setControl(control);
                result.setCadet(cadet);
                result.setStatus(cadetRaw.getStatus() != null ? cadetRaw.getStatus() : "Присутствует");
                result.setStandard(standard);
                result.setMark(mark);

                ControlResult saved = controlResultRepository.save(result);
                cadetResults.add(saved);
                allResults.add(saved);
            }

            // Проверка количества нормативов
            if (submittedNumbers.size() != expectedNumbers.size()) {
                throw new RuntimeException(
                        String.format("Для курсанта %d прислано %d нормативов, ожидалось %d",
                                cadet.getUserId(), submittedNumbers.size(), expectedNumbers.size())
                );
            }

            resultsByCadet.put(cadet.getUserId(), cadetResults);
        }

        Map<Long, Short> finalMarks = markCalculatorService.calculateFinalMarks(
                resultsByCadet, coursesByCadet);

        for (Map.Entry<Long, Short> entry : finalMarks.entrySet()) {
            Long cadetId = entry.getKey();
            Short finalMark = entry.getValue();

            Cadet cadet = cadetRepository.findById(cadetId)
                    .orElseThrow(() -> new RuntimeException("Курсант не найден"));

            ControlSummary summary = new ControlSummary();
            summary.setId(new ControlSummaryId(control.getId(), cadetId));
            summary.setControl(control);
            summary.setCadet(cadet);
            summary.setCadetMilitaryRank(cadet.getMilitaryRank());
            summary.setCadetPost(cadet.getPost());
            summary.setFinalMark(finalMark);

            controlSummaryRepository.save(summary);
        }
    }

    /**
     * Создание контроля с номерами нормативов
     */
    @Transactional
    public ControlDto createControl(UserDetails userDetails, CreateControlRequest request) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        // Проверка ID группы
        if (request.getGroupId() == null || request.getGroupId() <= 0) {
            throw new IllegalArgumentException("ID группы должен быть положительным числом");
        }

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
            throw new RuntimeException("Нет доступа к этой группе");
        }

        // Проверка типа контроля
        List<String> validTypes = Arrays.asList(
                "Контрольное занятие", "Зачет", "Экзамен", "Смотр-конкурс"
        );
        if (!validTypes.contains(request.getType())) {
            throw new IllegalArgumentException("Некорректный тип контроля. Допустимые: " + validTypes);
        }

        // Проверка даты
        if (request.getDate() == null) {
            throw new IllegalArgumentException("Дата контроля не может быть пустой");
        }

        // Проверка номеров нормативов
        if (request.getStandardNumbers() == null || request.getStandardNumbers().isEmpty()) {
            throw new IllegalArgumentException("Необходимо указать хотя бы один номер норматива");
        }

        for (Short number : request.getStandardNumbers()) {
            if (number == null || number <= 0 || number > 10000) {
                throw new IllegalArgumentException("Номер норматива должен быть от 1 до 10000");
            }
            if (!standardRepository.findFirstByNumber(number).isPresent()) {
                throw new RuntimeException("Норматив с номером " + number + " не найден");
            }
        }

        // Проверка инспекторов
        if (request.getInspectorIds() != null) {
            for (Long inspectorId : request.getInspectorIds()) {
                if (inspectorId == null || inspectorId <= 0) {
                    throw new IllegalArgumentException("ID инспектора должен быть положительным числом");
                }
                if (!teacherRepository.existsById(inspectorId)) {
                    throw new RuntimeException("Инспектор с ID " + inspectorId + " не найден");
                }
            }
        }

        Control control = new Control();
        control.setType(request.getType());
        control.setGroup(group);
        control.setDate(request.getDate());
        control.setCreatedBy(teacher);

        Control savedControl = controlRepository.save(control);

        for (Short number : request.getStandardNumbers()) {
            ControlStandard controlStandard = new ControlStandard(savedControl, number);
            controlStandardRepository.save(controlStandard);
        }

        if (request.getInspectorIds() != null) {
            for (Long inspectorId : request.getInspectorIds()) {
                Teacher inspector = teacherRepository.findById(inspectorId)
                        .orElseThrow(() -> new RuntimeException("Инспектор не найден"));

                Inspector inspectorEntity = new Inspector();
                inspectorEntity.setControl(savedControl);
                inspectorEntity.setTeacher(inspector);
                inspectorEntity.setFirstName(inspector.getUser().getFirstName());
                inspectorEntity.setLastName(inspector.getUser().getLastName());
                inspectorEntity.setPatronymic(inspector.getUser().getPatronymic());
                inspectorEntity.setExternal(false);

                inspectorRepository.save(inspectorEntity);
            }
        }

        return convertToDto(savedControl);
    }

    /**
     * Редактирование результатов контроля
     */
    @Transactional
    public void updateControlResults(UserDetails userDetails, UpdateControlResultsRequest request) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getControlId() == null || request.getControlId() <= 0) {
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        if (request.getUpdates() == null || request.getUpdates().isEmpty()) {
            throw new IllegalArgumentException("Список обновлений не может быть пустым");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Control control = controlRepository.findById(request.getControlId())
                .orElseThrow(() -> new RuntimeException("Контроль не найден"));

        if (!control.getGroup().getUniversity().getId().equals(teacher.getUniversity().getId())) {
            throw new RuntimeException("Нет доступа к этому контролю");
        }

        Map<Long, List<ControlResult>> updatedResultsByCadet = new HashMap<>();
        Map<Long, Integer> coursesByCadet = new HashMap<>();

        for (UpdateControlResultRequest update : request.getUpdates()) {

            // Проверка входных данных обновления
            if (update.getCadetId() == null || update.getCadetId() <= 0) {
                throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
            }

            if (update.getStandardNumber() == null || update.getStandardNumber() <= 0 || update.getStandardNumber() > 10000) {
                throw new IllegalArgumentException("Номер норматива должен быть от 1 до 10000");
            }

            Standard standard = standardRepository.findFirstByNumber(update.getStandardNumber())
                    .orElseThrow(() -> new RuntimeException(
                            "Норматив с номером " + update.getStandardNumber() + " не найден"));

            ControlResult result = controlResultRepository
                    .findByControlIdAndCadet_UserIdAndStandardId(
                            request.getControlId(),
                            update.getCadetId(),
                            standard.getId()
                    ).orElseThrow(() -> new RuntimeException(
                            String.format("Результат для курсанта %d и норматива %d не найден",
                                    update.getCadetId(), update.getStandardNumber())
                    ));

            Cadet cadet = result.getCadet();

            // Проверка статуса
            if (update.getStatus() != null) {
                List<String> validStatuses = Arrays.asList(
                        "Присутствует", "Болен", "Командировка", "Гауптвахта"
                );
                if (!validStatuses.contains(update.getStatus())) {
                    throw new IllegalArgumentException("Некорректный статус: " + update.getStatus());
                }
                result.setStatus(update.getStatus());
            }

            if (update.getTimeValue() != null || update.getIntValue() != null) {
                // Проверка что одно из значений передано
                if (update.getTimeValue() == null && update.getIntValue() == null) {
                    throw new IllegalArgumentException(
                            "Должно быть указано либо время, либо количество"
                    );
                }

                BigDecimal timeValue = update.getTimeValue() != null ? update.getTimeValue() : null;
                Integer intValue = update.getIntValue() != null ? update.getIntValue() : null;

                Short newMark = evaluationService.evaluateMark(
                        standard,
                        timeValue,
                        intValue,
                        cadet.getCourse().intValue()
                );

                result.setMark(newMark);
            }

            controlResultRepository.save(result);

            // Собираем все результаты этого курсанта
            List<ControlResult> cadetResults = controlResultRepository
                    .findByControlIdAndCadet_UserId(control.getId(), cadet.getUserId());

            // Только те, у которых есть оценка (присутствуют)
            List<ControlResult> presentResults = cadetResults.stream()
                    .filter(r -> r.getMark() != null)
                    .collect(Collectors.toList());

            if (!presentResults.isEmpty()) {
                updatedResultsByCadet.put(cadet.getUserId(), presentResults);
                coursesByCadet.put(cadet.getUserId(), cadet.getCourse().intValue());
            }
        }

        // Пересчитываем итоговые оценки ТОЛЬКО для присутствующих
        if (!updatedResultsByCadet.isEmpty()) {
            Map<Long, Short> finalMarks = markCalculatorService.calculateFinalMarks(
                    updatedResultsByCadet, coursesByCadet);

            for (Map.Entry<Long, Short> entry : finalMarks.entrySet()) {
                Long cadetId = entry.getKey();
                Short finalMark = entry.getValue();

                // Пытаемся найти существующую итоговую оценку
                Optional<ControlSummary> existingSummary = controlSummaryRepository
                        .findByControlIdAndCadetId(control.getId(), cadetId);

                if (existingSummary.isPresent()) {
                    // Обновляем существующую
                    ControlSummary summary = existingSummary.get();
                    summary.setFinalMark(finalMark);
                    controlSummaryRepository.save(summary);
                } else {
                    // Создаем новую, если ее нет
                    Cadet cadet = cadetRepository.findById(cadetId)
                            .orElseThrow(() -> new RuntimeException("Курсант не найден"));

                    ControlSummary summary = new ControlSummary();
                    summary.setId(new ControlSummaryId(control.getId(), cadetId));
                    summary.setControl(control);
                    summary.setCadet(cadet);
                    summary.setCadetMilitaryRank(cadet.getMilitaryRank());
                    summary.setCadetPost(cadet.getPost());
                    summary.setFinalMark(finalMark);

                    controlSummaryRepository.save(summary);
                }
            }
        }
    }
}