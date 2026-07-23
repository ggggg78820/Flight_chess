package com.example.demo.service;

import com.example.demo.dto.IdentifyRequest;
import com.example.demo.dto.IdentifyResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository);

    @Test
    void identifyTrimsAndLoadsExistingPlayer() {
        User player = new User("玩家_01");
        player.setId(7L);
        when(userRepository.findByUsername("玩家_01")).thenReturn(Optional.of(player));

        IdentifyResponse response = userService.identify(new IdentifyRequest("  玩家_01  "));

        assertEquals(7L, response.userId());
        assertEquals("玩家_01", response.username());
        assertFalse(response.isNewUser());
        verify(userRepository).findByUsername("玩家_01");
    }

    @Test
    void identifyRejectsInvalidPlayerName() {
        ApiException error = assertThrows(
                ApiException.class,
                () -> userService.identify(new IdentifyRequest("a!"))
        );

        assertTrue(error.getMessage().contains("2～20"));
        assertEquals("INVALID_USERNAME", error.getCode());
        verifyNoInteractions(userRepository);
    }
}
