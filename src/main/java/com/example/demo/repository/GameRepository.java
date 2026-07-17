package com.example.demo.repository;

import com.example.demo.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Game 資料表的 Repository（資料存取介面）。
 *
 * JpaRepository<Game, Long> 這樣繼承，就自動免費取得一整套操作 games 表格的方法，
 * 例如：save()（新增/更新）、findAll()（查全部）、findById()（依主鍵查詢）、
 * deleteById()（刪除）、count()（計算筆數）……等等。
 *
 * 這個 Repository 被 GameService 使用，負責儲存每一局的遊戲紀錄、查詢某個使用者最近的對局。
 */
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * 查詢某個使用者最近 5 局的遊戲紀錄，依建立時間新到舊排序。
     *
     * 方法名稱本身就是查詢內容，Spring Data JPA 會照著命名規則自動組出對應的 SQL：
     *   Top5              -> 限制最多回傳 5 筆（LIMIT 5）
     *   ByUserId          -> WHERE user_id = ?
     *   OrderByCreatedAtDesc -> ORDER BY created_at DESC
     * 完全不用自己寫 SQL 或分頁邏輯。
     */
    List<Game> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}
