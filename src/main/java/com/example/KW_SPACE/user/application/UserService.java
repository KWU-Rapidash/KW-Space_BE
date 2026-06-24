package com.example.KW_SPACE.user.application;

import com.example.KW_SPACE.reservation.domain.Reservation;
import com.example.KW_SPACE.reservation.domain.ReservationRepository;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import com.example.KW_SPACE.user.presentation.dto.PhoneUpdateResponse;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

	private static final String PHONE_NUMBER_PATTERN = "^010-\\d{4}-\\d{4}$";
	private static final String PHONE_NUMBER_UNIQUE_CONSTRAINT = "uk_users_phone_number";

	private final UserRepository userRepository;
	private final ReservationRepository reservationRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, ReservationRepository reservationRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.reservationRepository = reservationRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public UserInfoResponse getMyInfo(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(String.valueOf(userId)));

		return UserInfoResponse.from(user);
	}

	@Transactional
	public PhoneUpdateResponse updatePhoneNumber(Long userId, String phoneNumber) {
		if (phoneNumber == null || !phoneNumber.matches(PHONE_NUMBER_PATTERN)) {
			throw new UserException(UserErrorCode.USER_INVALID_PHONE_NUMBER);
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(String.valueOf(userId)));
		if (!phoneNumber.equals(user.getPhoneNumber()) && userRepository.existsByPhoneNumber(phoneNumber)) {
			throw new UserException(UserErrorCode.USER_DUPLICATED_PHONE_NUMBER);
		}

		user.changePhoneNumber(phoneNumber);

		return PhoneUpdateResponse.from(saveUser(user));
	}

	@Transactional
	public void updatePassword(Long userId, String currentPassword, String newPassword) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(String.valueOf(userId)));

		if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
			throw new UserException(UserErrorCode.USER_CURRENT_PASSWORD_MISMATCH);
		}

		user.changePasswordHash(passwordEncoder.encode(newPassword));
	}

	@Transactional
	public void withdraw(Long userId, String password) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(String.valueOf(userId)));

		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new UserException(UserErrorCode.USER_CURRENT_PASSWORD_MISMATCH);
		}

		List<Reservation> reservations = reservationRepository.findByUserId(userId);
		if (!reservations.isEmpty()) {
			reservationRepository.deleteAllInBatch(reservations);
		}

		userRepository.delete(user);
		userRepository.flush();
	}

	private User saveUser(User user) {
		try {
			return userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException exception) {
			if (isDuplicatedPhoneNumberViolation(exception)) {
				throw new UserException(UserErrorCode.USER_DUPLICATED_PHONE_NUMBER);
			}
			throw exception;
		}
	}

	private boolean isDuplicatedPhoneNumberViolation(DataIntegrityViolationException exception) {
		Throwable current = exception;
		while (current != null) {
			String message = current.getMessage();
			if (message != null
					&& message.toLowerCase(Locale.ROOT).contains(PHONE_NUMBER_UNIQUE_CONSTRAINT)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
