package com.example.KW_SPACE.user.application;

import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import com.example.KW_SPACE.user.presentation.dto.PhoneUpdateResponse;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final String PHONE_NUMBER_PATTERN = "^010-\\d{4}-\\d{4}$";

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserInfoResponse getMyInfo(String klasId) {
        User user = userRepository.findByKlasId(klasId)
                .orElseThrow(() -> new UserNotFoundException(klasId));

        return UserInfoResponse.from(user);
    }

    @Transactional
    public PhoneUpdateResponse updatePhoneNumber(Long userId, String phoneNumber) {
        if (!phoneNumber.matches(PHONE_NUMBER_PATTERN)) {
            throw new UserException(UserErrorCode.USER_INVALID_PHONE_NUMBER);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(String.valueOf(userId)));
        if (!phoneNumber.equals(user.getPhoneNumber()) && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new UserException(UserErrorCode.USER_DUPLICATED_PHONE_NUMBER);
        }

        user.changePhoneNumber(phoneNumber);

        return PhoneUpdateResponse.from(user);
    }
}
