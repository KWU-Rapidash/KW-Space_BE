package com.example.KW_SPACE.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthErrorResponseWriter;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.auth.security.CustomUserDetails;
import com.example.KW_SPACE.auth.security.CustomUserDetailsService;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class JwtAuthenticationFilterTest {

	private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
	private final CustomUserDetailsService customUserDetailsService = mock(CustomUserDetailsService.class);
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
			jwtTokenProvider,
			customUserDetailsService,
			new AuthErrorResponseWriter()
	);

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void continuesWhenAccessTokenCookieDoesNotExist() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verifyNoInteractions(jwtTokenProvider, customUserDetailsService);
	}

	@Test
	void skipsPublicEndpointEvenWhenAccessTokenCookieExists() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/user");
		request.setCookies(new Cookie("accessToken", "invalid-token"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(200);
		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(jwtTokenProvider, customUserDetailsService);
	}

	@Test
	void setsAuthenticationWhenAccessTokenIsValid() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("accessToken", "valid-token"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		setUserId(user, 1L);
		CustomUserDetails userDetails = CustomUserDetails.from(user);
		given(jwtTokenProvider.parseAccessToken("valid-token"))
				.willReturn(new JwtAuthenticationClaims(1L, UserRole.USER, 0));
		given(customUserDetailsService.loadUserById(1L)).willReturn(userDetails);

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(userDetails);
	}

	@Test
	void writesAuthErrorWhenTokenIsInvalid() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("accessToken", "invalid-token"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		given(jwtTokenProvider.parseAccessToken("invalid-token"))
				.willThrow(new AuthException(AuthErrorCode.AUTH_INVALID_TOKEN));

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
		assertThat(response.getContentAsString()).contains("AUTH_INVALID_TOKEN");
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void writesAuthErrorWhenUserDoesNotExist() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("accessToken", "valid-token"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		given(jwtTokenProvider.parseAccessToken("valid-token"))
				.willReturn(new JwtAuthenticationClaims(1L, UserRole.USER, 0));
		given(customUserDetailsService.loadUserById(1L))
				.willThrow(new UsernameNotFoundException("User not found"));

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentAsString()).contains("AUTH_INVALID_TOKEN");
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void writesAuthErrorWhenTokenVersionDoesNotMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("accessToken", "valid-token"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		setUserId(user, 1L);
		CustomUserDetails userDetails = CustomUserDetails.from(user);
		given(jwtTokenProvider.parseAccessToken("valid-token"))
				.willReturn(new JwtAuthenticationClaims(1L, UserRole.USER, 1));
		given(customUserDetailsService.loadUserById(1L)).willReturn(userDetails);

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentAsString()).contains("AUTH_INVALID_TOKEN");
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
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
