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

        // Проверка входных данных
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
        log.debug("Всего страниц: {}", controls.getTotalPages());

        return controls.map(this::convertToDto);
    }

    public ControlDto getControlById(Long controlId, UserDetails userDetails) {
        log.info("Получение контроля по ID: {}, пользователь: {}", controlId,
                userDetails != null ? userDetails.getUsername() : null);

        // Проверка входных данных
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

        log.debug("Контроль найден: тип={}, дата={}, группа={}",
                control.getType(), control.getDate(), control.getGroup().getNumber());

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

        // Проверка входных данных
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
        log.debug("Всего контролей в группе: {}", allControls.size());

        List<Control> filteredControls = allControls.stream()
                .filter(c -> filterByDate(c, dateFrom, dateTo))
                .filter(c -> filterByType(c, type))
                .collect(Collectors.toList());

        log.debug("После фильтрации осталось контролей: {}", filteredControls.size());

        filteredControls.sort((c1, c2) -> c2.getDate().compareTo(c1.getDate()));

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

        // Проверка входных данных
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

        log.debug("Контроль найден: тип={}, дата={}", control.getType(), control.getDate());

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
            log.debug("Поиск контролей по группе {}, типу {} и датам {} - {}", groupId, type, dateFrom, dateTo);
            return controlRepository.findByGroupIdAndTypeAndDateBetweenOrderByDateDesc(
                    groupId, type, dateFrom, dateTo, pageable);
        } else if (type != null) {
            log.debug("Поиск контролей по группе {} и типу {}", groupId, type);
            return controlRepository.findByGroupIdAndTypeOrderByDateDesc(
                    groupId, type, pageable);
        } else if (dateFrom != null && dateTo != null) {
            log.debug("Поиск контролей по группе {} и датам {} - {}", groupId, dateFrom, dateTo);
            return controlRepository.findByGroupIdAndDateBetweenOrderByDateDesc(
                    groupId, dateFrom, dateTo, pageable);
        } else {
            log.debug("Поиск всех контролей группы {}", groupId);
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

        log.info("Получение результатов контроля для курсанта: controlId={}, user={}",
                controlId, userDetails != null ? userDetails.getUsername() : null);

        // Проверка входных данных
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

    private ControlDto convertToDto(Control control) {
        log.trace("Конвертация контроля {} в DTO", control.getId());

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

        log.info("Получение полных результатов контроля: controlId={}, user={}",
                controlId, userDetails != null ? userDetails.getUsername() : null);

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения результатов с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (controlId == null || controlId <= 0) {
            log.warn("Некорректный ID контроля: {}", controlId);
            throw new IllegalArgumentException("ID контроля должен быть положительным числом");
        }

        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CADET"))) {
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
        } else {
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
                log.warn("Нет доступа к результатам контроля {} для преподавателя {}", controlId, teacher.getUserId());
                throw new RuntimeException("Нет доступа к этому контролю");
            }
        }

        Control control = controlRepository.findById(controlId)
                .orElseThrow(() -> {
                    log.warn("Контроль не найден: {}", controlId);
                    return new RuntimeException("Контроль не найден");
                });

        List<Cadet> allCadets = cadetRepository.findByGroupId(control.getGroup().getId());
        log.debug("Всего курсантов в группе: {}", allCadets.size());

        List<ControlResult> results = controlResultRepository.findByControlId(controlId);
        log.debug("Всего результатов по контролю: {}", results.size());

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

        // Проверка входных данных
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
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        Control control = controlRepository.findById(request.getControlId())
                .orElseThrow(() -> {
                    log.warn("Контроль не найден: {}", request.getControlId());
                    return new RuntimeException("Контроль не найден");
                });

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

        // Список допустимых статусов
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
                    .orElseThrow(() -> {
                        log.warn("Курсант с ID {} не найден", cadetRaw.getCadetId());
                        return new RuntimeException("Курсант с ID " + cadetRaw.getCadetId() + " не найден");
                    });

            log.debug("Обработка курсанта {} ({}), статус: {}",
                    cadet.getUserId(), cadet.getUser().getLastName(), cadetRaw.getStatus());

            // Проверка статуса
            if (cadetRaw.getStatus() != null && !validStatuses.contains(cadetRaw.getStatus())) {
                log.warn("Некорректный статус для курсанта {}: {}", cadet.getUserId(), cadetRaw.getStatus());
                throw new IllegalArgumentException("Некорректный статус: " + cadetRaw.getStatus() +
                        ". Допустимые: " + validStatuses);
            }

            // Если нет результатов - создаем записи со статусом
            if (cadetRaw.getRawResults() == null || cadetRaw.getRawResults().isEmpty()) {
                log.debug("Курсант {} без результатов, создаем записи со статусом", cadet.getUserId());
                for (Short number : expectedNumbers) {
                    Standard standard = standardRepository.findFirstByNumber(number)
                            .orElseThrow(() -> {
                                log.warn("Норматив с номером {} не найден", number);
                                return new RuntimeException("Норматив с номером " + number + " не найден");
                            });

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
                    log.warn("Некорректный номер норматива: {}", raw.getStandardNumber());
                    throw new IllegalArgumentException("Номер норматива должен быть от 1 до 10000");
                }

                Standard standard = standardRepository.findFirstByNumber(raw.getStandardNumber())
                        .orElseThrow(() -> {
                            log.warn("Норматив с номером {} не найден", raw.getStandardNumber());
                            return new RuntimeException(
                                    "Норматив с номером " + raw.getStandardNumber() + " не найден");
                        });

                if (!expectedNumbers.contains(raw.getStandardNumber())) {
                    log.warn("Норматив {} не входит в список нормативов контроля {}",
                            raw.getStandardNumber(), control.getId());
                    throw new RuntimeException(
                            String.format("Норматив с номером %d не входит в список нормативов контроля %d",
                                    raw.getStandardNumber(), control.getId())
                    );
                }

                // Проверка на дубликаты
                if (!submittedNumbers.add(raw.getStandardNumber())) {
                    log.warn("Дубликат норматива {} для курсанта {}", raw.getStandardNumber(), cadet.getUserId());
                    throw new RuntimeException("Обнаружен дубликат норматива с номером " + raw.getStandardNumber() +
                            " для курсанта " + cadet.getUserId());
                }

                // Проверка значений (одно из двух должно быть)
                if (raw.getTimeValue() == null && raw.getIntValue() == null) {
                    log.warn("Для норматива {} не указаны значения", raw.getStandardNumber());
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

                log.debug("Норматив {} оценен в {} баллов", raw.getStandardNumber(), mark);

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

            ControlSummary summary = new ControlSummary();
            summary.setId(new ControlSummaryId(control.getId(), cadetId));
            summary.setControl(control);
            summary.setCadet(cadet);
            summary.setCadetMilitaryRank(cadet.getMilitaryRank());
            summary.setCadetPost(cadet.getPost());
            summary.setFinalMark(finalMark);

            controlSummaryRepository.save(summary);
            log.debug("Сохранена итоговая оценка {} для курсанта {}", finalMark, cadetId);
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

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка создания контроля с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            log.warn("Попытка создания контроля с null request");
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        // Проверка ID группы
        if (request.getGroupId() == null || request.getGroupId() <= 0) {
            log.warn("Некорректный ID группы: {}", request.getGroupId());
            throw new IllegalArgumentException("ID группы должен быть положительным числом");
        }

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> {
                    log.warn("Группа не найдена: {}", request.getGroupId());
                    return new RuntimeException("Группа не найдена");
                });

        if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
            log.warn("Нет доступа к группе {} для преподавателя {}", request.getGroupId(), teacher.getUserId());
            throw new RuntimeException("Нет доступа к этой группе");
        }

        // Проверка типа контроля
        List<String> validTypes = Arrays.asList(
                "Контрольное занятие", "Зачет", "Экзамен", "Смотр-конкурс"
        );
        if (!validTypes.contains(request.getType())) {
            log.warn("Некорректный тип контроля: {}", request.getType());
            throw new IllegalArgumentException("Некорректный тип контроля. Допустимые: " + validTypes);
        }

        // Проверка даты
        if (request.getDate() == null) {
            log.warn("Не указана дата контроля");
            throw new IllegalArgumentException("Дата контроля не может быть пустой");
        }

        // Проверка номеров нормативов
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
            if (!standardRepository.findFirstByNumber(number).isPresent()) {
                log.warn("Норматив с номером {} не найден", number);
                throw new RuntimeException("Норматив с номером " + number + " не найден");
            }
        }

        // Проверка инспекторов
        if (request.getInspectorIds() != null) {
            log.debug("Инспекторы: {}", request.getInspectorIds());
            for (Long inspectorId : request.getInspectorIds()) {
                if (inspectorId == null || inspectorId <= 0) {
                    log.warn("Некорректный ID инспектора: {}", inspectorId);
                    throw new IllegalArgumentException("ID инспектора должен быть положительным числом");
                }
                if (!teacherRepository.existsById(inspectorId)) {
                    log.warn("Инспектор с ID {} не найден", inspectorId);
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

        // Проверка входных данных
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
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        Control control = controlRepository.findById(request.getControlId())
                .orElseThrow(() -> {
                    log.warn("Контроль не найден: {}", request.getControlId());
                    return new RuntimeException("Контроль не найден");
                });

        if (!control.getGroup().getUniversity().getId().equals(teacher.getUniversity().getId())) {
            log.warn("Нет доступа к контролю {} для преподавателя {}", request.getControlId(), teacher.getUserId());
            throw new RuntimeException("Нет доступа к этому контролю");
        }

        Map<Long, List<ControlResult>> updatedResultsByCadet = new HashMap<>();
        Map<Long, Integer> coursesByCadet = new HashMap<>();
        int updatedCount = 0;

        for (UpdateControlResultRequest update : request.getUpdates()) {
            updatedCount++;

            // Проверка входных данных обновления
            if (update.getCadetId() == null || update.getCadetId() <= 0) {
                log.warn("Некорректный ID курсанта в обновлении #{}", updatedCount);
                throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
            }

            if (update.getStandardNumber() == null || update.getStandardNumber() <= 0 || update.getStandardNumber() > 10000) {
                log.warn("Некорректный номер норматива в обновлении #{}: {}", updatedCount, update.getStandardNumber());
                throw new IllegalArgumentException("Номер норматива должен быть от 1 до 10000");
            }

            log.debug("Обновление #{}: курсант {}, норматив {}", updatedCount, update.getCadetId(), update.getStandardNumber());

            Standard standard = standardRepository.findFirstByNumber(update.getStandardNumber())
                    .orElseThrow(() -> {
                        log.warn("Норматив с номером {} не найден", update.getStandardNumber());
                        return new RuntimeException(
                                "Норматив с номером " + update.getStandardNumber() + " не найден");
                    });

            ControlResult result = controlResultRepository
                    .findByControlIdAndCadet_UserIdAndStandardId(
                            request.getControlId(),
                            update.getCadetId(),
                            standard.getId()
                    ).orElseThrow(() -> {
                        log.warn("Результат для курсанта {} и норматива {} не найден",
                                update.getCadetId(), update.getStandardNumber());
                        return new RuntimeException(
                                String.format("Результат для курсанта %d и норматива %d не найден",
                                        update.getCadetId(), update.getStandardNumber())
                        );
                    });

            Cadet cadet = result.getCadet();

            // Проверка статуса
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

            if (update.getTimeValue() != null || update.getIntValue() != null) {
                // Проверка что одно из значений передано
                if (update.getTimeValue() == null && update.getIntValue() == null) {
                    log.warn("Не указаны значения для норматива {}", update.getStandardNumber());
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
                log.debug("Оценка обновлена на {}", newMark);
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
            log.info("Пересчет итоговых оценок для {} курсантов", updatedResultsByCadet.size());

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
                    log.debug("Обновлена итоговая оценка {} для курсанта {}", finalMark, cadetId);
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
                    log.debug("Создана итоговая оценка {} для курсанта {}", finalMark, cadetId);
                }
            }
        }

        log.info("Обновление результатов контроля {} завершено (обработано {} записей)",
                control.getId(), request.getUpdates().size());
    }
}