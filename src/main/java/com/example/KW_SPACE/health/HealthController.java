package com.example.KW_SPACE.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	@GetMapping({"/api/health", "/api/health/"})
	public Map<String, String> health() {
		return Map.of("status", "ok");
	}
}
