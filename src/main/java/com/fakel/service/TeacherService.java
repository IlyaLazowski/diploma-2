package com.fakel.service;

import com.fakel.dto.UpdateTeacherProfileRequest;
import com.fakel.model.Teacher;
import com.fakel.model.User;
import com.fakel.repository.TeacherRepository;
import com.fakel.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherService {

    private static final Logger log = LoggerFactory.getLogger(TeacherService.class);

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void updateProfile(UserDetails userDetails, UpdateTeacherProfileRequest request) {

        log.info("Обновление профиля преподавателя: user={}",
                userDetails != null ? userDetails.getUsername() : null);

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка обновления профиля с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            log.warn("Попытка обновления профиля с null request");
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        // Получаем преподавателя
        log.debug("Поиск преподавателя по логину: {}", userDetails.getUsername());
        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        // Проверяем наличие пользователя
        User user = teacher.getUser();
        if (user == null) {
            log.warn("У преподавателя {} отсутствуют данные пользователя", teacher.getUserId());
            throw new RuntimeException("У преподавателя отсутствуют данные пользователя");
        }

        log.debug("Текущие данные преподавателя: email={}, телефон={}, квалификация={}, должность={}",
                user.getMail(), user.getPhoneNumber(), teacher.getQualification(), teacher.getPost());

        boolean updated = false;

        // Обновляем email если передан
        if (request.getMail() != null && !request.getMail().trim().isEmpty()) {
            String newMail = request.getMail().trim();
            log.debug("Запрос на обновление email: {} -> {}", user.getMail(), newMail);

            // Проверка длины email
            if (newMail.length() > 256) {
                log.warn("Email слишком длинный: {} символов", newMail.length());
                throw new IllegalArgumentException("Email не может быть длиннее 256 символов");
            }

            // Проверка формата email
            if (!isValidEmail(newMail)) {
                log.warn("Некорректный формат email: {}", newMail);
                throw new IllegalArgumentException("Некорректный формат email");
            }

            if (!newMail.equals(user.getMail())) {
                if (userRepository.existsByMail(newMail)) {
                    log.warn("Email уже используется: {}", newMail);
                    throw new RuntimeException("Email уже используется");
                }
                user.setMail(newMail);
                updated = true;
                log.info("Email преподавателя {} обновлен на {}", userDetails.getUsername(), newMail);
            } else {
                log.debug("Email не изменился");
            }
        }

        // Обновляем телефон если передан
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            String newPhone = request.getPhoneNumber().trim();
            log.debug("Запрос на обновление телефона: {} -> {}", user.getPhoneNumber(), newPhone);

            // Проверка формата телефона
            if (!isValidPhone(newPhone)) {
                log.warn("Некорректный формат телефона: {}", newPhone);
                throw new IllegalArgumentException("Телефон должен быть в формате +375XXXXXXXXX");
            }

            if (!newPhone.equals(user.getPhoneNumber())) {
                if (userRepository.existsByPhoneNumber(newPhone)) {
                    log.warn("Телефон уже используется: {}", newPhone);
                    throw new RuntimeException("Телефон уже используется");
                }
                user.setPhoneNumber(newPhone);
                updated = true;
                log.info("Телефон преподавателя {} обновлен на {}", userDetails.getUsername(), newPhone);
            } else {
                log.debug("Телефон не изменился");
            }
        }

        // Обновляем квалификацию если передана
        if (request.getQualification() != null && !request.getQualification().trim().isEmpty()) {
            String newQualification = request.getQualification().trim();
            log.debug("Запрос на обновление квалификации: {} -> {}", teacher.getQualification(), newQualification);

            // Проверка длины квалификации
            if (newQualification.length() > 64) {
                log.warn("Квалификация слишком длинная: {} символов", newQualification.length());
                throw new IllegalArgumentException("Квалификация не может быть длиннее 64 символов");
            }

            // Проверка допустимых символов
            if (!isValidText(newQualification)) {
                log.warn("Квалификация содержит недопустимые символы: {}", newQualification);
                throw new IllegalArgumentException("Квалификация содержит недопустимые символы");
            }

            teacher.setQualification(newQualification);
            updated = true;
            log.info("Квалификация преподавателя {} обновлена на {}", userDetails.getUsername(), newQualification);
        }

        // Обновляем должность если передана
        if (request.getPost() != null && !request.getPost().trim().isEmpty()) {
            String newPost = request.getPost().trim();
            log.debug("Запрос на обновление должности: {} -> {}", teacher.getPost(), newPost);

            // Проверка длины должности
            if (newPost.length() > 64) {
                log.warn("Должность слишком длинная: {} символов", newPost.length());
                throw new IllegalArgumentException("Должность не может быть длиннее 64 символов");
            }

            // Проверка допустимых символов
            if (!isValidText(newPost)) {
                log.warn("Должность содержит недопустимые символы: {}", newPost);
                throw new IllegalArgumentException("Должность содержит недопустимые символы");
            }

            teacher.setPost(newPost);
            updated = true;
            log.info("Должность преподавателя {} обновлена на {}", userDetails.getUsername(), newPost);
        }

        if (updated) {
            userRepository.save(user);
            teacherRepository.save(teacher);
            log.info("Профиль преподавателя {} успешно обновлен", userDetails.getUsername());
        } else {
            log.debug("Нет изменений в профиле преподавателя {}", userDetails.getUsername());
        }
    }

    /**
     * Проверка формата email
     */
    private boolean isValidEmail(String email) {
        if (email == null) {
            log.trace("Проверка email: null -> false");
            return false;
        }
        boolean isValid = email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        log.trace("Проверка email '{}': {}", email, isValid);
        return isValid;
    }

    /**
     * Проверка формата телефона (+375XXXXXXXXX)
     */
    private boolean isValidPhone(String phone) {
        if (phone == null) {
            log.trace("Проверка телефона: null -> false");
            return false;
        }
        boolean isValid = phone.matches("^\\+375\\d{9}$");
        log.trace("Проверка телефона '{}': {}", phone, isValid);
        return isValid;
    }

    /**
     * Проверка текста на допустимые символы
     */
    private boolean isValidText(String text) {
        if (text == null) {
            log.trace("Проверка текста: null -> false");
            return false;
        }
        boolean isValid = text.matches("^[А-Яа-яЁёA-Za-z0-9\\s\\-.,()]+$");
        log.trace("Проверка текста '{}': {}", text, isValid);
        return isValid;
    }
}