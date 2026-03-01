package com.fakel.security;

import com.fakel.model.User;
import com.fakel.repository.CadetRepository;
import com.fakel.repository.TeacherRepository;
import com.fakel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CadetRepository cadetRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + login));


        String role = determineRole(user);

        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    private String determineRole(User user) {
        Long userId = user.getId();

        if (cadetRepository.existsByUserId(userId)) {
            return "CADET";
        } else if (teacherRepository.existsByUserId(userId)) {
            return "TEACHER";
        } else {
            return "UNKNOWN";
        }
    }
}