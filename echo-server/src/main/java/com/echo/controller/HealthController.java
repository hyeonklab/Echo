package com.echo.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 상태 확인용 엔드포인트.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

	@GetMapping
	public Map<String, String> health() {
		return Map.of("status", "ok");
	}

}
