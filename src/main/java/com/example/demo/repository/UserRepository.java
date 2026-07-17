package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 資料表的 Repository。
 * 除了繼承 JpaRepository 自動取得的基本方法之外，
 * 這裡多宣告了一個「自訂查詢方法」：findByUsername。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 依帳號名稱查詢使用者。
     *
     * 這是 Spring Data JPA 很方便的一個特色：只要照著命名規則寫方法名稱
     * （findBy + 欄位名稱，這裡對應 User 的 username 欄位），
     * Spring 就會在啟動時自動解析方法名稱、產生對應的 SQL 查詢（背後等同於
     * SELECT * FROM users WHERE username = ?），完全不用自己寫 SQL 或實作內容。
     *
     * 回傳型別是 Optional<User>，代表「可能查不到人」，
     * 呼叫端（例如 UserController）要用 .isPresent() / .map() / .orElse() 等方式安全地處理查無資料的情況，
     * 避免直接回傳 null 造成 NullPointerException。
     */
    Optional<User> findByUsername(String username);
}
