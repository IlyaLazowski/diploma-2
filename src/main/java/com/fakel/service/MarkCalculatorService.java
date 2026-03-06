package com.fakel.service;

import com.fakel.model.ControlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MarkCalculatorService {

    private static final Logger log = LoggerFactory.getLogger(MarkCalculatorService.class);

    /**
     * Расчёт итоговой оценки по трём нормативам
     */
    public Short calculateFinalMark(List<Short> marks, Integer course) {
        log.info("Расчет итоговой оценки: marks={}, course={}", marks, course);
        log.debug("Входные данные: marks.size={}, course={}", marks != null ? marks.size() : null, course);

        // Проверка входных данных
        if (marks == null) {
            log.warn("Список оценок null");
            throw new IllegalArgumentException("Список оценок не может быть null");
        }

        if (marks.size() != 3) {
            log.warn("Некорректное количество оценок: {}, ожидалось 3", marks.size());
            throw new IllegalArgumentException("Должно быть ровно 3 оценки, получено: " + marks.size());
        }

        // Проверка, что все оценки не null
        for (int i = 0; i < marks.size(); i++) {
            if (marks.get(i) == null) {
                log.warn("Оценка под индексом {} null", i);
                throw new IllegalArgumentException("Оценка под индексом " + i + " не может быть null");
            }
        }

        // Проверка, что оценки в допустимом диапазоне (0-5)
        for (Short mark : marks) {
            if (mark < 0 || mark > 5) {
                log.warn("Оценка {} вне допустимого диапазона (0-5)", mark);
                throw new IllegalArgumentException("Оценка " + mark + " вне допустимого диапазона (0-5)");
            }
        }

        if (course == null) {
            log.warn("Курс null");
            throw new IllegalArgumentException("Курс не может быть null");
        }

        if (course < 1 || course > 5) {
            log.warn("Некорректный курс: {}", course);
            throw new IllegalArgumentException("Курс должен быть от 1 до 5, получено: " + course);
        }

        // Сортируем для удобства сравнения
        List<Short> sorted = marks.stream().sorted().collect(Collectors.toList());
        Short m1 = sorted.get(0);  // минимальная
        Short m2 = sorted.get(1);  // средняя
        Short m3 = sorted.get(2);  // максимальная

        log.debug("Отсортированные оценки: min={}, mid={}, max={}", m1, m2, m3);

        Short result;

        // Проверяем комбинации
        if (m3 == 5 && m2 == 5 && m1 == 5) {
            result = 10;
            log.debug("Комбинация 5,5,5 -> {}", result);
        } else if (m3 == 5 && m2 == 5 && m1 == 4) {
            result = 9;
            log.debug("Комбинация 5,5,4 -> {}", result);
        } else if (m3 == 5 && m2 == 4 && m1 == 4) {
            result = 8;
            log.debug("Комбинация 5,4,4 -> {}", result);
        } else if (m3 == 4 && m2 == 4 && m1 == 4) {
            result = 7;
            log.debug("Комбинация 4,4,4 -> {}", result);
        } else if (m3 >= 4 && m2 >= 4 && m1 == 3) {
            result = 6;
            log.debug("Комбинация 4/5,4/5,3 -> {}", result);
        } else if (m3 >= 4 && m2 == 3 && m1 == 3) {
            result = 5;
            log.debug("Комбинация 4/5,3,3 -> {}", result);
        } else if (m3 == 3 && m2 == 3 && m1 == 3) {
            result = 4;
            log.debug("Комбинация 3,3,3 -> {}", result);
        } else if (m3 >= 4 && m2 >= 4 && m1 == 2) {
            result = course == 1 ? (short)4 : (short)3;
            log.debug("Комбинация 4/5,4/5,2 с курсом {} -> {}", course, result);
        } else if (m3 >= 3 && m2 == 3 && m1 == 2) {
            result = 3;
            log.debug("Комбинация 3/4/5,3,2 -> {}", result);
        } else if (m3 >= 3 && m2 == 2 && m1 == 2) {
            result = 2;
            log.debug("Комбинация 3/4/5,2,2 -> {}", result);
        } else if (m3 == 2 && m2 == 2 && m1 == 2) {
            result = 1;
            log.debug("Комбинация 2,2,2 -> {}", result);
        } else {
            String errorMsg = "Невозможно рассчитать итоговую оценку для комбинации: " +
                    marks.stream().map(String::valueOf).collect(Collectors.joining(", "));
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.info("Итоговая оценка: {}", result);
        return result;
    }

    /**
     * Расчёт итоговых оценок для группы курсантов
     */
    public Map<Long, Short> calculateFinalMarks(
            Map<Long, List<ControlResult>> resultsByCadet,
            Map<Long, Integer> coursesByCadet) {

        log.info("Расчет итоговых оценок для группы курсантов");
        log.debug("Количество курсантов в resultsByCadet: {}, в coursesByCadet: {}",
                resultsByCadet != null ? resultsByCadet.size() : null,
                coursesByCadet != null ? coursesByCadet.size() : null);

        // Проверка входных данных
        if (resultsByCadet == null) {
            log.warn("resultsByCadet null");
            throw new IllegalArgumentException("Результаты по курсантам не могут быть null");
        }

        if (coursesByCadet == null) {
            log.warn("coursesByCadet null");
            throw new IllegalArgumentException("Информация о курсах не может быть null");
        }

        // Фильтруем и считаем
        Map<Long, Short> result = resultsByCadet.entrySet().stream()
                .filter(entry -> {
                    // Проверяем, что у курсанта ровно 3 результата
                    if (entry.getValue() == null) {
                        log.trace("Курсант {}: результаты null, пропускаем", entry.getKey());
                        return false;
                    }

                    // Проверяем, что все результаты имеют оценки
                    boolean allHaveMarks = entry.getValue().stream()
                            .allMatch(r -> r != null && r.getMark() != null);

                    boolean valid = entry.getValue().size() == 3 && allHaveMarks;

                    if (!valid) {
                        log.trace("Курсант {}: невалидные данные (size={}, allHaveMarks={}), пропускаем",
                                entry.getKey(), entry.getValue().size(), allHaveMarks);
                    }

                    return valid;
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<Short> marks = entry.getValue().stream()
                                    .map(ControlResult::getMark)
                                    .collect(Collectors.toList());

                            Integer course = coursesByCadet.get(entry.getKey());

                            // Проверяем, что курс найден
                            if (course == null) {
                                log.error("Не найден курс для курсанта с ID: {}", entry.getKey());
                                throw new IllegalArgumentException("Не найден курс для курсанта с ID: " + entry.getKey());
                            }

                            log.debug("Расчет оценки для курсанта {}: marks={}, course={}",
                                    entry.getKey(), marks, course);

                            return calculateFinalMark(marks, course);
                        }
                ));

        log.info("Рассчитаны итоговые оценки для {} курсантов", result.size());
        log.debug("Результаты: {}", result);

        return result;
    }

    /**
     * Итоговая оценка для отсутствующего курсанта
     */
    public Short calculateFinalMarkForAbsent() {
        log.info("Расчет итоговой оценки для отсутствующего курсанта: null");
        return null;  // или 0, или 2 — зависит от логики
    }
}