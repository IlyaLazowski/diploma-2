package com.fakel.service;

import com.fakel.dto.UpdateCadetProfileRequest;
import com.fakel.model.Cadet;
import com.fakel.model.User;
import com.fakel.repository.CadetRepository;
import com.fakel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadetService {

    @Autowired
    private CadetRepository cadetRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void updateProfile(UserDetails userDetails, UpdateCadetProfileRequest request) {

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        User user = cadet.getUser();
        boolean updated = false;

        if (request.getMail() != null && !request.getMail().isEmpty()) {
            if (!request.getMail().equals(user.getMail()) &&
                    userRepository.existsByMail(request.getMail())) {
                throw new RuntimeException("Email уже используется");
            }
            user.setMail(request.getMail());
            updated = true;
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            if (!request.getPhoneNumber().equals(user.getPhoneNumber()) &&
                    userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new RuntimeException("Телефон уже используется");
            }
            user.setPhoneNumber(request.getPhoneNumber());
            updated = true;
        }

        if (request.getWeight() != null) {
            cadet.setWeight(request.getWeight());
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
            cadetRepository.save(cadet);
        }
    }
}