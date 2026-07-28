package com.example.demo.service;

import com.example.demo.dto.GameEndRequest;
import com.example.demo.dto.GameEndResponse;
import com.example.demo.dto.GameSummaryResponse;
import com.example.demo.dto.TowerBuildDto;
import com.example.demo.entity.Game;
import com.example.demo.entity.GameTower;
import com.example.demo.entity.Tower;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.GameTowerRepository;
import com.example.demo.repository.TowerRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 遊戲局相關的商業邏輯層。負責「結束一局並結算戰績」「查詢某個使用者最近的對局紀錄」這兩件事，
 * 對應 GameController 底下的 API。詳見 UserService 開頭關於分層架構的說明。
 */
@Service
public class GameService {

    private static final Set<String> VALID_RESULTS = Set.of("WIN", "LOSE");
    private static final Set<String> VALID_TOWER_TYPES = Set.of("cannon", "freeze", "radar");
    private static final Set<String> VALID_OWNERS = Set.of("player", "ai");
    private static final Set<Integer> VALID_TOWER_POSITIONS = Set.of(3, 8, 15, 21, 25);
    // 遠大於任何一場正常對局可能出現的數字（正常對局頂多幾十~幾百回合），只用來擋明顯異常/
    // 惡意的請求（例如手動偽造一個 turnCount 或 towers 陣列長度大到誇張的請求）。
    // usedTowerCount 一定要跟 towers.size() 相等（見下方 TOWER_COUNT_MISMATCH 檢查），
    // 所以幫 usedTowerCount 設這個上限，towers 陣列長度也一併被限制住了，不用另外多寫一個檢查。
    private static final int MAX_REASONABLE_COUNT = 10_000;

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final TowerRepository towerRepository;
    private final GameTowerRepository gameTowerRepository;

    public GameService(GameRepository gameRepository, UserRepository userRepository,
                        TowerRepository towerRepository, GameTowerRepository gameTowerRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.towerRepository = towerRepository;
        this.gameTowerRepository = gameTowerRepository;
    }

