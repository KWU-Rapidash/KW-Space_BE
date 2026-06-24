package com.example.KW_SPACE.reservation.controller;

import com.example.KW_SPACE.auth.security.CustomUserDetails;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.example.KW_SPACE.reservation.dto.UserReservationResponse;
import com.example.KW_SPACE.reservation.service.ReservationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user/reservations")
public class UserReservationController {

	private final ReservationService reservationService;

	@GetMapping
	public List<UserReservationResponse> getMyReservations(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam(required = false) ReservationStatus status) {
		return reservationService.getUserReservations(userDetails.getId(), status);
	}
}
