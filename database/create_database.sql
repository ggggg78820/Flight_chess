-- ============================================================
-- 建立這個專案要用的 MySQL 資料庫。
--
-- 只需要跑這一份 SQL，不需要手動建立 users / games / towers / game_towers
-- 這幾張表——應用程式啟動時，Hibernate 會依照 @Entity 類別自動建立/更新表格結構
-- （spring.jpa.hibernate.ddl-auto=update，見 src/main/resources/application.properties），
-- 這裡只需要先把「資料庫本身」建出來就好。
--
-- 使用方式（用你自己的 MySQL root 帳號登入後執行，例如 `mysql -u root -p < database/create_database.sql`）。
-- ============================================================

CREATE DATABASE IF NOT EXISTS flight_chess
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
