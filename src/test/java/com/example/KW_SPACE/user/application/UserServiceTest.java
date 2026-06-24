package com.example.KW_SPACE.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.example.KW_SPACE.reservation.domain.ReservationRepository;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import com.example.KW_SPACE.user.presentation.dto.PhoneUpdateResponse;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserService userService = new UserService(userRepository, reservationRepository, passwordEncoder);

    @Test
    void getMyInfoReturnsUserInfoByUserId() {
        User user = User.create("2022202015", "tester", "010-1234-5678", "encoded-password");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserInfoResponse response = userService.getMyInfo(1L);

        assertThat(response.name()).isEqualTo("tester");
        assertThat(response.klasId()).isEqualTo("2022202015");
        assertThat(response.phoneNumber()).isEqualTo("010-****-5678");
    }

    @Test
    void getMyInfoThrowsExceptionWhenUserIdDoesNotExist() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyInfo(1L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updatePhoneNumberChangesCurrentUsersPhoneNumber() {
        User user = User.create("2022202015", "tester", "010-1234-5678", "encoded-password");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByPhoneNumber("010-0000-1111")).willReturn(false);
        given(userRepository.saveAndFlush(user)).willReturn(user);

        PhoneUpdateResponse response = userService.updatePhoneNumber(1L, "010-0000-1111");

        assertThat(user.getPhoneNumber()).isEqualTo("010-0000-1111");
        assertThat(response.phoneNumber()).isEqualTo("010-0000-1111");
        verify(userRepository).existsByPhoneNumber("010-0000-1111");
    }

    @Test
    void updatePhoneNumberAllowsKeepingSamePhoneNumber() {
        User user = User.create("2022202015", "tester", "010-1234-5678", "encoded-password");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.saveAndFlush(user)).willReturn(user);

        PhoneUpdateResponse response = userService.updatePhoneNumber(1L, "010-1234-5678");

        assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
    }

    @Test
    void updatePhoneNumberThrowsExceptionWhenFormatIsInvalid() {
        assertThatThrownBy(() -> userService.updatePhoneNumber(1L, "01012345678"))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_INVALID_PHONE_NUMBER);
        verifyNoInteractions(userRepository);
    }

    @Test
    void updatePhoneNumberThrowsExceptionWhenPhoneNumberIsNull() {
        assertThatThrownBy(() -> userService.updatePhoneNumber(1L, null))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_INVALID_PHONE_NUMBER);
        verifyNoInteractions(userRepository);
    }

    @Test
    void updatePhoneNumberThrowsExceptionWhenPhoneNumberIsDuplicated() {
        User user = User.create("2022202015", "tester", "010-1234-5678", "encoded-password");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByPhoneNumber("010-0000-1111")).willReturn(true);

        assertThatThrownBy(() -> userService.updatePhoneNumber(1L, "010-0000-1111"))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_DUPLICATED_PHONE_NUMBER);
    }

    @Test
    void updatePhoneNumberMapsUniqueConstraintViolationToDuplicatedPhoneNumber() {
        User user = User.create("2022202015", "tester", "010-1234-5678", "encoded-password");
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "constraint [uk_users_phone_number]");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByPhoneNumber("010-0000-1111")).willReturn(false);
        given(userRepository.saveAndFlush(user)).willThrow(exception);

        assertThatThrownBy(() -> userService.updatePhoneNumber(1L, "010-0000-1111"))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_DUPLICATED_PHONE_NUMBER);
    }

    @Test
    void updatePasswordStoresEncodedNewPassword() {
        User user = User.create("2025404000", "tester", null, "encoded-password");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("current-password", "encoded-password")).willReturn(true);
        given(passwordEncoder.encode("new-password")).willReturn("new-encoded-password");

        userService.updatePassword(1L, "current-password", "new-password");

        assertThat(user.getPasswordHash()).isEqualTo("new-encoded-password");
        verify(passwordEncoder).matches("current-password", "encoded-password");
        verify(passwordEncoder).encode("new-password");
    }

    @Test
    void updatePasswordRejectsMismatchedCurrentPassword() {
        User user = User.create("2025404000", "tester", null, "encoded-password");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> userService.updatePassword(1L, "wrong-password", "new-password"))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_CURRENT_PASSWORD_MISMATCH));

        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        verify(passwordEncoder).matches("wrong-password", "encoded-password");
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void updatePasswordThrowsExceptionWhenUserDoesNotExist() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updatePassword(1L, "current-password", "new-password"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void withdrawDeletesUserAndReservationsWhenPasswordMatches() {
        User user = User.create("2025404000", "tester", null, "encoded-password");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("current-password", "encoded-password")).willReturn(true);
        given(reservationRepository.findByUserId(1L)).willReturn(List.of());

        userService.withdraw(1L, "current-password");

        verify(passwordEncoder).matches("current-password", "encoded-password");
        verify(reservationRepository).findByUserId(1L);
        verify(userRepository).delete(user);
        verify(userRepository).flush();
    }

    @Test
    void withdrawRejectsMismatchedPassword() {
        User user = User.create("2025404000", "tester", null, "encoded-password");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> userService.withdraw(1L, "wrong-password"))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_CURRENT_PASSWORD_MISMATCH));

        verify(passwordEncoder).matches("wrong-password", "encoded-password");
        verifyNoMoreInteractions(passwordEncoder);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void withdrawThrowsExceptionWhenUserDoesNotExist() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.withdraw(1L, "current-password"))
                .isInstanceOf(UserNotFoundException.class);
    }
}