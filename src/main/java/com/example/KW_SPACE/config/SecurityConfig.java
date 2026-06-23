package com.example.KW_SPACE.config;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthErrorResponseWriter;
import com.example.KW_SPACE.auth.jwt.JwtAuthenticationFilter;
import com.example.KW_SPACE.auth.jwt.JwtTokenProvider;
import com.example.KW_SPACE.auth.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
			AuthErrorResponseWriter authErrorResponseWriter) throws Exception {
		return http
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint((request, response, authException) ->
								authErrorResponseWriter.write(response, AuthErrorCode.AUTH_INVALID_TOKEN))
						.accessDeniedHandler((request, response, accessDeniedException) ->
								authErrorResponseWriter.write(response, AuthErrorCode.AUTH_FORBIDDEN)))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, "/api/health", "/api/health/").permitAll()
						.requestMatchers(HttpMethod.HEAD, "/api/health", "/api/health/").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
						.requestMatchers(
								HttpMethod.POST,
								"/api/v1/auth/signup",
								"/api/v1/auth/login",
								"/api/v1/auth/password-reset",
								"/api/v1/auth/logout"
						).permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
			CustomUserDetailsService customUserDetailsService,
			AuthErrorResponseWriter authErrorResponseWriter) {
		return new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService, authErrorResponseWriter);
	}
}
