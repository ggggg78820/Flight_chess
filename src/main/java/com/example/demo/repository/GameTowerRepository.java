package com.example.demo.repository;

import com.example.demo.entity.GameTower;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * GameTower 資料表（game_towers，記錄某一局在哪個位置建了哪種塔）的 Repository。
 * 跟其他 Repository 一樣，繼承 JpaRepository 就自動取得 save / findAll / findById 等方法。
 * 由 GameService.endGame() 使用，在遊戲結束時把整局建過的塔逐一存進資料庫。
 */
public interface GameTowerRepository extends JpaRepository<GameTower, Long> {
}
