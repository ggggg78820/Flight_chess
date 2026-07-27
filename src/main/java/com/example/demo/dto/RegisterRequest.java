package com.example.demo.dto;

/**
 * 「註冊」API 的請求資料格式，對應 POST /api/auth/register 的 request body。
 * username／password 都是明碼字串（僅在這次 HTTP 請求的傳輸過程中存在），
 * UserService.register() 收到後會立刻用 PasswordEncoder 把 password 雜湊過才存進資料庫，
 * 不會有任何明碼密碼被寫進 users 資料表。
 */
public record RegisterRequest(String username, String password) {
}
