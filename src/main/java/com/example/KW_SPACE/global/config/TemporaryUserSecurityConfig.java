package com.example.KW_SPACE.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Auth 구현 전 User 조회 기능을 검증하기 위한 임시 보안 설정이다.
 * JWT Cookie 기반 인가가 구현되면 #31에서 이 설정을 제거하고 인증 정보를 사용한다.
 */
@Configuration
public class TemporaryUserSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/user", "/api/v1/user/").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
