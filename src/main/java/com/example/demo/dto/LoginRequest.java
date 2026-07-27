package com.example.demo.dto;

/**
 * 「登入」API 的請求資料格式，對應 POST /api/auth/login 的 request body。
 * UserService.login() 會用 PasswordEncoder.matches(password, 資料庫裡的雜湊值) 驗證密碼，
 * 全程不會把資料庫存的雜湊字串跟這次傳入的明碼密碼一起回傳給任何人。
 */
public record LoginRequest(String username, String password) {
}
