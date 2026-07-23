package com.example.demo.service;

import com.example.demo.dto.GameEndRequest;
import com.example.demo.dto.GameEndResponse;
import com.example.demo.dto.TowerBuildDto;
import com.example.demo.entity.Game;
import com.example.demo.entity.GameTower;
import com.example.demo.entity.Tower;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.GameTowerRepository;
import com.example.demo.repository.TowerRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GameServiceTests {

    private GameRepository gameRepository;
    private UserRepository userRepository;
    private TowerRepository towerRepository;
    private GameTowerRepository gameTowerRepository;
    private GameService gameService;
    private User player;

    @BeforeEach
    void setUp() {
        gameRepository = mock(GameRepository.class);
        userRepository = mock(UserRepository.class);
        towerRepository = mock(TowerRepository.class);
        gameTowerRepository = mock(GameTowerRepository.class);
        gameService = new GameService(gameRepository, userRepository, towerRepository, gameTowerRepository);

        player = new User("player1");
        player.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(player));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game game = invocation.getArgument(0);
            game.setId(99L);
            return game;
        });
    }

    @Test
    void validGameEndIsSaved() {
        Tower cannon = new Tower("砲塔", "cannon", "效果", "🏰");
        when(towerRepository.findByType("cannon")).thenReturn(Optional.of(cannon));

        GameEndResponse response = gameService.endGame(request(
                "WIN", 3, 1, 3, 2,
                List.of(new TowerBuildDto("cannon", 3, "player"))
        ));

        assertTrue(response.success());
        assertEquals(99L, response.gameId());
        assertEquals(1, response.winCount());
        assertEquals(1, response.towersRecorded());
        verify(gameRepository).save(any(Game.class));
        verify(gameTowerRepository).save(any(GameTower.class));
        verify(userRepository).save(player);
    }

    @Test
    void missingPlayerIsRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertCode("PLAYER_NOT_FOUND", validRequest());
        verifyNoInteractions(gameRepository, towerRepository, gameTowerRepository);
    }

    @Test
    void invalidResultIsRejected() {
        assertCode("INVALID_GAME_RESULT", request("DRAW", 2, 0, 2, 2, List.of()));
    }

    @Test
    void invalidMoveAndTurnCountsAreRejected() {
        assertCode("INVALID_TURN_COUNT", request("WIN", 0, 0, 0, 0, List.of()));
        assertCode("INVALID_PLAYER_MOVES", request("WIN", 2, 0, -1, 1, List.of()));
        assertCode("INVALID_AI_MOVES", request("WIN", 2, 0, 1, -1, List.of()));
    }

    @Test
    void invalidTowerTypeIsRejected() {
        assertCode("INVALID_TOWER_TYPE", request("WIN", 2, 1, 1, 1,
                List.of(new TowerBuildDto("laser", 3, "player"))));
    }

    @Test
    void invalidTowerOwnerIsRejected() {
        assertCode("INVALID_TOWER_OWNER", request("WIN", 2, 1, 1, 1,
                List.of(new TowerBuildDto("cannon", 3, "guest"))));
    }

    @Test
    void invalidTowerPositionIsRejected() {
        assertCode("INVALID_TOWER_POSITION", request("WIN", 2, 1, 1, 1,
                List.of(new TowerBuildDto("cannon", 4, "player"))));
    }

    @Test
    void duplicateTowerPositionIsRejected() {
        Tower cannon = new Tower("砲塔", "cannon", "效果", "🏰");
        when(towerRepository.findByType("cannon")).thenReturn(Optional.of(cannon));
        assertCode("DUPLICATE_TOWER_POSITION", request("WIN", 2, 2, 1, 1, List.of(
                new TowerBuildDto("cannon", 3, "player"),
                new TowerBuildDto("cannon", 3, "ai")
        )));
    }

    @Test
    void mismatchedTowerCountIsRejected() {
        assertCode("TOWER_COUNT_MISMATCH", request("WIN", 2, 1, 1, 1, List.of()));
    }

    @Test
    void databaseFailurePropagatesFromTransactionalMethod() throws Exception {
        Tower cannon = new Tower("砲塔", "cannon", "效果", "🏰");
        when(towerRepository.findByType("cannon")).thenReturn(Optional.of(cannon));
        when(gameTowerRepository.save(any(GameTower.class))).thenThrow(new RuntimeException("database failure"));

        assertThrows(RuntimeException.class, () -> gameService.endGame(request(
                "WIN", 2, 1, 1, 1,
                List.of(new TowerBuildDto("cannon", 3, "player"))
        )));
        verify(userRepository, never()).save(player);

        Method method = GameService.class.getMethod("endGame", GameEndRequest.class);
        assertNotNull(method.getAnnotation(Transactional.class), "結算方法必須由 Spring 交易管理");
    }

    private GameEndRequest validRequest() {
        return request("WIN", 2, 0, 1, 1, List.of());
    }

    private GameEndRequest request(String result, Integer turnCount, Integer towerCount,
                                   Integer playerMoves, Integer aiMoves, List<TowerBuildDto> towers) {
        return new GameEndRequest(1L, result, turnCount, towerCount, playerMoves, aiMoves, towers);
    }

    private void assertCode(String code, GameEndRequest request) {
        ApiException exception = assertThrows(ApiException.class, () -> gameService.endGame(request));
        assertEquals(code, exception.getCode());
        verifyNoInteractions(gameRepository, gameTowerRepository);
    }
}
