package com.echo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.echo.dto.AddFriendRequest;
import com.echo.dto.FriendResponse;
import com.echo.dto.UpdateFriendNicknameRequest;
import com.echo.security.UserPrincipal;
import com.echo.service.FriendService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 친구 목록 REST API.
 */
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

	private final FriendService friendService;

	/**
	 * 내 친구 목록을 반환한다.
	 */
	@GetMapping
	public List<FriendResponse> getFriends(@AuthenticationPrincipal UserPrincipal principal) {
		return friendService.getFriends(requireUserId(principal));
	}

	/**
	 * 친구를 추가한다.
	 */
	@PostMapping
	public FriendResponse addFriend(
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody AddFriendRequest request
	) {
		return executeFriendAction(() -> friendService.addFriend(requireUserId(principal), request.targetUserId()));
	}

	/**
	 * 친구를 삭제한다.
	 */
	@DeleteMapping("/{friendUserId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeFriend(
		@AuthenticationPrincipal UserPrincipal principal,
		@PathVariable Long friendUserId
	) {
		friendService.removeFriend(requireUserId(principal), friendUserId);
	}

	/**
	 * 친구 별칭을 변경한다.
	 */
	@PatchMapping("/{friendUserId}/nickname")
	public FriendResponse updateFriendNickname(
		@AuthenticationPrincipal UserPrincipal principal,
		@PathVariable Long friendUserId,
		@Valid @RequestBody UpdateFriendNicknameRequest request
	) {
		return executeFriendAction(() -> friendService.updateFriendNickname(
			requireUserId(principal),
			friendUserId,
			request.nickname()
		));
	}

	private Long requireUserId(UserPrincipal principal) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}

		return principal.getUserId();
	}

	private FriendResponse executeFriendAction(java.util.function.Supplier<FriendResponse> action) {
		try {
			return action.get();
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
	}

}
