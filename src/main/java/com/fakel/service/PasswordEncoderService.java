package com.fakel.service;

import com.fakel.model.User;
import com.fakel.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

@Service
public class PasswordEncoderService {

    private static final Logger log = LoggerFactory.getLogger(PasswordEncoderService.class);

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @PostConstruct
    @Transactional
    public void encodePasswords() {
        log.info("Запуск процедуры кодирования паролей всех пользователей");

        Iterable<User> users = userRepository.findAll();
        int processed = 0;
        int encoded = 0;
        int skipped = 0;
        int errors = 0;

        for (User user : users) {
            processed++;

            if (user == null) {
                log.warn("Найден null пользователь в базе данных, пропускаем");
                skipped++;
                continue;
            }

            String rawPassword = user.getPassword();
            String login = user.getLogin();

            if (rawPassword == null) {
                log.debug("Пользователь {}: пароль null, пропускаем", login);
                skipped++;
                continue;
            }

            String trimmedPassword = rawPassword.trim();
            if (trimmedPassword.isEmpty()) {
                log.debug("Пользователь {}: пустой пароль, пропускаем", login);
                skipped++;
                continue;
            }

            // Проверяем, не зашифрован ли уже пароль (BCrypt хеши начинаются с $2a$, $2b$ или $2y$)
            if (!trimmedPassword.startsWith("$2a$") && !trimmedPassword.startsWith("$2b$") && !trimmedPassword.startsWith("$2y$")) {
                try {
                    log.debug("Пользователь {}: пароль не зашифрован, кодируем", login);
                    user.setPassword(encoder.encode(trimmedPassword));
                    userRepository.save(user);
                    encoded++;
                    log.info("Пользователь {}: пароль успешно закодирован", login);
                } catch (Exception e) {
                    log.error("Пользователь {}: ошибка при кодировании пароля: {}", login, e.getMessage(), e);
                    errors++;
                }
            } else {
                log.debug("Пользователь {}: пароль уже зашифрован, пропускаем", login);
                skipped++;
            }
        }

        log.info("Кодирование паролей завершено. Всего обработано: {}, закодировано: {}, пропущено: {}, ошибок: {}",
                processed, encoded, skipped, errors);
    }

    /**
     * Метод для ручного кодирования пароля (можно вызвать из контроллера при создании пользователя)
     */
    public String encodePassword(String rawPassword) {
        log.info("Ручное кодирование пароля");

        if (rawPassword == null) {
            log.warn("Попытка кодирования null пароля");
            throw new IllegalArgumentException("Пароль не может быть null");
        }

        String trimmedPassword = rawPassword.trim();
        log.debug("Длина пароля после trim: {} символов", trimmedPassword.length());

        if (trimmedPassword.isEmpty()) {
            log.warn("Попытка кодирования пустого пароля");
            throw new IllegalArgumentException("Пароль не может быть пустым");
        }

        if (trimmedPassword.length() < 4 || trimmedPassword.length() > 256) {
            log.warn("Некорректная длина пароля: {} символов (допустимо 4-256)", trimmedPassword.length());
            throw new IllegalArgumentException("Пароль должен быть от 4 до 256 символов");
        }

        String encoded = encoder.encode(trimmedPassword);
        log.info("Пароль успешно закодирован");
        log.debug("Закодированный пароль: {}", encoded);

        return encoded;
    }

    /**
     * Проверка соответствия пароля
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        log.info("Проверка соответствия пароля");

        if (rawPassword == null || encodedPassword == null) {
            log.warn("Попытка проверки с null параметрами: rawPassword={}, encodedPassword={}",
                    rawPassword != null, encodedPassword != null);
            return false;
        }

        String trimmedRaw = rawPassword.trim();
        boolean matches = encoder.matches(trimmedRaw, encodedPassword);

        log.debug("Результат проверки: {}", matches ? "совпадает" : "не совпадает");
        return matches;
    }
}