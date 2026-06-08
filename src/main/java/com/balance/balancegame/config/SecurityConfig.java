package com.balance.balancegame.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 질문 등록 폼/투표/등록/수정/삭제, 마이페이지는 로그인 필요 (먼저 매칭되어야 함)
                        .requestMatchers("/questions/new", "/questions/*/edit", "/mypage").authenticated()
                        .requestMatchers(HttpMethod.POST, "/questions", "/questions/*/vote",
                                "/questions/*/edit", "/questions/*/delete").authenticated()
                        // 목록/결과 보기, 회원가입, 로그인, 정적 리소스는 누구나
                        .requestMatchers("/", "/questions", "/questions/*",
                                "/signup", "/login", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/questions", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/questions")
                        .permitAll()
                );
        return http.build();
    }
}
