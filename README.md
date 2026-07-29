# 飛行棋塔防 AI 對戰（Flight Chess Demo）

單機版飛行棋，玩家對戰 AI，途中可以在固定塔位建造路障塔干擾對手。前端純 HTML/CSS/JavaScript，後端 Java Spring Boot 提供帳號登入與戰績儲存，資料存在 MySQL。

## 功能清單

- 帳號註冊 / 登入 / 登出（密碼以 BCrypt 雜湊儲存，登入狀態用 HTTP Session 管理）
- 玩家 vs AI 的完整飛行棋對局：擲骰、移動、飛躍格、折返規則
- 3 種路障塔（砲塔／冰霧塔／雷達塔），塔種資料由資料庫提供，前端動態產生建塔選單
- 塔的技能只能觸發一次，觸發後失效，同一塔位可以再蓋新塔，一局不限制總共能蓋幾座
- 遊戲結束後把結果、雙方步數、建塔紀錄寫進資料庫，並更新玩家累計勝敗場次
- 「最近戰績」列表（最近 5 局）
- 未登入也能離線試玩（畫面正常玩，但結束後不會寫入資料庫）

## 頁面結構

前端沒有樣板引擎、沒有前端框架，是 5 個各自獨立的靜態 HTML 頁面（各頁 `<nav>` 導覽列是複製貼上，不是共用元件）：

| 頁面 | 說明 | 需要登入 |
|---|---|---|
| `index.html` | 首頁：專案介紹、登入 / 註冊 | 否 |
| `game.html` | 開始遊戲：棋盤、教學提示、建塔選單、事件紀錄 | 否（未登入可離線試玩，只是不會存戰績） |
| `rules.html` | 規則說明：飛行棋規則與塔種效果（純靜態文字） | 否 |
| `history.html` | 戰績：最近 5 局紀錄 | 是（未登入會顯示提示，不呼叫後端） |
| `about.html` | 關於資料庫：四張資料表用途與關聯說明（純靜態文字） | 否 |

登入狀態由 `auth.js`（`window.Auth`）統一管理：呼叫 `GET /api/auth/me` 還原登入狀態、`POST /api/auth/logout` 登出，並負責更新每個頁面導覽列右側的登入狀態文字與登出按鈕。因為登入是 `HttpSession`（cookie）機制，登入一次後在 5 個頁面之間切換都會維持登入狀態，不需要每頁重新登入。

## 使用技術

| 項目 | 版本/說明 |
|---|---|
| Java | 21（本機以 25 編譯測試通過，`pom.xml` 設定編譯目標為 21） |
| Spring Boot | 4.0.7（Web MVC、Data JPA、Actuator） |
| 資料庫 | MySQL 8（正式執行）／H2 記憶體資料庫（僅測試時使用，見下方測試說明） |
| 密碼雜湊 | `spring-security-crypto`（僅密碼雜湊工具，未使用完整 Spring Security 驗證框架） |
| 前端 | 純 HTML + CSS + Vanilla JavaScript，無框架、無建置流程 |
| 建置工具 | Maven（附 `mvnw` / `mvnw.cmd`，不需另外安裝 Maven） |

## 系統架構

後端採 Controller → Service → Repository 分層：

```
Controller   只處理 HTTP 細節（解析請求、決定狀態碼）
   ↓
Service      商業邏輯（帳密驗證、遊戲規則驗證、資料組裝）
   ↓
Repository   Spring Data JPA，跟資料庫溝通
```

登入狀態用 Servlet 內建的 `HttpSession` 手動管理（`SessionUtil`），沒有引入完整的 Spring Security 驗證框架。前端遊戲規則（移動、塔的效果、勝負判定）完全寫在瀏覽器端的 `app.js`，只有「一局結束」時才會把最終結果送給後端寫入資料庫。

## 資料表

| 資料表 | 用途 |
|---|---|
| `users` | 帳號（使用者名稱、BCrypt 密碼雜湊）、累計勝敗場次 |
| `towers` | 塔種設定（名稱、代號、效果說明、圖示），前端建塔選單的資料來源 |
| `games` | 每一局的結算紀錄（結果、回合數、雙方步數、建塔數、所屬玩家） |
| `game_towers` | 某一局裡，在哪個塔位建了哪種塔、哪一方建的、技能有沒有觸發過（`used`），關聯 `games` 與 `towers` |

資料表關聯（文字版 ER 說明）：

```
users (1) ──< games (1) ──< game_towers >── (1) towers
```

