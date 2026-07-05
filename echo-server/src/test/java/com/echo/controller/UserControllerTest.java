package com.echo.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.echo.domain.AuthProvider;
import com.echo.domain.User;
import com.echo.repository.StoredFileRepository;
import com.echo.repository.UserRepository;
import com.echo.security.JwtTokenProvider;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private StoredFileRepository storedFileRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	private String accessToken;

	@BeforeEach
	void setUp() {
		storedFileRepository.deleteAll();
		userRepository.deleteAll();
		User user = User.builder()
			.email("profile-test@example.com")
			.displayName("Profile Tester")
			.provider(AuthProvider.LOCAL)
			.providerId("profile-test")
			.passwordHash("hash")
			.build();
		user = userRepository.save(user);
		accessToken = jwtTokenProvider.createAccessToken(user);
	}

	@Test
	void updateDisplayName_changesName() throws Exception {
		mockMvc.perform(patch("/api/users/me")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"displayName":"새 이름"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("새 이름"));
	}

	@Test
	void removeAvatar_returnsNullAvatarWhenDefault() throws Exception {
		mockMvc.perform(delete("/api/users/me/avatar")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.avatarFileId").value(nullValue()));
	}

	@Test
	void removeAvatar_clearsUploadedAvatar() throws Exception {
		MockMultipartFile image = new MockMultipartFile(
			"file",
			"avatar.png",
			MediaType.IMAGE_PNG_VALUE,
			new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 }
		);

		mockMvc.perform(multipart("/api/users/me/avatar")
				.file(image)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.avatarFileId").exists());

		mockMvc.perform(delete("/api/users/me/avatar")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.avatarFileId").value(nullValue()));
	}

}
