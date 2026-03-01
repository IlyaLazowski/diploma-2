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

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        User user = teacher.getUser();
        boolean updated = false;

        // Обновляем email если передан
        if (request.getMail() != null && !request.getMail().isEmpty()) {
            if (!request.getMail().equals(user.getMail()) &&
                    userRepository.existsByMail(request.getMail())) {
                throw new RuntimeException("Email уже используется");
            }
            user.setMail(request.getMail());
            updated = true;
        }

        // Обновляем телефон если передан
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            if (!request.getPhoneNumber().equals(user.getPhoneNumber()) &&
                    userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new RuntimeException("Телефон уже используется");
            }
            user.setPhoneNumber(request.getPhoneNumber());
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
            // Teacher не нужно сохранять - он связан с User через @OneToOne
        }
    }
}