- 一個 `users` 可以對應多筆 `games`（一個玩家打很多局）。
- 一筆 `games` 可以對應多筆 `game_towers`（一局裡建了好幾座塔）。
- 一種 `towers` 也可以出現在很多筆 `game_towers` 裡（同一種塔在很多局裡都被用過）。

## API 清單

| 方法 | 路徑 | 需要登入 | 說明 |
|---|---|---|---|
| POST | `/api/auth/register` | 否 | 註冊新帳號，成功後自動建立登入 session |
| POST | `/api/auth/login` | 否 | 登入，成功後建立登入 session |
| POST | `/api/auth/logout` | 否 | 登出，清除 session |
| GET | `/api/auth/me` | 是 | 查詢目前登入者的個人資料 |
| GET | `/api/towers` | 否 | 取得塔種清單（名稱/說明/圖示） |
| POST | `/api/games/end` | 是 | 結算一局遊戲，寫入 `games`／`game_towers`，更新累計勝敗 |
| GET | `/api/games/history` | 是 | 查詢目前登入者最近 5 局的紀錄 |

「需要登入」的 API 都是從登入 session 讀出玩家身分，不接受前端在請求內容裡宣稱的任何使用者 id；未登入呼叫會收到 `401`。

## 環境需求

- Java 21 以上（JDK）
- MySQL 8 以上（正式執行遊戲、儲存戰績用；只跑測試不需要，見下方）
- 不需要另外安裝 Maven，`mvnw` / `mvnw.cmd` 會自動下載對應版本

## 建立資料庫

用你自己的 MySQL 帳號執行專案內附的 SQL：

```bash
mysql -u root -p < database/create_database.sql
```

這份 SQL 只負責建立空的 `flight_chess` 資料庫本身；`users`／`games`／`towers`／`game_towers` 這幾張表，會在應用程式第一次啟動時由 Hibernate 依照 `@Entity` 類別自動建立，不需要手動建表。

## 設定 DB_PASSWORD

啟動應用程式前，必須先設定 `DB_PASSWORD` 環境變數（你的 MySQL 密碼），`src/main/resources/application.properties` 會讀取這個變數，密碼本身不會寫進任何檔案或版本控制。

**只跑測試（`mvn test`）完全不需要這個步驟**——測試一律使用 H2 記憶體資料庫，跟正式的 MySQL 無關，見下方「執行測試」。

PowerShell（Windows）：

```powershell
$env:DB_PASSWORD = "你的MySQL密碼"
```

Bash / Git Bash：

```bash
export DB_PASSWORD="你的MySQL密碼"
```

這個設定只在目前這個終端機視窗有效，關閉視窗後就會消失，需要重新設定。

## 啟動應用程式

```bash
./mvnw spring-boot:run
```

啟動後開啟 <http://localhost:8080/>。應用程式第一次啟動時，`DataInitializer` 會自動：

- 建立 3 種塔的種子資料（砲塔／冰霧塔／雷達塔）
- 如果 `users` 資料表是空的，建立一組預設 Demo 帳號（見下方「預設 Demo 帳號」）

## 執行測試

```bash
./mvnw test
```

測試不需要設定 `DB_PASSWORD`、不需要安裝或啟動 MySQL——`src/test/resources/application.properties` 會蓋過主設定檔，改連一個測試一啟動就建立、測試結束就消失的 H2 記憶體資料庫（`com.h2database:h2`，`scope=test`）。目前共 24 個測試，涵蓋帳號註冊/登入、遊戲結算驗證規則（含異常大數值的防護）、未登入呼叫戰績 API 會被拒絕、API 錯誤格式，以及應用程式能否正常啟動（`DemoApplicationTests`）。

## 預設 Demo 帳號

| 帳號 | 密碼 |
|---|---|
| `player1` | `player123` |

這組帳號由 `DataInitializer` 在 `users` 資料表是空的時候自動建立，方便展示時直接登入使用，不需要臨場註冊。

## 3~5 分鐘 Demo 流程

