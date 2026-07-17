package com.example.demo.repository;

import com.example.demo.entity.Tower;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Tower 資料表的 Repository。
 * 跟 GameRepository 一樣，繼承 JpaRepository 就自動取得 save / findAll / findById / count 等方法，
 * 不需要自己寫任何實作。
 *
 * DataInitializer 用這個 Repository 在應用程式啟動時檢查/塞入預設的三種塔種子資料；
 * GameService 則在遊戲結束、需要把每座塔的建造紀錄存進 GameTower 時，
 * 用 findByType() 把前端送來的塔種代號（cannon/freeze/radar）對應回正確的 Tower 那一列。
 */
public interface TowerRepository extends JpaRepository<Tower, Long> {

    /**
     * 依塔種代號查詢。type 是穩定的機器可辨識代號（跟前端 app.js 的 towerTypes 物件 key 一致：
     * cannon/freeze/radar），不是給人看的顯示名稱（顯示名稱存在 name 欄位，例如「砲塔」）。
     */
    Optional<Tower> findByType(String type);
}
