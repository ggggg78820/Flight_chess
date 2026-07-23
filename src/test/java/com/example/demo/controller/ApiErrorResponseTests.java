package com.example.demo.controller;

import com.example.demo.exception.ApiException;
import com.example.demo.exception.ApiExceptionHandler;
import com.example.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiErrorResponseTests {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void invalidUsernameReturnsUnifiedBadRequest() throws Exception {
        when(userService.identify(any())).thenThrow(new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_USERNAME",
                "玩家名稱格式不正確"
        ));

        mockMvc.perform(post("/api/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_USERNAME"))
                .andExpect(jsonPath("$.message").value("玩家名稱格式不正確"));
    }

    @Test
    void malformedJsonReturnsUnifiedBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }
}
