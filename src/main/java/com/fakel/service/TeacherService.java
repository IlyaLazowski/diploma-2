package com.fakel.service;

import com.fakel.dto.UpdateTeacherProfileRequest;
import com.fakel.model.Teacher;
import com.fakel.model.User;
import com.fakel.repository.TeacherRepository;
import com.fakel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherService {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void updateProfile(UserDetails userDetails, UpdateTeacherProfileRequest request) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (request == null) {
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        // Получаем преподавателя
        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        // Проверяем наличие пользователя
        User user = teacher.getUser();
        if (user == null) {
            throw new RuntimeException("У преподавателя отсутствуют данные пользователя");
        }

        boolean updated = false;

        // Обновляем email если передан
        if (request.getMail() != null && !request.getMail().trim().isEmpty()) {
            String newMail = request.getMail().trim();

            // Проверка длины email
            if (newMail.length() > 256) {
                throw new IllegalArgumentException("Email не может быть длиннее 256 символов");
            }

            // Проверка формата email
            if (!isValidEmail(newMail)) {
                throw new IllegalArgumentException("Некорректный формат email");
            }

            if (!newMail.equals(user.getMail())) {
                if (userRepository.existsByMail(newMail)) {
                    throw new RuntimeException("Email уже используется");
                }
                user.setMail(newMail);
                updated = true;
            }
        }

        // Обновляем телефон если передан
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            String newPhone = request.getPhoneNumber().trim();

            // Проверка формата телефона
            if (!isValidPhone(newPhone)) {
                throw new IllegalArgumentException("Телефон должен быть в формате +375XXXXXXXXX");
            }

            if (!newPhone.equals(user.getPhoneNumber())) {
                if (userRepository.existsByPhoneNumber(newPhone)) {
                    throw new RuntimeException("Телефон уже используется");
                }
                user.setPhoneNumber(newPhone);
                updated = true;
            }
        }

        // Обновляем квалификацию если передана
        if (request.getQualification() != null && !request.getQualification().trim().isEmpty()) {
            String newQualification = request.getQualification().trim();

            // Проверка длины квалификации
            if (newQualification.length() > 64) {
                throw new IllegalArgumentException("Квалификация не может быть длиннее 64 символов");
            }

            // Проверка допустимых символов
            if (!isValidText(newQualification)) {
                throw new IllegalArgumentException("Квалификация содержит недопустимые символы");
            }

            teacher.setQualification(newQualification);
            updated = true;
        }

        // Обновляем должность если передана
        if (request.getPost() != null && !request.getPost().trim().isEmpty()) {
            String newPost = request.getPost().trim();

            // Проверка длины должности
            if (newPost.length() > 64) {
                throw new IllegalArgumentException("Должность не может быть длиннее 64 символов");
            }

            // Проверка допустимых символов
            if (!isValidText(newPost)) {
                throw new IllegalArgumentException("Должность содержит недопустимые символы");
            }

            teacher.setPost(newPost);
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
            teacherRepository.save(teacher);
        }
    }

    /**
     * Проверка формата email
     */
    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    /**
     * Проверка формата телефона (+375XXXXXXXXX)
     */
    private boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return phone.matches("^\\+375\\d{9}$");
    }

    /**
     * Проверка текста на допустимые символы
     */
    private boolean isValidText(String text) {
        if (text == null) return false;
        return text.matches("^[А-Яа-яЁёA-Za-z0-9\\s\\-.,()]+$");
    }
}