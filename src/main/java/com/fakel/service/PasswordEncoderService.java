package com.fakel.service;

import com.fakel.model.User;
import com.fakel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

@Service
public class PasswordEncoderService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @PostConstruct
    @Transactional
    public void encodePasswords() {
        Iterable<User> users = userRepository.findAll();

        for (User user : users) {
            if (user == null) {
                continue;
            }

            String rawPassword = user.getPassword();
            String login = user.getLogin();

            if (rawPassword == null) {
                continue;
            }

            if (rawPassword.trim().isEmpty()) {
                continue;
            }

            if (!rawPassword.startsWith("$2a$") && !rawPassword.startsWith("$2b$") && !rawPassword.startsWith("$2y$")) {
                try {
                    user.setPassword(encoder.encode(rawPassword.trim()));
                    userRepository.save(user);
                } catch (Exception e) {
                    // пропускаем ошибки для отдельных пользователей
                }
            }
        }
    }

    /**
     * Метод для ручного кодирования пароля (можно вызвать из контроллера при создании пользователя)
     */
    public String encodePassword(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Пароль не может быть null");
        }

        String trimmedPassword = rawPassword.trim();

        if (trimmedPassword.isEmpty()) {
            throw new IllegalArgumentException("Пароль не может быть пустым");
        }

        if (trimmedPassword.length() < 4 || trimmedPassword.length() > 256) {
            throw new IllegalArgumentException("Пароль должен быть от 4 до 256 символов");
        }

        return encoder.encode(trimmedPassword);
    }

    /**
     * Проверка соответствия пароля
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword.trim(), encodedPassword);
    }
}