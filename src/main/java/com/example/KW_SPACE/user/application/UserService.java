package com.example.KW_SPACE.user.application;

import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserInfoResponse getMyInfo(String klasId) {
        User user = userRepository.findByKlasId(klasId)
                .orElseThrow(() -> new UserNotFoundException(klasId));

        return UserInfoResponse.from(user);
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
}
