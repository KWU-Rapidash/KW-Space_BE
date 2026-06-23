package com.example.KW_SPACE.user.application;

import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserInfoResponse getMyInfo(String klasId) {
        User user = userRepository.findByKlasId(klasId)
                .orElseThrow(() -> new UserNotFoundException(klasId));

        return UserInfoResponse.from(user);
    }
}
