package com.example.demo.dto;

/**
 * 「表明身分」API 的回應資料格式。
 * isNewUser 標記這個名字是不是第一次出現（後端剛幫他建立新帳號），
 * 前端可以依此顯示「歡迎新朋友」或「歡迎回來」之類的訊息（目前沒有特別區分，但保留這個資訊）。
 */
public record IdentifyResponse(Long userId, String username, Integer winCount, Integer loseCount, boolean isNewUser) {
}
