package com.example.demo.service;

import com.example.demo.dto.IdentifyRequest;
import com.example.demo.dto.IdentifyResponse;
import com.example.demo.dto.UserProfileResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 使用者相關的商業邏輯層（Service）。
 *
 * 在「Controller -> Service -> Repository」的分層架構裡，Service 扮演的角色是：
 *   - Controller 只負責「處理 HTTP 細節」：解析請求、回傳適當的狀態碼，不應該知道商業規則怎麼運作
 *   - Service 負責「真正的商業邏輯」：玩家名稱怎麼驗證、資料要怎麼組裝
 *   - Repository 只負責「跟資料庫溝通」，不應該知道任何業務規則
 * 這樣分層的好處是：同一段商業邏輯如果未來要被兩個不同的 Controller 共用（例如網頁版 API 跟手機 App API），
 * 或是要寫單元測試（測試商業邏輯不需要真的啟動一個 HTTP 伺服器），都會容易很多。
 *
 * @Service 是 @Component 的語意化版本，效果一樣（會被 Spring 掃描並註冊成 Bean），
 * 差別只在於用 @Service 標記可以讓其他開發者一眼看出「這是商業邏輯層的類別」。
 */
@Service
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_-]{2,20}$");

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 依 id 查詢使用者的公開個人資料（不含密碼）。
     * 回傳 Optional，讓 Controller 自行決定「查無此人」時要回傳什麼樣的 HTTP 狀態碼（這裡是 404）。
     */
    public Optional<UserProfileResponse> getProfile(Long userId) {
        return userRepository.findById(userId).map(this::toProfileResponse);
    }

    /**
     * 「確認玩家」：只憑名字讓後端知道這次是誰在玩。
     * 商業規則：這個名字如果已經存在，就直接沿用那個帳號（並更新最後上線時間）；
     * 如果是第一次出現的名字，就自動幫他建立一個新帳號（勝敗場次從 0 開始）。
     *
     * 名稱會先去除前後空白，長度須為 2～20 字，並只允許中英文字母、數字、底線與連字號。
     */
    public IdentifyResponse identify(IdentifyRequest request) {
        String username = normalizeUsername(request == null ? null : request.username());
        return userRepository.findByUsername(username)
                .map(user -> {
                    user.setLastLogin(LocalDateTime.now());
                    userRepository.save(user);
                    return new IdentifyResponse(user.getId(), user.getUsername(), user.getWinCount(), user.getLoseCount(), false);
                })
                .orElseGet(() -> {
                    User user = new User(username);
                    user.setCreatedAt(LocalDateTime.now());
                    user.setLastLogin(LocalDateTime.now());
                    User saved = userRepository.save(user);
                    return new IdentifyResponse(saved.getId(), saved.getUsername(), saved.getWinCount(), saved.getLoseCount(), true);
                });
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_USERNAME",
                    "玩家名稱須為 2～20 字，且只能包含中英文字母、數字、底線或連字號");
        }
        return normalized;
    }

    // 把 Entity 轉換成對外的 DTO，只挑選畫面需要的玩家資料。
    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getWinCount(),
                user.getLoseCount(),
                user.getCreatedAt(),
                user.getLastLogin()
        );
    }
}
