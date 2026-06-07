package com.balance.balancegame.service;

import com.balance.balancegame.domain.User;
import com.balance.balancegame.dto.SignupForm;
import com.balance.balancegame.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void signup(SignupForm form) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }
        User user = new User();
        user.setUsername(form.getUsername());
        user.setPassword(passwordEncoder.encode(form.getPassword())); // BCrypt 암호화
        user.setRole("ROLE_USER");
        userRepository.save(user);
    }
}
