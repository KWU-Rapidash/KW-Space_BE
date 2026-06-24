package com.example.KW_SPACE.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

	private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	private static final String DIFFERENT_SECRET = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
	private static final Instant NOW = Instant.parse("2026-06-24T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
			new JwtProperties(SECRET, Duration.ofHours(1)),
			CLOCK
	);

	@Test
	void createsAccessTokenWithMinimalClaims() {
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		setUserId(user, 1L);

		String token = jwtTokenProvider.createAccessToken(user);

		SecretKey secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		Claims claims = Jwts.parser()
				.verifyWith(secretKey)
				.clock(() -> Date.from(NOW))
				.build()
				.parseSignedClaims(token)
				.getPayload();

		assertThat(claims.getSubject()).isEqualTo("1");
		assertThat(claims.get("role", String.class)).isEqualTo(UserRole.USER.name());
		assertThat(claims.get("tokenVersion", Integer.class)).isZero();
		assertThat(claims.getIssuedAt()).isEqualTo(Date.from(NOW));
		assertThat(claims.getExpiration()).isEqualTo(Date.from(NOW.plus(Duration.ofHours(1))));
		assertThat(claims).doesNotContainKeys("password", "passwordHash", "klasPassword", "klasId");
	}

	@Test
	void parsesAccessToken() {
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		setUserId(user, 1L);
		String token = jwtTokenProvider.createAccessToken(user);

		JwtAuthenticationClaims claims = jwtTokenProvider.parseAccessToken(token);

		assertThat(claims.userId()).isEqualTo(1L);
		assertThat(claims.role()).isEqualTo(UserRole.USER);
		assertThat(claims.tokenVersion()).isZero();
	}

	@Test
	void rejectsExpiredToken() {
		JwtTokenProvider expiredProvider = new JwtTokenProvider(
				new JwtProperties(SECRET, Duration.ofMillis(1)),
				CLOCK
		);
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		setUserId(user, 1L);
		String token = expiredProvider.createAccessToken(user);
		JwtTokenProvider laterProvider = new JwtTokenProvider(
				new JwtProperties(SECRET, Duration.ofHours(1)),
				Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC)
		);

		assertThatThrownBy(() -> laterProvider.parseAccessToken(token))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_EXPIRED_TOKEN));
	}

	@Test
	void rejectsTamperedToken() {
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		setUserId(user, 1L);
		String token = jwtTokenProvider.createAccessToken(user) + "tampered";

		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(token))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_TOKEN));
	}

	@Test
	void rejectsBlankToken() {
		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(" "))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_TOKEN));
	}

	@Test
	void rejectsTokenSignedWithDifferentSecret() {
		SecretKey differentSecretKey = Keys.hmacShaKeyFor(DIFFERENT_SECRET.getBytes(StandardCharsets.UTF_8));
		String token = Jwts.builder()
				.subject("1")
				.claim("role", UserRole.USER.name())
				.claim("tokenVersion", 0)
				.issuedAt(Date.from(NOW))
				.expiration(Date.from(NOW.plus(Duration.ofHours(1))))
				.signWith(differentSecretKey)
				.compact();

		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(token))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_TOKEN));
	}

	@Test
	void rejectsTokenWithoutExpiration() {
		SecretKey secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		String token = Jwts.builder()
				.subject("1")
				.claim("role", UserRole.USER.name())
				.claim("tokenVersion", 0)
				.issuedAt(Date.from(NOW))
				.signWith(secretKey)
				.compact();

		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(token))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_TOKEN));
	}

	@Test
	void rejectsTokenWithoutSubject() {
		String token = tokenWithClaims(null, UserRole.USER.name(), 0);

		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(token))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_TOKEN));
	}

	@Test
	void rejectsTokenWithNonNumericSubject() {
		String token = tokenWithClaims("not-a-number", UserRole.USER.name(), 0);

		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(token))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_TOKEN));
	}

	@Test
	void rejectsTokenWithoutRole() {
		String token = tokenWithClaims("1", null, 0);

		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(token))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_TOKEN));
	}

	@Test
	void rejectsTokenWithUnknownRole() {
		String token = tokenWithClaims("1", "UNKNOWN", 0);

		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(token))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_TOKEN));
	}

	@Test
	void rejectsTokenWithoutTokenVersion() {
		String token = tokenWithClaims("1", UserRole.USER.name(), null);

		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(token))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_TOKEN));
	}

	@Test
	void rejectsTokenWithNonIntegerTokenVersion() {
		String token = tokenWithClaims("1", UserRole.USER.name(), "not-a-version");

		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(token))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_TOKEN));
	}

	private String tokenWithClaims(String subject, String role, Object tokenVersion) {
		SecretKey secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		var builder = Jwts.builder()
				.issuedAt(Date.from(NOW))
				.expiration(Date.from(NOW.plus(Duration.ofHours(1))));
		if (subject != null) {
			builder.subject(subject);
		}
		if (role != null) {
			builder.claim("role", role);
		}
		if (tokenVersion != null) {
			builder.claim("tokenVersion", tokenVersion);
		}

		return builder.signWith(secretKey).compact();
	}

	private void setUserId(User user, Long id) {
		try {
			var field = User.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(user, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
