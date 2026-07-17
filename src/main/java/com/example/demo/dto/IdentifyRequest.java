package com.example.demo.dto;

/**
 * 「表明身分」API 的請求資料格式，對應 POST /api/users/identify 的 request body。
 * 跟 LoginRequest 不同，這裡不需要密碼——目的只是讓後端知道「這次是誰在玩」，
 * 用來把這一局的勝敗紀錄掛在正確的使用者名下，不是真正的身分驗證。
 */
public record IdentifyRequest(String username) {
}
