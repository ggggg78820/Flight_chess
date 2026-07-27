package com.example.demo.dto;

import java.util.List;

/**
 * 「結束一局遊戲」API 的請求資料格式，對應 POST /api/games/end 的 request body。
 * 前端 app.js 的 syncGameEnd() 會在遊戲分出勝負時送出這份資料。
 *
 * 這裡故意沒有 userId 欄位：這局是誰玩的，一律由 GameController 從登入 session 讀出來
 * （見 SessionUtil.requireUserId()），不再相信前端請求裡宣稱的身分——不然只要有人把
 * request body 裡的 userId 改成別人的帳號 id，就能冒充幫別人加勝場/敗場。
 *
 * playerMoves/aiMoves：這一局裡玩家、AI 各自實際移動飛機的次數（不是回合數 turnCount，
 * 兩者通常相等，但如果最後一回合是某一方移動後直接分出勝負，另一方那一回合就沒有機會移動，
 * 兩個數字就會差 1）。
 * towers：這一局裡雙方總共建造的每一座塔（可能是 null 或空陣列，代表這局完全沒建塔）。
 * 後端會依照這份清單，在建立 Game 紀錄後，逐一建立對應的 GameTower 關聯紀錄。
 */
public record GameEndRequest(
        String result,
        Integer turnCount,
        Integer usedTowerCount,
        Integer playerMoves,
        Integer aiMoves,
        List<TowerBuildDto> towers
) {
}
