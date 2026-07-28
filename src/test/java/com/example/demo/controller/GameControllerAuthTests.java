package com.example.demo.controller;

import com.example.demo.exception.ApiExceptionHandler;
import com.example.demo.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 驗證 GameController 的兩支 API 在「沒有登入」的情況下，一定會被 SessionUtil.requireUserId()
 * 擋下來（401），不會把 GameService 找出來執行——這是這次把「戰績只能寫入登入者」這個規則
 * 從 GameEndRequest.userId 改成從 session 讀取之後，最該被測試覆蓋的行為，之前完全沒有測試
 * 直接驗證過這一點。
 */
class GameControllerAuthTests {

    private MockMvc mockMvc;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = mock(GameService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new GameController(gameService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void endGameWithoutSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/games/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"WIN\",\"turnCount\":1,\"usedTowerCount\":0,\"playerMoves\":1,\"aiMoves\":1,\"towers\":[]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"));
        verifyNoInteractions(gameService);
    }

    @Test
    void historyWithoutSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/games/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"));
        verifyNoInteractions(gameService);
    }
}
