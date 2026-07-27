package com.example.demo.security;

import com.example.demo.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;

/**
 * 登入狀態的共用小工具。這個專題沒有引入完整的 Spring Security，登入狀態單純用
 * Servlet 內建的 HttpSession 手動管理：AuthController.login() 成功時把 userId 存進
 * session，之後每個「需要先登入才能用」的 API（結算戰績、查詢自己的歷史紀錄、/auth/me）
 * 都呼叫這裡的 requireUserId() 從 session 讀回來，取代原本「相信前端傳來的 userId」
 * 的做法——即使有人手動竄改 API 請求內容，也無法冒充成別的玩家寫入戰績。
 */
public final class SessionUtil {

    /** session 裡存放已登入使用者 id 的屬性名稱，AuthController 建立/清除 session 時都用這個常數。 */
    public static final String USER_ID_ATTRIBUTE = "userId";

    private SessionUtil() {
    }

    /**
     * 從目前請求的 session 讀出已登入玩家的 userId；如果沒有 session、或 session 裡沒有
     * 存過 userId（代表還沒登入），就直接丟出 401 例外，交給 ApiExceptionHandler 統一處理，
     * 呼叫端（Controller）不用自己再判斷一次「有沒有登入」。
     */
    public static Long requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED", "請先登入才能執行這個操作");
        }
        return (Long) userId;
    }
}
