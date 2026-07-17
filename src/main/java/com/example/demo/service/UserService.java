package com.example.demo.service;

import com.example.demo.dto.IdentifyRequest;
import com.example.demo.dto.IdentifyResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.RegisterResponse;
import com.example.demo.dto.UserProfileResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 使用者相關的商業邏輯層（Service）。
 *
 * 在「Controller -> Service -> Repository」的分層架構裡，Service 扮演的角色是：
 *   - Controller 只負責「處理 HTTP 細節」：解析請求、回傳適當的狀態碼，不應該知道商業規則怎麼運作
 *   - Service 負責「真正的商業邏輯」：帳密怎麼比對、註冊要檢查什麼、資料要怎麼組裝
 *   - Repository 只負責「跟資料庫溝通」，不應該知道任何業務規則
 * 這樣分層的好處是：同一段商業邏輯如果未來要被兩個不同的 Controller 共用（例如網頁版 API 跟手機 App API），
 * 或是要寫單元測試（測試商業邏輯不需要真的啟動一個 HTTP 伺服器），都會容易很多。
 *
 * @Service 是 @Component 的語意化版本，效果一樣（會被 Spring 掃描並註冊成 Bean），
 * 差別只在於用 @Service 標記可以讓其他開發者一眼看出「這是商業邏輯層的類別」。
 */
@Service
public class UserService {

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
     * 註冊新帳號。
     * 商業規則：帳號名稱不能重複；成功的話要記錄建立時間。
     */
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return new RegisterResponse(false, null, "使用者名稱已存在");
        }
        User user = new User(request.username(), request.password());
        user.setCreatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        return new RegisterResponse(true, saved.getId(), "註冊成功");
    }

    /**
     * 登入。
     * 商業規則：帳號要存在、密碼要相符，成功的話更新最後登入時間，並把目前累計勝敗場次一併回傳。
     *
     * 注意：這裡密碼比對方式沿用專案原本的明文比對（user.getPassword().equals(...)），
     * 只適合 demo 展示用途，正式產品應該改成儲存雜湊值（例如 BCrypt）並用雜湊比對。
     */
    public LoginResponse login(LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .filter(user -> user.getPassword().equals(request.password()))
                .map(user -> {
                    user.setLastLogin(LocalDateTime.now());
                    userRepository.save(user);
                    return new LoginResponse(true, user.getId(), user.getUsername(), user.getWinCount(), user.getLoseCount(), "登入成功");
                })
                // 帳號不存在或密碼錯誤，統一回覆同一種訊息，避免洩漏「帳號是否存在」這種資訊
                .orElseGet(() -> new LoginResponse(false, null, null, null, null, "帳號或密碼錯誤"));
    }

    /**
     * 「表明身分」：只憑名字讓後端知道這次是誰在玩，不檢查密碼。
     * 商業規則：這個名字如果已經存在，就直接沿用那個帳號（並更新最後上線時間）；
     * 如果是第一次出現的名字，就自動幫他建立一個新帳號（勝敗場次從 0 開始）。
     *
     * 這支方法刻意跟 login() 分開，而不是把 login() 改成「密碼可有可無」：
     * login() 保留給未來如果要做「真正需要密碼保護的帳號」使用，identify() 則是給
     * 「只是想知道這次是誰在玩、用來記錄戰績」這種輕量情境用，兩者商業意圖不同。
     */
    public IdentifyResponse identify(IdentifyRequest request) {
        return userRepository.findByUsername(request.username())
                .map(user -> {
                    user.setLastLogin(LocalDateTime.now());
                    userRepository.save(user);
                    return new IdentifyResponse(user.getId(), user.getUsername(), user.getWinCount(), user.getLoseCount(), false);
                })
                .orElseGet(() -> {
                    // password 欄位在資料庫是 NOT NULL，但這個流程完全不使用密碼，
                    // 所以用空字串佔位即可——這個帳號永遠不會、也不應該被拿去走 login() 的密碼比對流程。
                    User user = new User(request.username(), "");
                    user.setCreatedAt(LocalDateTime.now());
                    user.setLastLogin(LocalDateTime.now());
                    User saved = userRepository.save(user);
                    return new IdentifyResponse(saved.getId(), saved.getUsername(), saved.getWinCount(), saved.getLoseCount(), true);
                });
    }

    // 把 Entity 轉換成對外的 DTO：只挑選要公開的欄位，password 完全不會出現在這個轉換結果裡
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
