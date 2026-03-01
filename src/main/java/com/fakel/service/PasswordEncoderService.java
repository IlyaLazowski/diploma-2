package com.fakel.service;

import com.fakel.model.User;
import com.fakel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class PasswordEncoderService {

    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void encodePasswords() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        for (User user : userRepository.findAll()) {
            String rawPassword = user.getPassword();
            // Проверяем, не зашифрован ли уже пароль
            if (!rawPassword.startsWith("$2a$")) {
                user.setPassword(encoder.encode(rawPassword));
                userRepository.save(user);
                System.out.println("Зашифрован пароль для пользователя: " + user.getLogin());
            }
        }
    }
}