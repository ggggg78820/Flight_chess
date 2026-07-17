package com.example.demo.config;

import com.example.demo.entity.Tower;
import com.example.demo.entity.User;
import com.example.demo.repository.TowerRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 「種子資料」初始化器：讓這個 demo 專案一啟動就有基本資料可用，不用手動去資料庫塞資料。
 * @Configuration 表示這個類別會被 Spring 掃描，裡面定義的 @Bean 方法會被拿去註冊成元件。
 */
@Configuration
public class DataInitializer {

    /**
     * CommandLineRunner 是 Spring Boot 提供的介面：
     * 只要把它註冊成一個 Bean，Spring Boot 就會在「應用程式完全啟動之後」自動執行它的 run() 內容一次。
     * 很適合拿來做「啟動時的初始化工作」，例如這裡的塞入預設帳號、預設塔種資料。
     *
     * 方法參數 userRepository、towerRepository 是由 Spring 自動注入（Dependency Injection）進來的，
     * 不需要自己 new，Spring 容器會找到對應的 Bean 傳進來。
     */
    @Bean
    public CommandLineRunner initData(UserRepository userRepository, TowerRepository towerRepository) {
        return args -> {
            // 如果 users 資料表目前是空的，才建立一個預設帳號（避免每次重啟都重複新增）
            // 帳號：player1 / 密碼：password —— 前端 app.js 會用這組帳密自動登入示範
            if (userRepository.count() == 0) {
                userRepository.save(new User("player1", "password"));
            }

            // 塞入（或修正）三種塔的基本資料，type 欄位是跟前端遊戲邏輯裡比對用的字串
            // 完全一致的穩定代號（cannon/freeze/radar）——GameService 結算遊戲時，
            // 就是靠這個代號把前端送來的建塔紀錄對應回這裡的正確一列，藉此建立 GameTower 關聯；
            // 前端頁面載入時也會呼叫 GET /api/towers，直接拿 name/effectDesc/icon 這幾個欄位
            // 動態產生建塔選單，所以這裡塞的文字內容就是玩家實際會在畫面上看到的內容。
            // 用「依 type 查有沒有、沒有才新增」而不是單純檢查 towerRepository.count()==0，
            // 是為了讓這三筆關鍵資料每次啟動都能自我修正成正確狀態，不管資料庫先前存的是什麼。
            List<String> validTypes = List.of("cannon", "freeze", "radar");
            upsertTowerType(towerRepository, "cannon", "砲塔", "敵方飛機經過，立即回機坪", "🏰");
            upsertTowerType(towerRepository, "freeze", "冰霧塔", "敵方下回合骰子 -1", "❄️");
            upsertTowerType(towerRepository, "radar", "雷達塔", "敵方下一次不能飛躍", "📡");

            // 清掉不屬於這三個穩定代號的舊資料（例如專案早期版本塞過 type="攻擊"/"減速"/"範圍"
            // 這種分類標籤而不是代號的舊列）。這一步很重要：GET /api/towers 現在是前端建塔選單
            // 真正的資料來源，如果留著這些舊列，畫面上就會多出玩家點了也沒有任何效果的假塔選項
            // ——這正是「資料庫資料跟遊戲實際邏輯兜不起來」的情況，跟這個功能本身要解決的問題一樣。
            List<Tower> obsolete = towerRepository.findAll().stream()
                    .filter(t -> !validTypes.contains(t.getType()))
                    .toList();
            if (!obsolete.isEmpty()) {
                towerRepository.deleteAll(obsolete);
            }
        };
    }

    private void upsertTowerType(TowerRepository towerRepository, String type, String name, String effectDesc, String icon) {
        Tower tower = towerRepository.findByType(type).orElseGet(Tower::new);
        tower.setType(type);
        tower.setName(name);
        tower.setEffectDesc(effectDesc);
        tower.setIconUrl(icon);
        towerRepository.save(tower);
    }
}
