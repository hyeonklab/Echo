package com.echo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
class FileControllerTest {

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
			.email("file-test@example.com")
			.displayName("File Tester")
			.provider(AuthProvider.LOCAL)
			.providerId("file-test")
			.passwordHash("hash")
			.build();
		user = userRepository.save(user);
		accessToken = jwtTokenProvider.createAccessToken(user);
	}

	@Test
	void uploadMessageFiles_returnsFileIds() throws Exception {
		MockMultipartFile image = new MockMultipartFile(
			"files",
			"photo.png",
			MediaType.IMAGE_PNG_VALUE,
			new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 }
		);

		mockMvc.perform(multipart("/api/files")
				.file(image)
				.param("purpose", "MESSAGE")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.files[0].id").exists())
			.andExpect(jsonPath("$.files[0].originalName").value("photo.png"));
	}

	@Test
	void uploadMessageFiles_acceptsJpegWithGenericContentType() throws Exception {
		MockMultipartFile image = new MockMultipartFile(
			"files",
			"photo.jpg",
			MediaType.APPLICATION_OCTET_STREAM_VALUE,
			new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }
		);

		mockMvc.perform(multipart("/api/files")
				.file(image)
				.param("purpose", "MESSAGE")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.files[0].contentType").value("image/jpeg"));
	}

}
