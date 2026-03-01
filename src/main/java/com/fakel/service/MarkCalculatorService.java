package com.fakel.service;

import com.fakel.model.ControlResult;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MarkCalculatorService {

    /**
     * Расчёт итоговой оценки по трём нормативам
     */
    public Short calculateFinalMark(List<Short> marks, Integer course) {
        if (marks.size() != 3) {
            throw new RuntimeException("Должно быть ровно 3 оценки");
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
        return null;
    }

    /**
     * Расчёт итоговых оценок для группы курсантов
     */
    public Map<Long, Short> calculateFinalMarks(
            Map<Long, List<ControlResult>> resultsByCadet,
            Map<Long, Integer> coursesByCadet) {

        return resultsByCadet.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 3)  // только те, у кого 3 оценки
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<Short> marks = entry.getValue().stream()
                                    .map(ControlResult::getMark)
                                    .collect(Collectors.toList());
                            Integer course = coursesByCadet.get(entry.getKey());
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