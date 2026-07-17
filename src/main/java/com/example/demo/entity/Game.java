package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 「一局遊戲紀錄」實體類別，對應資料庫的 games 資料表。
 * 每次遊戲結束（GameService 的 endGame() 被呼叫）時，就會多存一筆這樣的紀錄，
 * 用來累積玩家的歷史戰績（可以透過 /api/games/history 撈出某個使用者最近幾局的紀錄）。
 */
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 這局遊戲屬於哪個使用者，對應 users.id（沒有用 @ManyToOne 建立正式的外鍵關聯，
    // 而是直接存 Long 型別的 id，屬於比較簡化的寫法，適合 demo 專案）
    @Column(name = "user_id")
    private Long userId;

    // 這局的結果，例如 "WIN" 或 "LOSE"（由前端 app.js 呼叫 API 時傳入）
    @Column(name = "result")
    private String result;

    // 這局總共玩了幾個回合（round，雙方各走一輪算一回合）
    @Column(name = "turn_count")
    private Integer turnCount;

    // 這局總共建了幾座塔
    @Column(name = "used_tower_count")
    private Integer usedTowerCount;

    // 玩家這局總共實際移動了幾次飛機
    @Column(name = "player_moves")
    private Integer playerMoves;

    // AI 這局總共實際移動了幾次飛機。
    // 注意：playerMoves 跟 aiMoves 不一定相等——如果玩家這一回合的移動就直接讓玩家獲勝，
    // AI 那個回合就沒有機會再動一次，兩者會差 1；一般情況下雙方輪流各走一次，兩者才會相等。
    @Column(name = "ai_moves")
    private Integer aiMoves;

    // 這筆紀錄的建立時間，new 出物件的當下自動設定
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // JPA 要求的無參數建構子
    public Game() {
    }

    // 方便在 GameService 裡一次把所有欄位帶入建立一筆新紀錄
    public Game(Long userId, String result, Integer turnCount, Integer usedTowerCount,
                Integer playerMoves, Integer aiMoves) {
        this.userId = userId;
        this.result = result;
        this.turnCount = turnCount;
        this.usedTowerCount = usedTowerCount;
        this.playerMoves = playerMoves;
        this.aiMoves = aiMoves;
    }

    // ------------------------------------------------------------------
    // 標準 getter / setter，供 JPA 存取資料庫欄位、供 Jackson 轉換成 JSON 回應之用
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getTurnCount() {
        return turnCount;
    }

    public void setTurnCount(Integer turnCount) {
        this.turnCount = turnCount;
    }

    public Integer getUsedTowerCount() {
        return usedTowerCount;
    }

    public void setUsedTowerCount(Integer usedTowerCount) {
        this.usedTowerCount = usedTowerCount;
    }

    public Integer getPlayerMoves() {
        return playerMoves;
    }

    public void setPlayerMoves(Integer playerMoves) {
        this.playerMoves = playerMoves;
    }

    public Integer getAiMoves() {
        return aiMoves;
    }

    public void setAiMoves(Integer aiMoves) {
        this.aiMoves = aiMoves;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
