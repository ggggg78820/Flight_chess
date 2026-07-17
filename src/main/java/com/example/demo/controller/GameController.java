package com.example.demo.controller;

import com.example.demo.dto.GameEndRequest;
import com.example.demo.dto.GameEndResponse;
import com.example.demo.dto.GameSummaryResponse;
import com.example.demo.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 負責處理「遊戲局」相關的 HTTP API：結束一局、查詢歷史紀錄。
 * 跟 UserController 一樣，這一層只處理 HTTP 細節，商業邏輯都在 GameService 裡。
 *
 * 這個 Controller 會被前端 app.js 在兩個時機呼叫：
 *   1. 遊戲分出勝負時 -> POST /api/games/end（唯一真正寫入資料庫的地方）
 *   2. 頁面載入、每次表明身分、每局結束後 -> GET /api/games/history?userId=，
 *      顯示「最近戰績」列表
 *
 * 原本這裡還有一個 POST /api/games/start，但它不寫資料庫、也沒有回傳 gameId 讓
 * /games/end 對應回去，形同一個沒有實際職責的端點，所以移除了。這個專題只需要保存
 * 最終戰績，不需要追蹤「開始但沒完成」的對局，因此不採用「先建立 IN_PROGRESS 狀態的
 * Game、結束時再用 PATCH 更新」這種更完整、但對目前需求來說過度設計的生命週期。
 */
@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;

    // 建構子注入 GameService，由 Spring 容器自動提供實例
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    // 查詢某個使用者最近 5 局的歷史紀錄。userId 是必填的查詢字串參數，
    // 因為「歷史戰績」這個功能本來就是「某個玩家自己的」戰績，不是全站所有人的對局混在一起。
    @GetMapping("/games/history")
    public List<GameSummaryResponse> getRecentHistory(@RequestParam Long userId) {
        return gameService.getRecentHistory(userId);
    }

    // 結束一局遊戲：存進資料庫、更新使用者累計勝敗，回傳最新結算資訊
    @PostMapping("/games/end")
    public ResponseEntity<GameEndResponse> endGame(@RequestBody GameEndRequest request) {
        return ResponseEntity.ok(gameService.endGame(request));
    }
}
