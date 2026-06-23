package com.example.KW_SPACE.auth.jwt;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthErrorResponseWriter;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.auth.security.CustomUserDetails;
import com.example.KW_SPACE.auth.security.CustomUserDetailsService;
import com.example.KW_SPACE.user.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
	private static final Set<String> PUBLIC_GET_PATHS = Set.of("/api/health", "/api/health/");
	private static final Set<String> PUBLIC_HEAD_PATHS = Set.of("/api/health", "/api/health/");
	private static final Set<String> PUBLIC_POST_PATHS = Set.of(
			"/api/v1/auth/signup",
			"/api/v1/auth/login",
			"/api/v1/auth/password-reset",
			"/api/v1/auth/logout"
	);
	private static final Set<String> PUBLIC_OPTIONS_PATHS = Set.of(
			"/api/health",
			"/api/health/",
			"/api/v1/auth/signup",
			"/api/v1/auth/login",
			"/api/v1/auth/password-reset",
			"/api/v1/auth/logout"
	);

	private final JwtTokenProvider jwtTokenProvider;
	private final CustomUserDetailsService customUserDetailsService;
	private final AuthErrorResponseWriter authErrorResponseWriter;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
			CustomUserDetailsService customUserDetailsService,
			AuthErrorResponseWriter authErrorResponseWriter) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.customUserDetailsService = customUserDetailsService;
		this.authErrorResponseWriter = authErrorResponseWriter;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return isPublicRequest(request, "GET", PUBLIC_GET_PATHS)
				|| isPublicRequest(request, "HEAD", PUBLIC_HEAD_PATHS)
				|| isPublicRequest(request, "POST", PUBLIC_POST_PATHS)
				|| isPublicRequest(request, "OPTIONS", PUBLIC_OPTIONS_PATHS);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String accessToken = extractAccessToken(request);
		if (accessToken == null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			authenticate(accessToken);
			filterChain.doFilter(request, response);
		} catch (AuthException exception) {
			SecurityContextHolder.clearContext();
			authErrorResponseWriter.write(response, exception.getErrorCode());
		}
	}

	private void authenticate(String accessToken) {
		JwtAuthenticationClaims claims = jwtTokenProvider.parseAccessToken(accessToken);
		CustomUserDetails userDetails = loadUserDetails(claims.userId());
		validateTokenClaims(claims, userDetails);

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				userDetails,
				null,
				userDetails.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private CustomUserDetails loadUserDetails(Long userId) {
		try {
			return customUserDetailsService.loadUserById(userId);
		} catch (UsernameNotFoundException exception) {
			throw new AuthException(AuthErrorCode.AUTH_INVALID_TOKEN);
		}
	}

	private void validateTokenClaims(JwtAuthenticationClaims claims, CustomUserDetails userDetails) {
		if (claims.tokenVersion() != userDetails.getTokenVersion() || claims.role() != userDetails.getRole()) {
			throw new AuthException(AuthErrorCode.AUTH_INVALID_TOKEN);
		}
	}

	private String extractAccessToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		return Arrays.stream(cookies)
				.filter(cookie -> ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName()))
				.findFirst()
				.map(Cookie::getValue)
				.orElse(null);
	}

	private boolean isPublicRequest(HttpServletRequest request, String method, Set<String> paths) {
		return method.equals(request.getMethod()) && paths.contains(resolveRequestPath(request));
	}

	private String resolveRequestPath(HttpServletRequest request) {
		String servletPath = request.getServletPath();
		if (servletPath != null && !servletPath.isBlank()) {
			return servletPath;
		}

		return request.getRequestURI();
	}
}
