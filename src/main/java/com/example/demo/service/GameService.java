package com.example.demo.service;

import com.example.demo.dto.GameEndRequest;
import com.example.demo.dto.GameEndResponse;
import com.example.demo.dto.GameSummaryResponse;
import com.example.demo.dto.TowerBuildDto;
import com.example.demo.entity.Game;
import com.example.demo.entity.GameTower;
import com.example.demo.entity.Tower;
import com.example.demo.entity.User;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.GameTowerRepository;
import com.example.demo.repository.TowerRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 遊戲局相關的商業邏輯層。負責「結束一局並結算戰績」「查詢某個使用者最近的對局紀錄」這兩件事，
 * 對應 GameController 底下的 API。詳見 UserService 開頭關於分層架構的說明。
 */
@Service
public class GameService {

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
     * 缺漏的欄位會用預設值代替，避免因為前端漏傳欄位而整支程式出錯。
     */
    public GameEndResponse endGame(GameEndRequest request) {
        String result = request.result() == null ? "LOSE" : request.result();
        Integer turnCount = request.turnCount() == null ? 0 : request.turnCount();
        Integer usedTowerCount = request.usedTowerCount() == null ? 0 : request.usedTowerCount();
        Integer playerMoves = request.playerMoves() == null ? 0 : request.playerMoves();
        Integer aiMoves = request.aiMoves() == null ? 0 : request.aiMoves();

        Game game = new Game(request.userId(), result, turnCount, usedTowerCount, playerMoves, aiMoves);
        Game savedGame = gameRepository.save(game);

        int towersRecorded = recordTowers(savedGame, request.towers());

        Integer winCount = null;
        Integer loseCount = null;

        if (request.userId() != null) {
            User user = userRepository.findById(request.userId()).orElse(null);
            if (user != null) {
                if ("WIN".equalsIgnoreCase(result)) {
                    user.setWinCount(user.getWinCount() + 1);
                } else {
                    user.setLoseCount(user.getLoseCount() + 1);
                }
                userRepository.save(user);
                winCount = user.getWinCount();
                loseCount = user.getLoseCount();
            }
        }

        return new GameEndResponse(true, "遊戲結果已儲存", savedGame.getId(), winCount, loseCount, towersRecorded);
    }

    /**
     * 把這一局建造過的每一座塔存成 GameTower 紀錄，關聯到剛存好的 game。
     * 對每一筆塔建造資料，依 type（塔種代號）查出對應的 Tower；查不到就略過那一筆
     * （理論上前端只會送出 towerTypes 裡定義的 cannon/freeze/radar 三種之一，這裡的略過
     * 是針對「請求資料格式不如預期」這種系統邊界情況做的保護，不是預期會發生的正常流程）。
     * 回傳實際成功寫入的筆數。
     */
    private int recordTowers(Game game, List<TowerBuildDto> towerBuilds) {
        if (towerBuilds == null || towerBuilds.isEmpty()) {
            return 0;
        }
        int recorded = 0;
        for (TowerBuildDto build : towerBuilds) {
            Tower tower = towerRepository.findByType(build.type()).orElse(null);
            if (tower == null) {
                continue;
            }
            GameTower gameTower = new GameTower(game, tower, build.pos(), build.owner());
            gameTowerRepository.save(gameTower);
            recorded++;
        }
        return recorded;
    }

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
