package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserProfileResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    // 用真正的 BCryptPasswordEncoder（不是 mock），因為要驗證的行為就是「密碼真的有被雜湊、
    // 而且 matches() 真的能認出正確/錯誤的密碼」，這種行為 mock 掉就測不出意義了。
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserService userService = new UserService(userRepository, passwordEncoder);

    @BeforeEach
    void setUp() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registerCreatesHashedPasswordForNewUsername() {
        when(userRepository.findByUsername("玩家_01")).thenReturn(Optional.empty());

        UserProfileResponse response = userService.register(new RegisterRequest("  玩家_01  ", "pass1234"));

        assertEquals("玩家_01", response.username());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertNotEquals("pass1234", saved.getPassword(), "密碼必須雜湊過，不可以明碼存進 Entity");
        assertTrue(passwordEncoder.matches("pass1234", saved.getPassword()));
    }

    @Test
    void registerRejectsInvalidUsername() {
        ApiException error = assertThrows(ApiException.class,
                () -> userService.register(new RegisterRequest("a!", "pass1234")));

        assertEquals("INVALID_USERNAME", error.getCode());
        verifyNoInteractions(userRepository);
    }

    @Test
    void registerRejectsTooShortPassword() {
        ApiException error = assertThrows(ApiException.class,
                () -> userService.register(new RegisterRequest("player9", "123")));

        assertEquals("INVALID_PASSWORD", error.getCode());
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(new User("player1")));

        ApiException error = assertThrows(ApiException.class,
                () -> userService.register(new RegisterRequest("player1", "pass1234")));

        assertEquals("USERNAME_TAKEN", error.getCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        User existing = new User("player1");
        existing.setId(5L);
        existing.setPassword(passwordEncoder.encode("correct-pass"));
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(existing));

        UserProfileResponse response = userService.login(new LoginRequest("player1", "correct-pass"));

        assertEquals(5L, response.id());
        verify(userRepository).save(existing);
    }

    @Test
    void loginRejectsWrongPassword() {
        User existing = new User("player1");
        existing.setPassword(passwordEncoder.encode("correct-pass"));
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(existing));

        ApiException error = assertThrows(ApiException.class,
                () -> userService.login(new LoginRequest("player1", "wrong-pass")));

        assertEquals("INVALID_CREDENTIALS", error.getCode());
    }

    @Test
    void loginRejectsUnknownUsernameWithSameErrorAsWrongPassword() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> userService.login(new LoginRequest("ghost", "whatever")));

        assertEquals("INVALID_CREDENTIALS", error.getCode());
    }
}
