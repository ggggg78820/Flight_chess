package com.example.demo.controller;

import com.example.demo.dto.IdentifyRequest;
import com.example.demo.dto.IdentifyResponse;
import com.example.demo.dto.UserProfileResponse;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 負責玩家識別與個人戰績查詢。系統只使用玩家名稱，不提供密碼註冊或登入。
 *
 * 這一層現在很薄——每個方法只做三件事：接請求（用 DTO 當參數型別）、呼叫 UserService、
 * 依照結果組成適當的 ResponseEntity（決定 HTTP 狀態碼）。玩家名稱驗證與資料組裝
 * 都放在 UserService，方便單獨測試商業邏輯，不用每次都模擬 HTTP 請求。
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
     * 表明身分：只輸入名字，讓後端知道「這次是誰在玩」，不需要密碼。
     * 名字第一次出現時後端會自動建立新帳號；名字已存在就直接沿用、回傳目前的累計勝敗。
     * 前端 app.js 的 identifyUser() 會呼叫這個端點，取代原本寫死 player1 的做法。
     */
    @PostMapping("/identify")
    public ResponseEntity<IdentifyResponse> identify(@RequestBody IdentifyRequest request) {
        return ResponseEntity.ok(userService.identify(request));
    }
}
