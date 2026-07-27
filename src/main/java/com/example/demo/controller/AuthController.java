package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserProfileResponse;
import com.example.demo.security.SessionUtil;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 負責帳號註冊、登入、登出，以及查詢「目前登入者」是誰。
 *
 * 這個專題沒有引入完整的 Spring Security，登入狀態單純用 Servlet 內建的 HttpSession
 * 手動管理：login()/register() 成功時把 userId 存進 session，瀏覽器會自動收到一組 session
 * cookie，之後每次呼叫同一個網域的 API，瀏覽器都會自動帶上這組 cookie，後端就能透過
 * SessionUtil.requireUserId() 讀回「這次是誰在呼叫」，不需要每支 API 都要求前端手動傳 userId
 * ——這正是取代舊版「/api/users/identify 只憑名字信任身分」做法的關鍵。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 註冊新帳號。成功後直接建立登入 session（等同註冊完自動幫玩家登入一次），
     * 前端不需要註冊完再多呼叫一次 /login。
     */
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        UserProfileResponse profile = userService.register(request);
        startSession(httpRequest, profile.id());
        return ResponseEntity.ok(profile);
    }

    /** 登入：帳密驗證通過後建立 session，瀏覽器之後的請求都會自動帶上這個 session 的 cookie。 */
    @PostMapping("/login")
    public ResponseEntity<UserProfileResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        UserProfileResponse profile = userService.login(request);
        startSession(httpRequest, profile.id());
        return ResponseEntity.ok(profile);
    }

    /** 登出：把目前的 session 整個清掉（如果本來就沒有 session，什麼事都不做）。 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 查詢「目前登入者」的個人資料。前端頁面一載入就會呼叫這支 API，
     * 藉此得知瀏覽器裡還留著有效的登入 session（重新整理頁面後不會被登出），
     * 沒有登入時會回傳 401（由 SessionUtil.requireUserId() 拋出），前端就知道要顯示登入畫面。
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(HttpServletRequest httpRequest) {
        Long userId = SessionUtil.requireUserId(httpRequest);
        return userService.getProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 建立新 session 並把 userId 存進去；true 代表「沒有就新建一個」，
    // 這裡一定是剛驗證通過帳密，所以直接允許建立新的 session。
    private void startSession(HttpServletRequest httpRequest, Long userId) {
        httpRequest.getSession(true).setAttribute(SessionUtil.USER_ID_ATTRIBUTE, userId);
    }
}
