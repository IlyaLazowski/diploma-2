package com.fakel.service;

import com.fakel.dto.*;
import com.fakel.model.*;
import com.fakel.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ControlService.class);

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

        log.info("Получение контролей группы: groupId={}, user={}, dateFrom={}, dateTo={}, type={}, page={}, size={}",
                groupId, userDetails != null ? userDetails.getUsername() : null, dateFrom, dateTo, type,
                pageable.getPageNumber(), pageable.getPageSize());

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения контролей с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (groupId == null || groupId <= 0) {
            log.warn("Некорректный ID группы: {}", groupId);
            throw new IllegalArgumentException("ID группы должен быть положительным числом");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            log.warn("Некорректный диапазон дат: dateFrom={} > dateTo={}", dateFrom, dateTo);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    log.warn("Группа не найдена: {}", groupId);
                    return new RuntimeException("Группа не найдена");
                });

        if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
            log.warn("Нет доступа к группе {} для преподавателя {}", groupId, teacher.getUserId());
            throw new RuntimeException("Нет доступа к этой группе");
        }

        Page<Control> controls = getFilteredControls(groupId, dateFrom, dateTo, type, pageable);
        log.info("Найдено {} контролей для группы {}", controls.getTotalElements(), groupId);

        return controls.map(this::convertToDto);
    }

    public ControlDto getControlById(Long controlId, UserDetails userDetails) {
        log.info("Получение контроля по ID: {}, пользователь: {}", controlId,
                userDetails != null ? userDetails.getUsername() : null);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения контроля с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (controlId == null || controlId <= 0) {
            log.warn("Некорректный ID контроля: {}", controlId);
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> {
                    log.warn("Контроль не найден: {}", controlId);
                    return new RuntimeException("Контроль не найден");
                });

        if (!control.getGroup().getUniversity().getId().equals(teacher.getUniversity().getId())) {
            log.warn("Нет доступа к контролю {} для преподавателя {}", controlId, teacher.getUserId());
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

        log.info("Получение контролей курсанта: user={}, dateFrom={}, dateTo={}, type={}, page={}, size={}",
                userDetails != null ? userDetails.getUsername() : null, dateFrom, dateTo, type,
                pageable.getPageNumber(), pageable.getPageSize());

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения контролей с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            log.warn("Некорректный диапазон дат: dateFrom={} > dateTo={}", dateFrom, dateTo);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Курсант не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Курсант не найден");
                });

        Long groupId = cadet.getGroupId();
        log.debug("Курсант {} из группы {}", cadet.getUserId(), groupId);

        List<Control> allControls = controlRepository.findByGroupId(groupId);
        List<Control> filteredControls = allControls.stream()
                .filter(c -> filterByDate(c, dateFrom, dateTo))
                .filter(c -> filterByType(c, type))
                .sorted((c1, c2) -> c2.getDate().compareTo(c1.getDate()))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredControls.size());

        List<ControlDto> pageContent = filteredControls.subList(start, end)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.info("Найдено {} контролей для курсанта {}", filteredControls.size(), cadet.getUserId());

        return new PageImpl<>(pageContent, pageable, filteredControls.size());
    }

    public ControlDto getCadetControlById(UserDetails userDetails, Long controlId) {
        log.info("Получение контроля курсантом: controlId={}, user={}", controlId,
                userDetails != null ? userDetails.getUsername() : null);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения контроля с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (controlId == null || controlId <= 0) {
            log.warn("Некорректный ID контроля: {}", controlId);
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Курсант не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Курсант не найден");
                });

        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> {
                    log.warn("Контроль не найден: {}", controlId);
                    return new RuntimeException("Контроль не найден");
                });

        if (!control.getGroup().getId().equals(cadet.getGroupId())) {
            log.warn("Нет доступа к контролю {} для курсанта {}", controlId, cadet.getUserId());
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
     * Получить нормативы для контроля - ВСЕ варианты для каждого номера
     */
    private List<StandardDto> getStandardsForControl(Long controlId) {
        List<Short> numbers = controlStandardRepository.findStandardNumbersByControlId(controlId);
        List<StandardDto> standardDtos = new ArrayList<>();

        for (Short number : numbers) {
            // Загружаем ВСЕ нормативы с этим номером
            List<Standard> standards = standardRepository.findByNumber(number);
            for (Standard standard : standards) {
                standardDtos.add(convertStandardToDto(standard));
            }
        }

        return standardDtos;
    }

    private List<Standard> getStandardsFromNumbers(List<Short> numbers) {
        return numbers.stream()
                .flatMap(num -> standardRepository.findByNumber(num).stream())
                .collect(Collectors.toList());
    }

    /**
     * Получить результаты контроля для курсанта
     */
    public Page<ControlResultDto> getControlResultsForCadet(
            UserDetails userDetails,
            Long controlId,
            Pageable pageable) {

        log.info("Получение результатов контроля для курсанта: controlId={}, user={}",
                controlId, userDetails != null ? userDetails.getUsername() : null);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения результатов с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (controlId == null || controlId <= 0) {
            log.warn("Некорректный ID контроля: {}", controlId);
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Курсант не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Курсант не найден");
                });

        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> {
                    log.warn("Контроль не найден: {}", controlId);
                    return new RuntimeException("Контроль не найден");
                });

        if (!control.getGroup().getId().equals(cadet.getGroupId())) {
            log.warn("Нет доступа к результатам контроля {} для курсанта {}", controlId, cadet.getUserId());
            throw new RuntimeException("Нет доступа к этому контролю");
        }

        Page<ControlResult> results = controlResultRepository.findByControlId(controlId, pageable);
        log.info("Найдено {} результатов для контроля {}", results.getTotalElements(), controlId);

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

    private StandardDto convertStandardToDto(Standard standard) {
        if (standard == null) return null;

        return new StandardDto(
                standard.getId(),
                standard.getNumber(),
                standard.getName(),
                standard.getMeasurementUnit() != null ? standard.getMeasurementUnit().getCode() : null,
                standard.getCourse(),
                standard.getGrade(),
                standard.getTimeValue(),
                standard.getIntValue(),
                standard.getWeightCategory()
        );
    }

    private ControlDto convertToDto(Control control) {
        log.trace("Конвертация контроля {} в DTO", control.getId());

        Long studentsCount = controlResultRepository.countByControlId(control.getId());
        Long passedCount = controlResultRepository.countPassedByControlId(control.getId());
        Double averageMark = controlResultRepository.averageMarkByControlId(control.getId());

        List<InspectorDto> inspectors = getInspectorsForControl(control.getId());

        // ✅ ИСПРАВЛЕНО: загружаем ВСЕ нормативы для каждого номера
        List<StandardDto> standardDtos = getStandardsForControl(control.getId());

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

        log.info("Получение полных результатов контроля: controlId={}, user={}",
                controlId, userDetails != null ? userDetails.getUsername() : null);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения результатов с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (controlId == null || controlId <= 0) {
            log.warn("Некорректный ID контроля: {}", controlId);
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        // Проверка доступа
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
        log.info("Сформированы результаты для {} курсантов", result.size());
        return result;
    }

    /**
     * Отправка результатов по номерам нормативов
     */
    @Transactional
    public void submitRawResults(UserDetails userDetails, SubmitRawResultsRequest request) {

        log.info("Отправка результатов контроля: controlId={}, user={}",
                request != null ? request.getControlId() : null,
                userDetails != null ? userDetails.getUsername() : null);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка отправки результатов с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            log.warn("Попытка отправки результатов с null request");
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getControlId() == null || request.getControlId() <= 0) {
            log.warn("Некорректный ID контроля: {}", request.getControlId());
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        if (request.getResults() == null || request.getResults().isEmpty()) {
            log.warn("Пустой список результатов для контроля {}", request.getControlId());
            throw new IllegalArgumentException("Список результатов не может быть пустым");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Control control = controlRepository.findById(request.getControlId())
                .orElseThrow(() -> new RuntimeException("Контроль не найден"));

        if (!control.getGroup().getUniversity().getId().equals(teacher.getUniversity().getId())) {
            log.warn("Нет доступа к контролю {} для преподавателя {}", request.getControlId(), teacher.getUserId());
            throw new RuntimeException("Нет доступа к этому контролю");
        }

        List<Short> expectedNumbers = controlStandardRepository.findStandardNumbersByControlId(control.getId());

        if (expectedNumbers.isEmpty()) {
            log.warn("Для контроля {} не указаны нормативы", control.getId());
            throw new RuntimeException("Для контроля не указаны нормативы");
        }

        log.debug("Ожидаемые нормативы: {}", expectedNumbers);

        List<String> validStatuses = Arrays.asList(
                "Присутствует", "Болен", "Командировка", "Гауптвахта"
        );

        List<ControlResult> allResults = new ArrayList<>();
        Map<Long, List<ControlResult>> resultsByCadet = new HashMap<>();
        Map<Long, Integer> coursesByCadet = new HashMap<>();
        int processedCadets = 0;

        for (CadetRawResultDto cadetRaw : request.getResults()) {
            processedCadets++;

            if (cadetRaw.getCadetId() == null || cadetRaw.getCadetId() <= 0) {
                log.warn("Некорректный ID курсанта в запросе: {}", cadetRaw.getCadetId());
                throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
            }

            Cadet cadet = cadetRepository.findById(cadetRaw.getCadetId())
                    .orElseThrow(() -> new RuntimeException("Курсант с ID " + cadetRaw.getCadetId() + " не найден"));

            log.debug("Обработка курсанта {} ({}), статус: {}",
                    cadet.getUserId(), cadet.getUser().getLastName(), cadetRaw.getStatus());

            if (cadetRaw.getStatus() != null && !validStatuses.contains(cadetRaw.getStatus())) {
                log.warn("Некорректный статус для курсанта {}: {}", cadet.getUserId(), cadetRaw.getStatus());
                throw new IllegalArgumentException("Некорректный статус: " + cadetRaw.getStatus() +
                        ". Допустимые: " + validStatuses);
            }

            // Если нет результатов - создаем записи со статусом
            if (cadetRaw.getRawResults() == null || cadetRaw.getRawResults().isEmpty()) {
                log.debug("Курсант {} без результатов, создаем записи со статусом", cadet.getUserId());
                for (Short number : expectedNumbers) {
                    List<Standard> standards = standardRepository.findByNumber(number);
                    if (standards.isEmpty()) {
                        log.warn("Норматив с номером {} не найден", number);
                        throw new RuntimeException("Норматив с номером " + number + " не найден");
                    }
                    // Используем первый попавшийся норматив (для сохранения в result)
                    Standard standard = standards.get(0);

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

                if (raw.getStandardNumber() == null || raw.getStandardNumber() <= 0 || raw.getStandardNumber() > 10000) {
                    log.warn("Некорректный номер норматива: {}", raw.getStandardNumber());
                    throw new IllegalArgumentException("Номер норматива должен быть от 1 до 10000");
                }

                // Проверяем, что норматив входит в список
                if (!expectedNumbers.contains(raw.getStandardNumber())) {
                    log.warn("Норматив {} не входит в список нормативов контроля {}",
                            raw.getStandardNumber(), control.getId());
                    throw new RuntimeException(
                            String.format("Норматив с номером %d не входит в список нормативов контроля %d",
                                    raw.getStandardNumber(), control.getId())
                    );
                }

                if (!submittedNumbers.add(raw.getStandardNumber())) {
                    log.warn("Дубликат норматива {} для курсанта {}", raw.getStandardNumber(), cadet.getUserId());
                    throw new RuntimeException("Обнаружен дубликат норматива с номером " + raw.getStandardNumber() +
                            " для курсанта " + cadet.getUserId());
                }

                if (raw.getTimeValue() == null && raw.getIntValue() == null) {
                    log.warn("Для норматива {} не указаны значения", raw.getStandardNumber());
                    throw new IllegalArgumentException(
                            "Для норматива " + raw.getStandardNumber() + " должно быть указано либо время, либо количество"
                    );
                }

                // Получаем ВСЕ нормативы для сравнения
                List<Standard> standards = standardRepository.findByNumberAndCourseOrderByGrade(
                        raw.getStandardNumber(), cadet.getCourse().shortValue());

                if (standards.isEmpty()) {
                    log.warn("Не найдены нормативы для номера {} и курса {}", raw.getStandardNumber(), cadet.getCourse());
                    throw new RuntimeException(
                            "Нормативы для номера " + raw.getStandardNumber() + " и курса " + cadet.getCourse() + " не найдены"
                    );
                }

                // Находим подходящий норматив и получаем оценку
                Short mark = evaluationService.evaluateMarkWithStandards(
                        raw.getStandardNumber(),
                        cadet.getCourse().intValue(),
                        raw.getTimeValue(),
                        raw.getIntValue(),
                        standards
                );

                log.debug("Норматив {} оценен в {} баллов", raw.getStandardNumber(), mark);

                // Используем первый норматив для сохранения (как представление)
                Standard standardForSave = standards.get(0);

                ControlResult result = new ControlResult();
                result.setControl(control);
                result.setCadet(cadet);
                result.setStatus(cadetRaw.getStatus() != null ? cadetRaw.getStatus() : "Присутствует");
                result.setStandard(standardForSave);
                result.setMark(mark);

                ControlResult saved = controlResultRepository.save(result);
                cadetResults.add(saved);
                allResults.add(saved);
            }

            if (submittedNumbers.size() != expectedNumbers.size()) {
                log.warn("Для курсанта {} прислано {} нормативов, ожидалось {}",
                        cadet.getUserId(), submittedNumbers.size(), expectedNumbers.size());
                throw new RuntimeException(
                        String.format("Для курсанта %d прислано %d нормативов, ожидалось %d",
                                cadet.getUserId(), submittedNumbers.size(), expectedNumbers.size())
                );
            }

            resultsByCadet.put(cadet.getUserId(), cadetResults);
        }

        log.info("Обработано {} курсантов, вычисление итоговых оценок...", processedCadets);

        Map<Long, Short> finalMarks = markCalculatorService.calculateFinalMarks(
                resultsByCadet, coursesByCadet);

        log.debug("Получено {} итоговых оценок", finalMarks.size());

        for (Map.Entry<Long, Short> entry : finalMarks.entrySet()) {
            Long cadetId = entry.getKey();
            Short finalMark = entry.getValue();

            Cadet cadet = cadetRepository.findById(cadetId)
                    .orElseThrow(() -> new RuntimeException("Курсант не найден"));

            Optional<ControlSummary> existingSummary = controlSummaryRepository
                    .findByControlIdAndCadetId(control.getId(), cadetId);

            if (existingSummary.isPresent()) {
                ControlSummary summary = existingSummary.get();
                summary.setFinalMark(finalMark);
                controlSummaryRepository.save(summary);
                log.debug("Обновлена итоговая оценка {} для курсанта {}", finalMark, cadetId);
            } else {
                ControlSummary summary = new ControlSummary();
                summary.setId(new ControlSummaryId(control.getId(), cadetId));
                summary.setControl(control);
                summary.setCadet(cadet);
                summary.setCadetMilitaryRank(cadet.getMilitaryRank());
                summary.setCadetPost(cadet.getPost());
                summary.setFinalMark(finalMark);
                controlSummaryRepository.save(summary);
                log.debug("Создана итоговая оценка {} для курсанта {}", finalMark, cadetId);
            }
        }

        log.info("Результаты контроля {} успешно сохранены", control.getId());
    }

    /**
     * Создание контроля с номерами нормативов
     */
    @Transactional
    public ControlDto createControl(UserDetails userDetails, CreateControlRequest request) {

        log.info("Создание контроля: user={}, группа={}, тип={}",
                userDetails != null ? userDetails.getUsername() : null,
                request != null ? request.getGroupId() : null,
                request != null ? request.getType() : null);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка создания контроля с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            log.warn("Попытка создания контроля с null request");
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        if (request.getGroupId() == null || request.getGroupId() <= 0) {
            log.warn("Некорректный ID группы: {}", request.getGroupId());
            throw new IllegalArgumentException("ID группы должен быть положительным числом");
        }

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
            log.warn("Нет доступа к группе {} для преподавателя {}", request.getGroupId(), teacher.getUserId());
            throw new RuntimeException("Нет доступа к этой группе");
        }

        List<String> validTypes = Arrays.asList(
                "Контрольное занятие", "Зачет", "Экзамен", "Смотр-конкурс"
        );
        if (!validTypes.contains(request.getType())) {
            log.warn("Некорректный тип контроля: {}", request.getType());
            throw new IllegalArgumentException("Некорректный тип контроля. Допустимые: " + validTypes);
        }

        if (request.getDate() == null) {
            log.warn("Не указана дата контроля");
            throw new IllegalArgumentException("Дата контроля не может быть пустой");
        }

        if (request.getStandardNumbers() == null || request.getStandardNumbers().isEmpty()) {
            log.warn("Не указаны нормативы для контроля");
            throw new IllegalArgumentException("Необходимо указать хотя бы один номер норматива");
        }

        log.debug("Нормативы: {}", request.getStandardNumbers());

        for (Short number : request.getStandardNumbers()) {
            if (number == null || number <= 0 || number > 10000) {
                log.warn("Некорректный номер норматива: {}", number);
                throw new IllegalArgumentException("Номер норматива должен быть от 1 до 10000");
            }
            List<Standard> standards = standardRepository.findByNumber(number);
            if (standards.isEmpty()) {
                log.warn("Норматив с номером {} не найден", number);
                throw new RuntimeException("Норматив с номером " + number + " не найден");
            }
        }

        Control control = new Control();
        control.setType(request.getType());
        control.setGroup(group);
        control.setDate(request.getDate());
        control.setCreatedBy(teacher);

        Control savedControl = controlRepository.save(control);
        log.info("Контроль создан с ID: {}", savedControl.getId());

        for (Short number : request.getStandardNumbers()) {
            ControlStandard controlStandard = new ControlStandard(savedControl, number);
            controlStandardRepository.save(controlStandard);
            log.trace("Добавлен норматив {} к контролю {}", number, savedControl.getId());
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
                log.debug("Добавлен инспектор {} к контролю {}", inspectorId, savedControl.getId());
            }
        }

        log.info("Контроль {} успешно создан", savedControl.getId());
        return convertToDto(savedControl);
    }

    /**
     * Редактирование результатов контроля
     */
    @Transactional
    public void updateControlResults(UserDetails userDetails, UpdateControlResultsRequest request) {

        log.info("Обновление результатов контроля: controlId={}, user={}",
                request != null ? request.getControlId() : null,
                userDetails != null ? userDetails.getUsername() : null);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка обновления результатов с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            log.warn("Попытка обновления результатов с null request");
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        if (request.getControlId() == null || request.getControlId() <= 0) {
            log.warn("Некорректный ID контроля: {}", request.getControlId());
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        if (request.getUpdates() == null || request.getUpdates().isEmpty()) {
            log.warn("Пустой список обновлений для контроля {}", request.getControlId());
            throw new IllegalArgumentException("Список обновлений не может быть пустым");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Control control = controlRepository.findById(request.getControlId())
                .orElseThrow(() -> new RuntimeException("Контроль не найден"));

        if (!control.getGroup().getUniversity().getId().equals(teacher.getUniversity().getId())) {
            log.warn("Нет доступа к контролю {} для преподавателя {}", request.getControlId(), teacher.getUserId());
            throw new RuntimeException("Нет доступа к этому контролю");
        }

        Map<Long, List<ControlResult>> updatedResultsByCadet = new HashMap<>();
        Map<Long, Integer> coursesByCadet = new HashMap<>();
        int updatedCount = 0;

        for (UpdateControlResultRequest update : request.getUpdates()) {
            updatedCount++;

            if (update.getCadetId() == null || update.getCadetId() <= 0) {
                log.warn("Некорректный ID курсанта в обновлении #{}", updatedCount);
                throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
            }

            if (update.getStandardNumber() == null || update.getStandardNumber() <= 0 || update.getStandardNumber() > 10000) {
                log.warn("Некорректный номер норматива в обновлении #{}: {}", updatedCount, update.getStandardNumber());
                throw new IllegalArgumentException("Номер норматива должен быть от 1 до 10000");
            }

            log.debug("Обновление #{}: курсант {}, норматив {}", updatedCount, update.getCadetId(), update.getStandardNumber());

            Cadet cadet = cadetRepository.findById(update.getCadetId())
                    .orElseThrow(() -> new RuntimeException("Курсант с ID " + update.getCadetId() + " не найден"));

            List<Standard> standards = standardRepository.findByNumber(update.getStandardNumber());
            if (standards.isEmpty()) {
                log.warn("Норматив с номером {} не найден", update.getStandardNumber());
                throw new RuntimeException("Норматив с номером " + update.getStandardNumber() + " не найден");
            }

            // Находим существующий результат
            ControlResult result = controlResultRepository
                    .findByControlIdAndCadet_UserIdAndStandardId(
                            request.getControlId(),
                            update.getCadetId(),
                            standards.get(0).getId()
                    ).orElseThrow(() -> {
                        log.warn("Результат для курсанта {} и норматива {} не найден",
                                update.getCadetId(), update.getStandardNumber());
                        return new RuntimeException(
                                String.format("Результат для курсанта %d и норматива %d не найден",
                                        update.getCadetId(), update.getStandardNumber())
                        );
                    });

            // Обновляем статус
            if (update.getStatus() != null) {
                List<String> validStatuses = Arrays.asList(
                        "Присутствует", "Болен", "Командировка", "Гауптвахта"
                );
                if (!validStatuses.contains(update.getStatus())) {
                    log.warn("Некорректный статус: {}", update.getStatus());
                    throw new IllegalArgumentException("Некорректный статус: " + update.getStatus());
                }
                result.setStatus(update.getStatus());
                log.debug("Статус обновлен на {}", update.getStatus());
            }

            // Обновляем оценку
            if (update.getTimeValue() != null || update.getIntValue() != null) {
                Short newMark = evaluationService.evaluateMarkWithStandards(
                        update.getStandardNumber(),
                        cadet.getCourse().intValue(),
                        update.getTimeValue(),
                        update.getIntValue(),
                        standards
                );

                result.setMark(newMark);
                log.debug("Оценка обновлена на {}", newMark);
            }

            controlResultRepository.save(result);

            // Собираем все результаты этого курсанта
            List<ControlResult> cadetResults = controlResultRepository
                    .findByControlIdAndCadet_UserId(control.getId(), cadet.getUserId());

            List<ControlResult> presentResults = cadetResults.stream()
                    .filter(r -> r.getMark() != null)
                    .collect(Collectors.toList());

            if (!presentResults.isEmpty()) {
                updatedResultsByCadet.put(cadet.getUserId(), presentResults);
                coursesByCadet.put(cadet.getUserId(), cadet.getCourse().intValue());
            }
        }

        // Пересчитываем итоговые оценки
        if (!updatedResultsByCadet.isEmpty()) {
            log.info("Пересчет итоговых оценок для {} курсантов", updatedResultsByCadet.size());

            Map<Long, Short> finalMarks = markCalculatorService.calculateFinalMarks(
                    updatedResultsByCadet, coursesByCadet);

            for (Map.Entry<Long, Short> entry : finalMarks.entrySet()) {
                Long cadetId = entry.getKey();
                Short finalMark = entry.getValue();

                Optional<ControlSummary> existingSummary = controlSummaryRepository
                        .findByControlIdAndCadetId(control.getId(), cadetId);

                if (existingSummary.isPresent()) {
                    ControlSummary summary = existingSummary.get();
                    summary.setFinalMark(finalMark);
                    controlSummaryRepository.save(summary);
                    log.debug("Обновлена итоговая оценка {} для курсанта {}", finalMark, cadetId);
                } else {
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
                    log.debug("Создана итоговая оценка {} для курсанта {}", finalMark, cadetId);
                }
            }
        }

        log.info("Обновление результатов контроля {} завершено (обработано {} записей)",
                control.getId(), request.getUpdates().size());
    }


    /**
     * Получить полные результаты контроля с расширенной информацией
     */
    public ControlFullDetailsDto getControlFullDetails(Long controlId, UserDetails userDetails) {

        log.info("Получение полных результатов контроля: controlId={}, user={}",
                controlId, userDetails != null ? userDetails.getUsername() : null);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения результатов с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (controlId == null || controlId <= 0) {
            log.warn("Некорректный ID контроля: {}", controlId);
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        // Проверка доступа
        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> new RuntimeException("Контроль не найден"));

        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CADET"))) {
            Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Курсант не найден"));
            if (!control.getGroup().getId().equals(cadet.getGroupId())) {
                throw new RuntimeException("Нет доступа к этому контролю");
            }
        } else {
            Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));
            if (!control.getGroup().getUniversity().getId().equals(teacher.getUniversity().getId())) {
                throw new RuntimeException("Нет доступа к этому контролю");
            }
        }

        // Создаем ответ
        ControlFullDetailsDto result = new ControlFullDetailsDto();

        // Заполняем информацию о контроле
        result.setControlId(control.getId());
        result.setControlType(control.getType());
        result.setControlDate(control.getDate());

        // Заполняем информацию о группе
        Group group = control.getGroup();
        result.setGroupId(group.getId());
        result.setGroupNumber(group.getNumber());
        result.setGroupFoundationDate(group.getFoundationDate());

        // Заполняем информацию об инспекторах
        List<Inspector> inspectors = inspectorRepository.findByControlId(controlId);
        List<ControlFullDetailsDto.InspectorInfoDto> inspectorDtos = inspectors.stream()
                .map(this::convertToInspectorInfoDto)
                .collect(Collectors.toList());
        result.setInspectors(inspectorDtos);

        // Получаем всех курсантов группы
        List<Cadet> allCadets = cadetRepository.findByGroupId(group.getId());

        // Получаем результаты
        List<ControlResult> results = controlResultRepository.findByControlId(controlId);
        Map<Long, List<ControlResult>> resultsByCadet = results.stream()
                .collect(Collectors.groupingBy(r -> r.getCadet().getUserId()));

        // Получаем итоговые оценки
        List<ControlSummary> summaries = controlSummaryRepository.findByControlId(controlId);
        Map<Long, Short> finalMarks = summaries.stream()
                .collect(Collectors.toMap(
                        cs -> cs.getCadet().getUserId(),
                        ControlSummary::getFinalMark,
                        (existing, replacement) -> existing
                ));

        // Формируем результаты по курсантам
        List<CadetFullResultDto> cadetResults = new ArrayList<>();
        ControlFullDetailsDto.ControlStatisticsDto statistics = new ControlFullDetailsDto.ControlStatisticsDto();

        // Инициализируем счетчики
        int presentCount = 0;
        int sickCount = 0;
        int dutyCount = 0;
        int guardhouseCount = 0;
        int excellentCount = 0;
        int goodCount = 0;
        int satisfactoryCount = 0;
        int unsatisfactoryCount = 0;
        double totalMarkSum = 0;
        int totalMarkCount = 0;

        for (Cadet cadet : allCadets) {
            CadetFullResultDto cadetDto = new CadetFullResultDto();
            cadetDto.setCadetId(cadet.getUserId());
            cadetDto.setFullName(cadet.getUser().getLastName() + " " +
                    cadet.getUser().getFirstName() +
                    (cadet.getUser().getPatronymic() != null ? " " + cadet.getUser().getPatronymic() : ""));
            cadetDto.setMilitaryRank(cadet.getMilitaryRank());
            cadetDto.setPost(cadet.getPost());
            cadetDto.setCourse(cadet.getCourse() != null ? cadet.getCourse().intValue() : null);

            List<ControlResult> cadetResultsList = resultsByCadet.get(cadet.getUserId());

            if (cadetResultsList != null && !cadetResultsList.isEmpty()) {
                String status = cadetResultsList.get(0).getStatus();
                cadetDto.setStatus(status);

                // Считаем статистику по статусам
                if ("Присутствует".equals(status)) presentCount++;
                else if ("Болен".equals(status)) sickCount++;
                else if ("Командировка".equals(status)) dutyCount++;
                else if ("Гауптвахта".equals(status)) guardhouseCount++;

                List<StandardResultDetailDto> standardDtos = cadetResultsList.stream()
                        .map(r -> {
                            String grade = convertMarkToGrade(r.getMark());
                            return new StandardResultDetailDto(
                                    r.getStandard().getNumber().intValue(),
                                    r.getStandard().getName(),
                                    r.getMark(),
                                    grade,
                                    r.getStandard().getMeasurementUnit() != null ?
                                            r.getStandard().getMeasurementUnit().getCode() : null
                            );
                        })
                        .collect(Collectors.toList());
                cadetDto.setStandards(standardDtos);

                Short finalMark = finalMarks.get(cadet.getUserId());
                cadetDto.setFinalMark(finalMark);
                cadetDto.setFinalGrade(convertMarkToFinalGrade(finalMark));

                // Считаем статистику по итоговым оценкам
                if (finalMark != null) {
                    totalMarkSum += finalMark;
                    totalMarkCount++;

                    if (finalMark >= 8 && finalMark <= 10) excellentCount++;
                    else if (finalMark >= 6 && finalMark <= 7) goodCount++;
                    else if (finalMark >= 4 && finalMark <= 5) satisfactoryCount++;
                    else if (finalMark >= 1 && finalMark <= 3) unsatisfactoryCount++;
                }
            } else {
                cadetDto.setStatus("Не явился");
                cadetDto.setStandards(new ArrayList<>());
                cadetDto.setFinalMark(null);
                cadetDto.setFinalGrade("Не сдавал");
                presentCount++; // Не явившиеся считаем как отсутствующие на сдаче
            }

            cadetResults.add(cadetDto);
        }

        // Сортируем по ФИО
        cadetResults.sort(Comparator.comparing(CadetFullResultDto::getFullName));
        result.setCadetResults(cadetResults);

        // Заполняем статистику
        statistics.setTotalCadets(allCadets.size());
        statistics.setPresentCount(presentCount);
        statistics.setSickCount(sickCount);
        statistics.setDutyCount(dutyCount);
        statistics.setGuardhouseCount(guardhouseCount);
        statistics.setAbsentCount(allCadets.size() - (presentCount + sickCount + dutyCount + guardhouseCount));
        statistics.setAverageMark(totalMarkCount > 0 ? Math.round(totalMarkSum / totalMarkCount * 10.0) / 10.0 : 0.0);
        statistics.setExcellentCount(excellentCount);
        statistics.setGoodCount(goodCount);
        statistics.setSatisfactoryCount(satisfactoryCount);
        statistics.setUnsatisfactoryCount(unsatisfactoryCount);

        result.setStatistics(statistics);

        log.info("Сформированы результаты для {} курсантов", cadetResults.size());
        return result;
    }

    // Вспомогательные методы
    private ControlFullDetailsDto.InspectorInfoDto convertToInspectorInfoDto(Inspector inspector) {
        String rank = null;
        if (inspector.getTeacher() != null && inspector.getTeacher().getMilitaryRank() != null) {
            rank = inspector.getTeacher().getMilitaryRank();
        }

        return new ControlFullDetailsDto.InspectorInfoDto(
                inspector.getId(),
                inspector.getFullName(),
                inspector.getExternal(),
                rank
        );
    }

    private String convertMarkToGrade(Short mark) {
        if (mark == null) return "Не оценено";
        switch (mark) {
            case 5: return "Отлично";
            case 4: return "Хорошо";
            case 3: return "Удовлетворительно";
            default: return "Неудовлетворительно";
        }
    }

    private String convertMarkToFinalGrade(Short mark) {
        if (mark == null) return "Не сдавал";
        if (mark >= 8 && mark <= 10) return "Отлично";
        if (mark >= 6 && mark <= 7) return "Хорошо";
        if (mark >= 4 && mark <= 5) return "Удовлетворительно";
        if (mark >= 1 && mark <= 3) return "Неудовлетворительно";
        return "Не определено";
    }
}