package com.example.demo.dto;

/**
 * 「表明身分」API 的請求資料格式，對應 POST /api/users/identify 的 request body。
 * 玩家只需輸入名稱，後端便能載入既有戰績或自動建立新玩家。
 * 用來把這一局的勝敗紀錄掛在正確的使用者名下，不是真正的身分驗證。
 */
public record IdentifyRequest(String username) {
}
