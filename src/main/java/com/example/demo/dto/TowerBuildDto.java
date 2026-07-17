package com.example.demo.dto;

/**
 * 「這一局裡建了一座塔」的資料格式，是 GameEndRequest.towers() 陣列裡的每一個元素。
 * 對應前端 app.js 的 state.towers 陣列裡的每一筆 {owner, type, pos}。
 *
 * type：塔種代號（cannon/freeze/radar），後端會用這個值去 Tower 資料表查對應的那一列。
 * pos：這座塔蓋在棋盤路徑上的第幾格。
 * owner：這座塔是哪一方建造的，"player" 或 "ai"。
 */
public record TowerBuildDto(String type, Integer pos, String owner) {
}
