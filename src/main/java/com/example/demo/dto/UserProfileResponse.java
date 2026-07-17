package com.example.demo.dto;

import java.time.LocalDateTime;

/**
 * 查詢個人資料 API（GET /api/users/profile）的回應資料格式。
 * 同樣完全不包含 password 欄位，安全性由「資料結構本身」保證，
 * 不依賴呼叫端記得要濾掉密碼欄位。
 */
public record UserProfileResponse(
        Long id,
        String username,
        Integer winCount,
        Integer loseCount,
        LocalDateTime createdAt,
        LocalDateTime lastLogin
) {
}
