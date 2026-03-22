package com.fakel.service;

import com.fakel.dto.UpdateCadetProfileRequest;
import com.fakel.model.Cadet;
import com.fakel.model.User;
import com.fakel.repository.CadetRepository;
import com.fakel.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Service
@Validated
public class CadetService {

    private static final Logger log = LoggerFactory.getLogger(CadetService.class);

    @Autowired
    private CadetRepository cadetRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void updateProfile(UserDetails userDetails, @Valid UpdateCadetProfileRequest request) {
        log.info("Обновление профиля курсанта: {}", userDetails != null ? userDetails.getUsername() : "null");

        // Проверка на null
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка обновления профиля с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            log.warn("Попытка обновления профиля с null request");
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Курсант не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Курсант не найден");
                });

        User user = cadet.getUser();
        log.debug("Текущие данные курсанта: email={}, телефон={}, вес={}",
                user.getMail(), user.getPhoneNumber(), cadet.getWeight());

        boolean updated = false;

        // Обновление email
        if (request.getMail() != null && !request.getMail().trim().isEmpty()) {
            String newMail = request.getMail().trim();


            if (!newMail.equals(user.getMail())) {
                log.debug("Запрос на обновление email: {} -> {}", user.getMail(), newMail);

                // Проверка длины email
                if (newMail.length() > 256) {
                    log.warn("Email слишком длинный: {} символов", newMail.length());
                    throw new IllegalArgumentException("Email не может быть длиннее 256 символов");
                }

                if (userRepository.existsByMail(newMail)) {
                    log.warn("Email уже используется: {}", newMail);
                    throw new RuntimeException("Email уже используется");
                }
                user.setMail(newMail);
                updated = true;
                log.info("Email курсанта {} обновлен на {}", userDetails.getUsername(), newMail);
            } else {
                log.debug("Email не изменился");
            }
        }

        // Обновление телефона
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            String newPhone = request.getPhoneNumber().trim();

            if (!newPhone.equals(user.getPhoneNumber())) {
                log.debug("Запрос на обновление телефона: {} -> {}", user.getPhoneNumber(), newPhone);

                if (userRepository.existsByPhoneNumber(newPhone)) {
                    log.warn("Телефон уже используется: {}", newPhone);
                    throw new RuntimeException("Телефон уже используется");
                }
                user.setPhoneNumber(newPhone);
                updated = true;
                log.info("Телефон курсанта {} обновлен на {}", userDetails.getUsername(), newPhone);
            } else {
                log.debug("Телефон не изменился");
            }
        }

        // Обновление веса
        if (request.getWeight() != null) {

            if (!request.getWeight().equals(cadet.getWeight())) {
                log.debug("Запрос на обновление веса: {} -> {}", cadet.getWeight(), request.getWeight());

                // Проверка диапазона веса согласно БД (30-180 кг)
                if (request.getWeight().compareTo(new BigDecimal("30")) < 0 ||
                        request.getWeight().compareTo(new BigDecimal("180")) > 0) {
                    log.warn("Вес вне допустимого диапазона: {}", request.getWeight());
                    throw new IllegalArgumentException("Вес должен быть от 30 до 180 кг");
                }

                // Проверка точности (до 3 знаков после запятой)
                if (request.getWeight().scale() > 3) {
                    log.warn("Слишком высокая точность веса: {} знаков после запятой", request.getWeight().scale());
                    throw new IllegalArgumentException("Вес может содержать не более 3 знаков после запятой");
                }

                cadet.setWeight(request.getWeight());
                updated = true;
                log.info("Вес курсанта {} обновлен на {}", userDetails.getUsername(), request.getWeight());
            } else {
                log.debug("Вес не изменился");
            }
        }

        if (updated) {
            userRepository.save(user);
            cadetRepository.save(cadet);
            log.info("Профиль курсанта {} успешно обновлен", userDetails.getUsername());
        } else {
            log.debug("Нет изменений в профиле курсанта {}", userDetails.getUsername());
        }
    }

    @Transactional(readOnly = true)
    public Cadet getCadetByLogin(String login) {
        log.info("Получение курсанта по логину: {}", login);

        if (login == null || login.trim().isEmpty()) {
            log.warn("Попытка получения курсанта с пустым логином");
            throw new IllegalArgumentException("Логин не может быть пустым");
        }

        Cadet cadet = cadetRepository.findByUserLogin(login)
                .orElseThrow(() -> {
                    log.warn("Курсант не найден по логину: {}", login);
                    return new RuntimeException("Курсант не найден");
                });

        log.debug("Курсант найден: ID={}, email={}, телефон={}",
                cadet.getUserId(), cadet.getUser().getMail(), cadet.getUser().getPhoneNumber());

        return cadet;
    }

    @Transactional(readOnly = true)
    public boolean isValidWeight(BigDecimal weight) {
        boolean isValid = weight != null &&
                weight.compareTo(new BigDecimal("30")) >= 0 &&
                weight.compareTo(new BigDecimal("180")) <= 0 &&
                weight.scale() <= 3;

        log.debug("Проверка веса {}: {}", weight, isValid ? "корректный" : "некорректный");
        return isValid;
    }
}