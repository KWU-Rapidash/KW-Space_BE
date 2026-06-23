package com.example.KW_SPACE.user.presentation;

import com.example.KW_SPACE.user.application.UserService;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** JWT 인가 적용 전 임시 엔드포인트다. #31에서 인증 정보 기반으로 전환한다. */
    @GetMapping({"", "/"})
    public UserInfoResponse getMyInfo(@RequestParam @NotBlank String klasId) {
        return userService.getMyInfo(klasId);
    }
}
