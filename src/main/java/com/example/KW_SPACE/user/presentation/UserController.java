package com.example.KW_SPACE.user.presentation;

import com.example.KW_SPACE.auth.security.CustomUserDetails;
import com.example.KW_SPACE.user.application.UserService;
import com.example.KW_SPACE.user.presentation.dto.PhoneUpdateRequest;
import com.example.KW_SPACE.user.presentation.dto.PhoneUpdateResponse;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PatchMapping("/phone")
    public PhoneUpdateResponse updatePhoneNumber(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PhoneUpdateRequest request
    ) {
        return userService.updatePhoneNumber(userDetails.getId(), request.phoneNumber());
    }
}
