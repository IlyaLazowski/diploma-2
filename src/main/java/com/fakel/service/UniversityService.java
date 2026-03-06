package com.fakel.service;

import com.fakel.dto.UniversityDto;
import com.fakel.model.University;
import com.fakel.repository.UniversityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UniversityService {

    private static final Logger log = LoggerFactory.getLogger(UniversityService.class);

    @Autowired
    private UniversityRepository universityRepository;

    public List<UniversityDto> getAllUniversities() {

        log.info("Получение списка всех университетов");

        List<University> universities;

        try {
            // Достаем все университеты из БД, сортируем по оценке
            universities = universityRepository.findAllByOrderByMarkDesc();
            log.debug("Получено {} университетов из БД", universities != null ? universities.size() : 0);
        } catch (Exception e) {
            log.error("Ошибка при получении университетов из БД: {}", e.getMessage(), e);
            // Если ошибка при запросе к БД, возвращаем пустой список
            return new ArrayList<>();
        }

        // Проверка на null
        if (universities == null || universities.isEmpty()) {
            log.debug("Список университетов пуст");
            return new ArrayList<>();
        }

        // Превращаем University → UniversityDto
        List<UniversityDto> result = universities.stream()
                .filter(u -> u != null)  // пропускаем null элементы
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.info("Возвращаем {} университетов", result.size());
        log.debug("Университеты: {}", result.stream()
                .map(u -> u.getName() + " (" + u.getMark() + ")")
                .collect(Collectors.joining(", ")));

        return result;
    }

    private UniversityDto convertToDto(University university) {
        if (university == null) {
            log.trace("Конвертация null университета в null DTO");
            return null;
        }

        log.trace("Конвертация университета id={} в DTO", university.getId());

        // Безопасное получение данных с проверками на null
        Long id = university.getId();
        String code = university.getCode();

        // Если code null, используем пустую строку
        if (code == null) {
            log.trace("У университета id={} отсутствует code, используем пустую строку", id);
            code = "";
        }

        UniversityDto dto = new UniversityDto(id, code, university.getMark());
        log.trace("Создан DTO: id={}, name={}, mark={}", id, code, university.getMark());

        return dto;
    }
}