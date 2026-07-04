package com.echo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.echo.dto.MessageHistoryResponse;
import com.echo.dto.MessageResponse;
import com.echo.dto.SendMessageRequest;
import com.echo.security.UserPrincipal;
import com.echo.service.MessageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 메시지 REST API.
 */
@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class MessageController {

	private final MessageService messageService;

	/**
	 * 채팅방 메시지 히스토리를 반환한다.
	 */
	@GetMapping
	public MessageHistoryResponse getMessages(
		@AuthenticationPrincipal UserPrincipal principal,
		@PathVariable Long roomId,
		@RequestParam(required = false) Long before,
		@RequestParam(required = false) Integer limit
	) {
		return executeMessageAction(
			() -> messageService.getMessages(roomId, requireUserId(principal), before, limit)
		);
	}

	/**
	 * 채팅방에 메시지를 전송한다.
	 */
	@PostMapping
	public MessageResponse sendMessage(
		@AuthenticationPrincipal UserPrincipal principal,
		@PathVariable Long roomId,
		@Valid @RequestBody SendMessageRequest request
	) {
		return executeMessageAction(
			() -> messageService.sendMessage(roomId, requireUserId(principal), request)
		);
	}

	private Long requireUserId(UserPrincipal principal) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}

		return principal.getUserId();
	}

	private <T> T executeMessageAction(java.util.function.Supplier<T> action) {
		try {
			return action.get();
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
	}

}