    // 查詢某個使用者最近 5 局的遊戲紀錄，轉換成對外的 DTO 陣列，最新的排最前面
    public List<GameSummaryResponse> getRecentHistory(Long userId) {
        return gameRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toSummary).toList();
    }

    /**
     * 結束一局遊戲，這是真正會寫入資料庫的地方，做三件事：
     *   1. 把這局遊戲存成一筆新的 Game 紀錄
     *   2. 把這局裡雙方建造過的每一座塔，逐一存成一筆 GameTower 紀錄，關聯到剛存好的這局 Game
     *   3. 找到對應的使用者，把累計勝場或敗場 +1，並把最新的累計數字放進回應
     * 所有請求內容會先驗證完畢、解析出實際塔種後才開始寫入。整個方法位於同一交易中，
     * 任一步驟失敗都會完整回滾，不會留下部分遊戲或塔紀錄。
     *
     * userId 由 GameController 從登入 session 讀出來後傳進來，不是從 request 本身取得——
     * 這局是誰玩的，只能由後端已經驗證過的登入狀態決定，見 GameEndRequest 的說明。
     */
    @Transactional
    public GameEndResponse endGame(Long userId, GameEndRequest request) {
        ValidatedGameEnd validated = validateGameEnd(userId, request);

        Game game = new Game(validated.user().getId(), validated.result(), request.turnCount(),
                request.usedTowerCount(), request.playerMoves(), request.aiMoves());
        Game savedGame = gameRepository.save(game);

        int towersRecorded = recordTowers(savedGame, validated.towers());

        User user = validated.user();
        if ("WIN".equals(validated.result())) {
            user.setWinCount(safeCount(user.getWinCount()) + 1);
        } else {
            user.setLoseCount(safeCount(user.getLoseCount()) + 1);
        }
        userRepository.save(user);

        return new GameEndResponse(true, "遊戲結果已儲存", savedGame.getId(),
                user.getWinCount(), user.getLoseCount(), towersRecorded);
    }

    /**
     * 把這一局建造過的每一座塔存成 GameTower 紀錄，關聯到剛存好的 game。
     * 對每一筆塔建造資料，依 type（塔種代號）查出對應的 Tower；查不到就略過那一筆
     * （理論上前端只會送出 towerTypes 裡定義的 cannon/freeze/radar 三種之一，這裡的略過
     * 是針對「請求資料格式不如預期」這種系統邊界情況做的保護，不是預期會發生的正常流程）。
     * 回傳實際成功寫入的筆數。
     */
    private int recordTowers(Game game, List<ValidatedTower> towers) {
        for (ValidatedTower validated : towers) {
            TowerBuildDto build = validated.build();
            GameTower gameTower = new GameTower(game, validated.tower(), build.pos(), build.owner(), build.used());
            gameTowerRepository.save(gameTower);
        }
        return towers.size();
    }

    private ValidatedGameEnd validateGameEnd(Long userId, GameEndRequest request) {
        if (request == null) {
            throw badRequest("INVALID_GAME_REQUEST", "缺少遊戲結算資料");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAYER_NOT_FOUND", "找不到指定玩家"));

        String result = request.result() == null ? "" : request.result().trim().toUpperCase(Locale.ROOT);
        if (!VALID_RESULTS.contains(result)) {
            throw badRequest("INVALID_GAME_RESULT", "result 只能是 WIN 或 LOSE");
        }
        requirePositive(request.turnCount(), "turnCount", "INVALID_TURN_COUNT");
        requireNonNegative(request.usedTowerCount(), "usedTowerCount", "INVALID_TOWER_COUNT");
        requireNonNegative(request.playerMoves(), "playerMoves", "INVALID_PLAYER_MOVES");
        requireNonNegative(request.aiMoves(), "aiMoves", "INVALID_AI_MOVES");

        // 一局不限制最多能建幾座塔：因為每座塔的技能只能觸發一次，觸發後就失效，
        // 同一個塔位在遊戲過程中可以重複蓋新塔，所以 towers 陣列裡本來就可能出現同一個 pos
        // 對應多筆紀錄（有的 used=true 已失效、有的 used=false 還在生效中），不再視為異常。
        List<TowerBuildDto> builds = request.towers() == null ? List.of() : request.towers();
        if (request.usedTowerCount() != builds.size()) {
            throw badRequest("TOWER_COUNT_MISMATCH", "usedTowerCount 必須等於 towers 的實際數量");
        }

        List<ValidatedTower> validatedTowers = new ArrayList<>();
        for (TowerBuildDto build : builds) {
            if (build == null) {
                throw badRequest("INVALID_TOWER", "塔資料不可為空");
            }
            if (!VALID_TOWER_TYPES.contains(build.type())) {
                throw badRequest("INVALID_TOWER_TYPE", "塔型只能是 cannon、freeze 或 radar");
            }
            if (!VALID_OWNERS.contains(build.owner())) {
                throw badRequest("INVALID_TOWER_OWNER", "塔的 owner 只能是 player 或 ai");
            }
            if (!VALID_TOWER_POSITIONS.contains(build.pos())) {
                throw badRequest("INVALID_TOWER_POSITION", "塔的位置只能是 3、8、15、21 或 25");
            }
            Tower tower = towerRepository.findByType(build.type())
                    .orElseThrow(() -> badRequest("TOWER_TYPE_NOT_FOUND", "資料庫中找不到塔型：" + build.type()));
            validatedTowers.add(new ValidatedTower(build, tower));
        }
        return new ValidatedGameEnd(user, result, List.copyOf(validatedTowers));
    }

    private void requirePositive(Integer value, String field, String code) {
        if (value == null || value <= 0 || value > MAX_REASONABLE_COUNT) {
            throw badRequest(code, field + " 必須介於 1～" + MAX_REASONABLE_COUNT + " 之間");
        }
    }

    private void requireNonNegative(Integer value, String field, String code) {
        if (value == null || value < 0 || value > MAX_REASONABLE_COUNT) {
            throw badRequest(code, field + " 必須介於 0～" + MAX_REASONABLE_COUNT + " 之間");
        }
    }

    private ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private record ValidatedGameEnd(User user, String result, List<ValidatedTower> towers) {}

    private record ValidatedTower(TowerBuildDto build, Tower tower) {}

    // 把 Game Entity 轉換成對外的 GameSummaryResponse DTO
    private GameSummaryResponse toSummary(Game game) {
        return new GameSummaryResponse(
                game.getId(),
                game.getUserId(),
                game.getResult(),
                game.getTurnCount(),
                game.getUsedTowerCount(),
                game.getPlayerMoves(),
                game.getAiMoves(),
                game.getCreatedAt()
        );
    }
}
