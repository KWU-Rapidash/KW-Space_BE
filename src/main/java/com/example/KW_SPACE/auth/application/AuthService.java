package com.example.KW_SPACE.auth.application;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.auth.klas.KlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthResult;
import com.example.KW_SPACE.auth.presentation.dto.SignupRequest;
import com.example.KW_SPACE.auth.presentation.dto.SignupResponse;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

	private final UserRepository userRepository;
	private final KlasAuthClient klasAuthClient;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, KlasAuthClient klasAuthClient, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.klasAuthClient = klasAuthClient;
		this.passwordEncoder = passwordEncoder;
	}

	public SignupResponse signup(SignupRequest request) {
		if (userRepository.existsByKlasId(request.klasId())) {
			throw new AuthException(AuthErrorCode.AUTH_DUPLICATED_KLAS_ID);
		}

		KlasAuthResult klasAuthResult = klasAuthClient.verify(request.klasId(), request.klasPassword());
		if (!klasAuthResult.authenticated()) {
			throw new AuthException(AuthErrorCode.AUTH_INVALID_KLAS_CREDENTIALS);
		}

		String passwordHash = passwordEncoder.encode(request.password());
		String name = resolveName(request.name(), klasAuthResult.name());
		User user = userRepository.save(User.create(request.klasId(), name, null, passwordHash));

		return SignupResponse.from(user);
	}

	private String resolveName(String requestName, String klasName) {
		if (klasName != null && !klasName.isBlank()) {
			return klasName;
		}

		return requestName;
	}
}
