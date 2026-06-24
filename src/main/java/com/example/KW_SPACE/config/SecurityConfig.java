package com.example.KW_SPACE.config;

import static com.example.KW_SPACE.config.AuthPublicEndpoints.PUBLIC_GET_PATHS;
import static com.example.KW_SPACE.config.AuthPublicEndpoints.PUBLIC_HEAD_PATHS;
import static com.example.KW_SPACE.config.AuthPublicEndpoints.PUBLIC_POST_PATHS;

import com.example.KW_SPACE.auth.cookie.AuthCookieProperties;
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
				.csrf(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint((request, response, authException) ->
								authErrorResponseWriter.write(response, AuthErrorCode.AUTH_INVALID_TOKEN))
						.accessDeniedHandler((request, response, accessDeniedException) ->
								authErrorResponseWriter.write(response, AuthErrorCode.AUTH_FORBIDDEN)))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS.toArray(String[]::new)).permitAll()
						.requestMatchers(HttpMethod.HEAD, PUBLIC_HEAD_PATHS.toArray(String[]::new)).permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
						.requestMatchers(HttpMethod.POST, PUBLIC_POST_PATHS.toArray(String[]::new)).permitAll()
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
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
			AuthErrorResponseWriter authErrorResponseWriter,
			AuthCookieProperties authCookieProperties) {
		return new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService, authErrorResponseWriter,
				authCookieProperties);
	}
}
