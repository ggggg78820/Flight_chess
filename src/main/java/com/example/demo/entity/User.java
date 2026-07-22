package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 「使用者」實體（Entity）類別，對應資料庫裡的 users 資料表。
 *
 * 在 Spring Data JPA 裡，一個標了 @Entity 的類別就是一張資料表的 Java 版本：
 *   - 類別本身 = 一張表
 *   - 每個欄位（field） = 表裡的一個欄位（column）
 *   - 每一個 User 物件的實例 = 表裡的一列資料（row）
 * Hibernate（JPA 的實作）會自動幫我們把 Java 物件跟資料庫的資料互相轉換，
 * 不用自己手寫 INSERT / SELECT 等 SQL 語法。
 */
@Entity
@Table(name = "users") // 指定這個類別對應到資料庫裡名為 "users" 的表格
public class User {

    @Id // 標示這個欄位是主鍵（Primary Key）
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主鍵由資料庫自動遞增產生（AUTO_INCREMENT）
    private Long id;

    // 使用者帳號，不能是 null，而且整張表裡不能重複（unique = true）
    @Column(nullable = false, unique = true)
    private String username;

    // 舊資料庫相容欄位。系統已不提供密碼註冊或登入；保留映射是為了讓既有
    // password NOT NULL 欄位在 ddl-auto=update 環境中仍可正常新增玩家。
    // 此欄位沒有 getter/setter，也不會出現在任何 API。
    @Column(nullable = false)
    private String password = "";

    // 累計勝場數，預設 0。每次 /api/games/end 判定玩家獲勝時，GameService 會把這個數字 +1。
    @Column(name = "win_count")
    private Integer winCount = 0;

    // 累計敗場數，預設 0，邏輯同上（+1 的時機是判定玩家落敗時）。
    @Column(name = "lose_count")
    private Integer loseCount = 0;

    // 帳號建立時間，物件被 new 出來的當下就會設定成目前時間
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // 最後一次確認玩家身分的時間
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // JPA 規定 Entity 一定要有一個「無參數建構子」，讓 Hibernate 從資料庫讀資料時可以先 new 出空物件再逐一填值
    public User() {
    }

    // 方便在程式碼裡快速建立一個新使用者（例如 DataInitializer 建立預設帳號時）
    public User(String username) {
        this.username = username;
    }

    // ------------------------------------------------------------------
    // 以下都是標準的 getter / setter（存取子），是 JavaBean 的慣例寫法。
    // JPA、Jackson（JSON 轉換）等框架都是透過呼叫這些 getXxx() / setXxx() 方法
    // 來讀取或寫入欄位值，而不是直接存取 private 欄位，所以每個欄位都要一組對應的方法。
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getWinCount() {
        return winCount;
    }

    public void setWinCount(Integer winCount) {
        this.winCount = winCount;
    }

    public Integer getLoseCount() {
        return loseCount;
    }

    public void setLoseCount(Integer loseCount) {
        this.loseCount = loseCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}
