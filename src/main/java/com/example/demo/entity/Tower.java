package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 「塔種」實體類別，對應資料庫的 towers 資料表。
 * 存放遊戲中可以建造的塔的基本設定資料（名稱、類型代號、效果說明、圖示）。
 *
 * 這張表現在是「顯示層」設定的真正來源：前端頁面載入時會呼叫 GET /api/towers，
 * 依照這張表回傳的名稱、效果說明、圖示動態產生建塔選單，不再把這些文字寫死在 app.js 裡，
 * 避免資料庫跟前端各自維護一份、內容兜不起來的風險。
 *
 * 但塔的「效果邏輯」本身（砲塔送回機坪、冰霧塔骰子 -1、雷達塔擋飛躍這些實際遊戲規則怎麼跑）
 * 仍然是寫在前端 static/app.js 的遊戲邏輯函式裡，這張表不會、也還沒有能力改變規則的實際運作
 * ——要做到那個程度，需要把 moveSide()/applyTowerEffects() 改寫成讀取資料庫參數的通用規則引擎，
 * 是比這張表大得多的重構，目前沒有做。type 欄位的值會跟前端遊戲邏輯裡比對用的字串
 * （"cannon"/"freeze"/"radar"）完全一致，讓 GameService 在遊戲結束時，能把前端送來的塔種代號
 * 對應回這裡的正確一列，進而建立 GameTower 關聯紀錄（見 GameTower 的說明）。
 */
@Entity
@Table(name = "towers")
public class Tower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 塔的名稱，例如「砲塔」
    private String name;

    // 塔種的穩定代號，例如 "cannon"／"freeze"／"radar"，跟前端 app.js 的 towerTypes 物件 key 一致
    private String type;

    // 效果說明文字，例如「對敵飛行棋造成傷害」
    @Column(name = "effect_desc")
    private String effectDesc;

    // 塔的圖示。欄位名稱雖然叫 icon_url（原本設計是存圖片網址），但目前實際存放的是
    // 一個 emoji 字元（例如 "🏰"），前端直接把它當文字內容顯示，不是真的去下載圖片資源。
    @Column(name = "icon_url")
    private String iconUrl;

    // 建立時間
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // JPA 要求的無參數建構子
    public Tower() {
    }

    // 方便一次帶入四個欄位建立一筆塔種資料（DataInitializer 就是這樣用的）
    public Tower(String name, String type, String effectDesc, String iconUrl) {
        this.name = name;
        this.type = type;
        this.effectDesc = effectDesc;
        this.iconUrl = iconUrl;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEffectDesc() {
        return effectDesc;
    }

    public void setEffectDesc(String effectDesc) {
        this.effectDesc = effectDesc;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
