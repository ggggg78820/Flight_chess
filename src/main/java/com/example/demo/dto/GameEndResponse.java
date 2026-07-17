package com.example.demo.dto;

/**
 * 「結束一局遊戲」API 的回應資料格式。
 * winCount/loseCount 是這局結算後、使用者最新的累計勝敗場次（找不到對應使用者時會是 null），
 * 前端會用這兩個欄位更新畫面上顯示的累計戰績，確保跟資料庫裡的數字一致。
 * towersRecorded 是這次成功寫入 GameTower 的筆數，方便呼叫端確認塔的紀錄有沒有真的存進去。
 */
public record GameEndResponse(boolean success, String message, Long gameId, Integer winCount, Integer loseCount, int towersRecorded) {
}
