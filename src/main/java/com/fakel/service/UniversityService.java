package com.fakel.service;

import com.fakel.dto.UniversityDto;
import com.fakel.model.University;
import com.fakel.repository.UniversityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UniversityService {

    @Autowired
    private UniversityRepository universityRepository;

    public List<UniversityDto> getAllUniversities() {

        List<University> universities;

        try {
            // Достаем все университеты из БД, сортируем по оценке
            universities = universityRepository.findAllByOrderByMarkDesc();
        } catch (Exception e) {
            // Если ошибка при запросе к БД, возвращаем пустой список
            return new ArrayList<>();
        }

        // Проверка на null
        if (universities == null) {
            return new ArrayList<>();
        }

        // Превращаем University → UniversityDto
        return universities.stream()
                .filter(u -> u != null)  // пропускаем null элементы
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private UniversityDto convertToDto(University university) {
        if (university == null) {
            return null;
        }

        // Безопасное получение данных с проверками на null
        Long id = university.getId();
        String code = university.getCode();

        // Если code null, используем пустую строку
        if (code == null) {
            code = "";
        }

        return new UniversityDto(id, code, university.getMark());
    }
}