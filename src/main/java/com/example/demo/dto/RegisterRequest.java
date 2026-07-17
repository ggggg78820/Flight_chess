package com.example.demo.dto;

/**
 * 註冊 API 的請求資料格式，對應 POST /api/users/register 的 request body。
 */
public record RegisterRequest(String username, String password) {
}
