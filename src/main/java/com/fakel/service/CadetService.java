package com.fakel.service;

import com.fakel.dto.UpdateCadetProfileRequest;
import com.fakel.model.Cadet;
import com.fakel.model.User;
import com.fakel.repository.CadetRepository;
import com.fakel.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Service
@Validated
public class CadetService {

    @Autowired
    private CadetRepository cadetRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void updateProfile(UserDetails userDetails, @Valid UpdateCadetProfileRequest request) {

        // Проверка на null
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        User user = cadet.getUser();
        boolean updated = false;

        // Обновление email
        if (request.getMail() != null && !request.getMail().trim().isEmpty()) {
            String newMail = request.getMail().trim();

            // Проверка длины email
            if (newMail.length() > 256) {
                throw new IllegalArgumentException("Email не может быть длиннее 256 символов");
            }

            if (!newMail.equals(user.getMail()) &&
                    userRepository.existsByMail(newMail)) {
                throw new RuntimeException("Email уже используется");
            }
            user.setMail(newMail);
            updated = true;
        }

        // Обновление телефона
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            String newPhone = request.getPhoneNumber().trim();

            if (!newPhone.equals(user.getPhoneNumber()) &&
                    userRepository.existsByPhoneNumber(newPhone)) {
                throw new RuntimeException("Телефон уже используется");
            }
            user.setPhoneNumber(newPhone);
            updated = true;
        }

        // Обновление веса
        if (request.getWeight() != null) {
            // Проверка диапазона веса согласно БД (30-180 кг)
            if (request.getWeight().compareTo(new BigDecimal("30")) < 0 ||
                    request.getWeight().compareTo(new BigDecimal("180")) > 0) {
                throw new IllegalArgumentException("Вес должен быть от 30 до 180 кг");
            }

            // Проверка точности (до 3 знаков после запятой)
            if (request.getWeight().scale() > 3) {
                throw new IllegalArgumentException("Вес может содержать не более 3 знаков после запятой");
            }

            cadet.setWeight(request.getWeight());
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
            cadetRepository.save(cadet);
        }
    }

    @Transactional(readOnly = true)
    public Cadet getCadetByLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("Логин не может быть пустым");
        }

        return cadetRepository.findByUserLogin(login)
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));
    }

    @Transactional(readOnly = true)
    public boolean isValidWeight(BigDecimal weight) {
        return weight != null &&
                weight.compareTo(new BigDecimal("30")) >= 0 &&
                weight.compareTo(new BigDecimal("180")) <= 0 &&
                weight.scale() <= 3;
    }
}