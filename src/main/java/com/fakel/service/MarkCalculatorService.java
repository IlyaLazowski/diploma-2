package com.fakel.service;

import com.fakel.model.ControlResult;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MarkCalculatorService {

    /**
     * Расчёт итоговой оценки по трём нормативам
     */
    public Short calculateFinalMark(List<Short> marks, Integer course) {
        // Проверка входных данных
        if (marks == null) {
            throw new IllegalArgumentException("Список оценок не может быть null");
        }

        if (marks.size() != 3) {
            throw new IllegalArgumentException("Должно быть ровно 3 оценки, получено: " + marks.size());
        }

        // Проверка, что все оценки не null
        for (int i = 0; i < marks.size(); i++) {
            if (marks.get(i) == null) {
                throw new IllegalArgumentException("Оценка под индексом " + i + " не может быть null");
            }
        }

        // Проверка, что оценки в допустимом диапазоне (0-5)
        for (Short mark : marks) {
            if (mark < 0 || mark > 5) {
                throw new IllegalArgumentException("Оценка " + mark + " вне допустимого диапазона (0-5)");
            }
        }

        if (course == null) {
            throw new IllegalArgumentException("Курс не может быть null");
        }

        if (course < 1 || course > 5) {
            throw new IllegalArgumentException("Курс должен быть от 1 до 5, получено: " + course);
        }

        // Сортируем для удобства сравнения
        List<Short> sorted = marks.stream().sorted().collect(Collectors.toList());
        Short m1 = sorted.get(0);  // минимальная
        Short m2 = sorted.get(1);  // средняя
        Short m3 = sorted.get(2);  // максимальная

        // Проверяем комбинации
        if (m3 == 5 && m2 == 5 && m1 == 5) return 10;
        if (m3 == 5 && m2 == 5 && m1 == 4) return 9;
        if (m3 == 5 && m2 == 4 && m1 == 4) return 8;
        if (m3 == 4 && m2 == 4 && m1 == 4) return 7;

        // 4/5 4/5 3 → 6
        if (m3 >= 4 && m2 >= 4 && m1 == 3) return 6;

        // 4/5 3 3 → 5
        if (m3 >= 4 && m2 == 3 && m1 == 3) return 5;

        // 3 3 3 → 4
        if (m3 == 3 && m2 == 3 && m1 == 3) return 4;

        // 4/5 4/5 2 → для 1 курса 4, для остальных 3
        if (m3 >= 4 && m2 >= 4 && m1 == 2) {
            return course == 1 ? (short)4 : (short)3;
        }

        // 3/4/5 3 2 → 3
        if (m3 >= 3 && m2 == 3 && m1 == 2) return 3;

        // 3/4/5 2 2 → 2
        if (m3 >= 3 && m2 == 2 && m1 == 2) return 2;

        // 2 2 2 → 1
        if (m3 == 2 && m2 == 2 && m1 == 2) return 1;

        // Неизвестная комбинация
        throw new IllegalArgumentException("Невозможно рассчитать итоговую оценку для комбинации: " +
                marks.stream().map(String::valueOf).collect(Collectors.joining(", ")));
    }

    /**
     * Расчёт итоговых оценок для группы курсантов
     */
    public Map<Long, Short> calculateFinalMarks(
            Map<Long, List<ControlResult>> resultsByCadet,
            Map<Long, Integer> coursesByCadet) {

        // Проверка входных данных
        if (resultsByCadet == null) {
            throw new IllegalArgumentException("Результаты по курсантам не могут быть null");
        }

        if (coursesByCadet == null) {
            throw new IllegalArgumentException("Информация о курсах не может быть null");
        }

        return resultsByCadet.entrySet().stream()
                .filter(entry -> {
                    // Проверяем, что у курсанта ровно 3 результата
                    if (entry.getValue() == null) {
                        return false;
                    }

                    // Проверяем, что все результаты имеют оценки
                    boolean allHaveMarks = entry.getValue().stream()
                            .allMatch(r -> r != null && r.getMark() != null);

                    return entry.getValue().size() == 3 && allHaveMarks;
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
                                throw new IllegalArgumentException("Не найден курс для курсанта с ID: " + entry.getKey());
                            }

                            return calculateFinalMark(marks, course);
                        }
                ));
    }

    /**
     * Итоговая оценка для отсутствующего курсанта
     */
    public Short calculateFinalMarkForAbsent() {
        return null;  // или 0, или 2 — зависит от логики
    }
}