1. 開啟首頁（`index.html`），用預設帳號 `player1` / `player123` 登入（或現場註冊一個新帳號，展示註冊流程）；登入成功會自動跳轉到「開始遊戲」頁。
2. 按「▶ 開始 / 重新開始」開局。
3. 依序按「🎲 擲骰子」→「✈ 起飛 / 移動」，讓飛機起飛並前進；不確定規則可以先點導覽列「規則說明」頁看塔種效果。
4. 移動到棋盤上標示 `towerSpot` 的塔位格時，畫面會跳出建塔選單，選一種塔建造，展示「塔只能觸發一次、觸發後失效、同一塔位可以重建」的規則。
5. 按「⏎ 結束回合」，觀察 AI 自動擲骰、移動、建塔。
6. 重複幾輪，讓某一方任一架飛機經過對方的塔，展示塔的效果（送回機坪／骰子 -1／取消飛躍）跟「已觸發塔種」的統計。
7. 遊戲結束（任一方 4 架飛機都抵達終點）時，結算彈窗會顯示本局戰報，同時自動寫入資料庫、更新累計勝敗；點彈窗裡的「查看戰績」連結會直接跳到「戰績」頁。
8. 重新整理頁面（或切換到導覽列的「戰績」頁），展示登入狀態沒有消失（session 還在），並在「最近戰績」看到剛剛那一局。
9. 點導覽列右側的「登出」，展示登出後「戰績」頁不再顯示歷史紀錄、且無法再送出正式戰績（此時仍可在「開始遊戲」頁離線試玩）。

正常對局要 4 架飛機都抵達終點，回合數可能較多、擲骰結果也不受控制。如果現場時間有限，可以在步驟 3~6 之間視情況提早結束展示，改用「重新整理頁面看到歷史紀錄還在」「登出/登入」這些已經能確定會發生的行為當作展示重點，不用硬等一局真正打完。

## 現場驗證資料庫

畫面上看得到「戰績已儲存」不代表老師會相信資料真的進了 MySQL，建議展示時額外開一個 MySQL 用戶端（例如 `mysql -u root -p flight_chess`、MySQL Workbench 或 DBeaver 都可以），在遊戲進行的前後各查一次，直接讓老師看到筆數變化：

```sql
-- 展示剛剛註冊/登入的帳號在 users 裡（累計勝敗場次也在這張表）
SELECT id, username, win_count, lose_count, created_at, last_login
FROM users
ORDER BY id DESC
LIMIT 5;

-- 展示剛剛結束的那一局有寫進 games
SELECT id, user_id, result, turn_count, used_tower_count, player_moves, ai_moves, created_at
FROM games
ORDER BY id DESC
LIMIT 5;

-- 展示這一局實際建過的每一座塔，包含跟 towers 表的關聯、以及技能有沒有觸發過
SELECT gt.id, gt.game_id, t.name AS tower_name, gt.board_position, gt.owner, gt.used
FROM game_towers gt
JOIN towers t ON t.id = gt.tower_id
ORDER BY gt.id DESC
LIMIT 10;
```

建議流程：先查一次目前 `users`/`games` 的筆數 → 現場玩完一局 → 重新整理 MySQL 查詢結果，直接讓老師看到新增的那幾筆，比空口說「有存進資料庫」更有說服力。

## GitHub Pages 的限制

`.github/workflows/deploy-pages.yml` 只會把 `src/main/resources/static` 這個純前端資料夾部署到 GitHub Pages，**沒有後端、沒有資料庫**。透過 GitHub Pages 開啟的版本，遊戲畫面可以正常操作，但登入、註冊、戰績儲存都無法使用（背後呼叫的 `/api/...` 端點不存在），只能拿來看畫面/UI，不能拿來展示完整功能。要展示完整功能，需要照上面「啟動應用程式」的步驟在本機（或有 MySQL 的環境）跑起來。

## 已知限制

- 只有單機版（玩家 vs AI），沒有真正的多人連線對戰。
- 棋盤只有一張固定地圖，塔種固定 3 種、塔位固定 5 格（`3`／`8`／`15`／`21`／`25`）。
- 遊戲進行中的狀態只存在瀏覽器記憶體裡，只有「一局結束」時才會整批寫入資料庫；重新整理頁面會讓進行中的對局重置（不影響已經結束、已寫入資料庫的歷史紀錄）。
- 前端遊戲規則（移動、塔的效果）目前沒有自動化測試，全靠手動測試/展示驗證；後端 API 與資料驗證邏輯則有完整的自動化測試（`./mvnw test`）。
- 後端只保存「一局結束時的最終摘要結果」（勝負、回合數、雙方步數、建塔紀錄），不會逐步驗證整場對局的每一步是否都照著規則走；後端驗證的重點是「數值範圍合不合理」（例如結果只能是 WIN/LOSE、步數不可為負數或大到異常），這是單機課堂 Demo 的範圍，不具備正式競技系統該有的伺服器權威判定或防作弊能力。
