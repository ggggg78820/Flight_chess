package com.example.demo.controller;

import com.example.demo.dto.IdentifyRequest;
import com.example.demo.dto.IdentifyResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.RegisterResponse;
import com.example.demo.dto.UserProfileResponse;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 負責處理「使用者帳號」相關的 HTTP API：查詢個人資料、註冊、登入。
 *
 * 這一層現在很薄——每個方法只做三件事：接請求（用 DTO 當參數型別）、呼叫 UserService、
 * 依照結果組成適當的 ResponseEntity（決定 HTTP 狀態碼）。所有實際的商業邏輯（帳密比對、
 * 查重、資料組裝）都搬到 UserService 裡了，方便日後單獨測試商業邏輯，不用每次都要模擬 HTTP 請求。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 建構子注入 UserService，由 Spring 容器自動提供實例
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查詢單一使用者的個人資料。
     * @RequestParam Long userId：從網址的查詢字串取值，例如 GET /api/users/profile?userId=1
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@RequestParam Long userId) {
        return userService.getProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 註冊新帳號。
     * @RequestBody RegisterRequest request：Spring 會把前端傳來的 JSON body
     * 自動轉換成 RegisterRequest 這個 record，例如 {"username":"abc","password":"123"}。
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        RegisterResponse response = userService.register(request);
        // HTTP 400 Bad Request：告訴前端這次請求本身有問題（帳號重複），不是伺服器出錯
        return response.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * 登入（需要密碼），保留給未來如果要做真正的帳號驗證使用。
     * 目前前端沒有呼叫這支，改用下面的 identify() 做輕量的「表明身分」。
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return response.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * 表明身分：只輸入名字，讓後端知道「這次是誰在玩」，不需要密碼。
     * 名字第一次出現時後端會自動建立新帳號；名字已存在就直接沿用、回傳目前的累計勝敗。
     * 前端 app.js 的 identifyUser() 會呼叫這個端點，取代原本寫死登入 player1 的做法。
     */
    @PostMapping("/identify")
    public ResponseEntity<IdentifyResponse> identify(@RequestBody IdentifyRequest request) {
        return ResponseEntity.ok(userService.identify(request));
    }
}
