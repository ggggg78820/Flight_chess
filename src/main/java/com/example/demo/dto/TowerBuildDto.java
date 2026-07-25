package com.example.demo.dto;

/**
 * 「這一局裡建了一座塔」的資料格式，是 GameEndRequest.towers() 陣列裡的每一個元素。
 * 對應前端 app.js 的 state.towers 陣列裡的每一筆 {owner, type, pos, used}。
 *
 * type：塔種代號（cannon/freeze/radar），後端會用這個值去 Tower 資料表查對應的那一列。
 * pos：這座塔蓋在棋盤路徑上的第幾格。同一局裡，塔位可以重複出現多筆紀錄——
 *      因為一座塔的技能只會觸發一次，觸發後就失效，玩家/AI 可以在同一塔位再蓋一座新塔。
 * owner：這座塔是哪一方建造的，"player" 或 "ai"。
 * used：這座塔在遊戲結束前，技能有沒有被觸發過（true=已觸發並失效，false=蓋了但還沒被敵方經過）。
 */
public record TowerBuildDto(String type, Integer pos, String owner, boolean used) {
}
