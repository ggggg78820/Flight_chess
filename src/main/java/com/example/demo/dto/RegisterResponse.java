package com.example.demo.dto;

/**
 * 註冊 API 的回應資料格式。
 * success 為 false 時（帳號已存在），userId 會是 null，message 說明失敗原因。
 */
public record RegisterResponse(boolean success, Long userId, String message) {
}
