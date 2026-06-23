package com.example.KW_SPACE.auth.jwt;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private static final String ROLE_CLAIM = "role";
	private static final String TOKEN_VERSION_CLAIM = "tokenVersion";

	private final JwtProperties jwtProperties;
	private final Clock clock;
	private final SecretKey secretKey;

	@Autowired
	public JwtTokenProvider(JwtProperties jwtProperties) {
		this(jwtProperties, Clock.systemUTC());
	}

	JwtTokenProvider(JwtProperties jwtProperties, Clock clock) {
		this.jwtProperties = jwtProperties;
		this.clock = clock;
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String createAccessToken(User user) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());

		return Jwts.builder()
				.subject(user.getId().toString())
				.claim(ROLE_CLAIM, user.getRole().name())
				.claim(TOKEN_VERSION_CLAIM, user.getTokenVersion())
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiresAt))
				.signWith(secretKey)
				.compact();
	}

	public JwtAuthenticationClaims parseAccessToken(String token) {
		Claims claims = parseClaims(token);

		try {
			return new JwtAuthenticationClaims(
					Long.valueOf(claims.getSubject()),
					UserRole.valueOf(claims.get(ROLE_CLAIM, String.class)),
					claims.get(TOKEN_VERSION_CLAIM, Integer.class)
			);
		} catch (RuntimeException exception) {
			throw new AuthException(AuthErrorCode.AUTH_INVALID_TOKEN);
		}
	}

	private Claims parseClaims(String token) {
		try {
			return Jwts.parser()
					.verifyWith(secretKey)
					.clock(() -> Date.from(clock.instant()))
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (ExpiredJwtException exception) {
			throw new AuthException(AuthErrorCode.AUTH_EXPIRED_TOKEN);
		} catch (IllegalArgumentException | JwtException exception) {
			throw new AuthException(AuthErrorCode.AUTH_INVALID_TOKEN);
		}
	}
}
