package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 「某一局遊戲中，在哪個棋盤位置建了哪種塔」的關聯實體類別，對應資料庫的 game_towers 資料表。
 *
 * 這是一張「多對多的中介表（join table）」的概念延伸：
 * 一局 Game 可以建很多座塔、一種 Tower 也可能在很多局裡被建造，
 * GameTower 就是用來記錄「某一局」+「某一種塔」+「蓋在棋盤第幾格」+「是哪一方蓋的」這樣的組合關係。
 *
 * 這張表由 GameService.endGame() 在遊戲結束時寫入：前端 app.js 會把整局收集到的
 * state.towers 陣列（每座塔的類型、棋盤位置、擁有者）一起送過來，後端逐一轉成 GameTower 存進資料庫。
 */
@Entity
@Table(name = "game_towers")
public class GameTower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne：多筆 GameTower 可以對應到同一個 Game（多對一）
    // @JoinColumn 指定資料庫裡實際存外鍵的欄位名稱是 game_id
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    // @ManyToOne：多筆 GameTower 可以對應到同一種 Tower（多對一）
    @ManyToOne
    @JoinColumn(name = "tower_id")
    private Tower tower;

    // 這座塔蓋在棋盤路徑上的第幾格（對應 app.js 裡棋盤格子的 index）
    @Column(name = "board_position")
    private Integer boardPosition;

    // 這座塔是哪一方建造的，值是 "player" 或 "ai"（跟前端 state.towers 裡的 owner 欄位一致）
    @Column(name = "owner")
    private String owner;

    // 建立時間
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // JPA 要求的無參數建構子
    public GameTower() {
    }

    // 方便一次帶入「哪一局」「哪種塔」「蓋在哪一格」「哪一方蓋的」四個關聯資訊來建立一筆紀錄
    public GameTower(Game game, Tower tower, Integer boardPosition, String owner) {
        this.game = game;
        this.tower = tower;
        this.boardPosition = boardPosition;
        this.owner = owner;
    }

    // ------------------------------------------------------------------
    // 標準 getter / setter
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Tower getTower() {
        return tower;
    }

    public void setTower(Tower tower) {
        this.tower = tower;
    }

    public Integer getBoardPosition() {
        return boardPosition;
    }

    public void setBoardPosition(Integer boardPosition) {
        this.boardPosition = boardPosition;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
