package com.example.demo.dto;

import java.time.LocalDateTime;

/**
 * 單筆歷史遊戲紀錄的資料格式，對應 GET /api/games/history 回傳陣列裡的每一個元素。
 * 內容目前跟 Game 這個 Entity 的欄位一模一樣，但刻意還是獨立定義一個 DTO，
 * 而不是直接把 List<Game> 回傳出去——這樣未來如果 Game 這個 Entity 內部欄位有調整
 * （例如新增跟其他表的關聯），API 對外的回應格式不會被迫跟著變動，Controller/前端都不用改。
 */
public record GameSummaryResponse(
        Long id,
        Long userId,
        String result,
        Integer turnCount,
        Integer usedTowerCount,
        Integer playerMoves,
        Integer aiMoves,
        LocalDateTime createdAt
) {
}
