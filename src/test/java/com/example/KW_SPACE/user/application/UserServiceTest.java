package com.example.KW_SPACE.user.application;

import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository);

    @Test
    void 학번으로_내_정보를_조회한다() {
        User user = new User("홍길동", "2022202015", "encoded-password", "010-1234-5678");
        given(userRepository.findByStudentNumber("2022202015")).willReturn(Optional.of(user));

        UserInfoResponse response = userService.getMyInfo("2022202015");

        assertThat(response.username()).isEqualTo("홍길동");
        assertThat(response.studentNumber()).isEqualTo("2022202015");
        assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
    }

    @Test
    void 없는_학번이면_예외를_던진다() {
        given(userRepository.findByStudentNumber("2022202015")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyInfo("2022202015"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
