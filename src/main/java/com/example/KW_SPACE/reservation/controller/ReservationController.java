package com.example.KW_SPACE.reservation.controller;

import com.example.KW_SPACE.auth.security.CustomUserDetails;
import com.example.KW_SPACE.reservation.dto.ReservationCreateRequest;
import com.example.KW_SPACE.reservation.dto.ReservationCreateResponse;
import com.example.KW_SPACE.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public ReservationCreateResponse create(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody ReservationCreateRequest request) {
		return reservationService.create(userDetails.getId(), request);
	}

	@DeleteMapping("/{reservationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long reservationId) {
		reservationService.cancel(userDetails.getId(), reservationId);
	}
}
