package com.example.KW_SPACE.config;

import com.example.KW_SPACE.auth.cookie.AuthCookieProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthCookieProperties.class)
public class AuthCookieConfig {
}
