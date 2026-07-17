package com.example.demo.dto;

/**
 * 登入 API 的回應資料格式，對應 POST /api/users/login 的回傳內容。
 *
 * 刻意「不」把 password 放進來——這個 DTO 只裝前端真正需要知道的欄位，
 * 從資料結構上就杜絕了「不小心把密碼欄位序列化回傳給前端」的可能性，
 * 比起在 Entity 上用 @JsonProperty(WRITE_ONLY) 排除欄位更直接、更不容易日後被改壞。
 *
 * success 為 false 時（帳號或密碼錯誤），userId/username/winCount/loseCount 都會是 null，
 * 只有 message 欄位會有內容，前端可以直接用 success 判斷這次登入是否成功。
 */
public record LoginResponse(
        boolean success,
        Long userId,
        String username,
        Integer winCount,
        Integer loseCount,
        String message
) {
}
