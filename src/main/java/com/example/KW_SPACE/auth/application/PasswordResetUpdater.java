package com.example.KW_SPACE.auth.application;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetUpdater {

	private final UserRepository userRepository;

	public PasswordResetUpdater(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public void update(String klasId, String passwordHash) {
		User user = userRepository.findByKlasId(klasId)
				.orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_USER_NOT_FOUND));

		user.resetPassword(passwordHash);
	}
}
