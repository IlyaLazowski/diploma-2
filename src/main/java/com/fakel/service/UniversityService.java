package com.fakel.service;

import com.fakel.dto.UniversityDto;
import com.fakel.model.University;
import com.fakel.repository.UniversityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UniversityService {

    @Autowired
    private UniversityRepository universityRepository;

    public List<UniversityDto> getAllUniversities() {
        // Достаем все университеты из БД, сортируем по оценке
        List<University> universities = universityRepository.findAllByOrderByMarkDesc();

        // Превращаем University → UniversityDto
        return universities.stream()
                .map(u -> new UniversityDto(u.getId(), u.getCode(), u.getMark()))
                .collect(Collectors.toList());
    }
}