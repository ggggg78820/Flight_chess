package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserProfileResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 使用者相關的商業邏輯層（Service），負責帳號註冊、登入驗證、個人資料查詢。
 *
 * 在「Controller -> Service -> Repository」的分層架構裡，Service 扮演的角色是：
 *   - Controller 只負責「處理 HTTP 細節」：解析請求、回傳適當的狀態碼，不應該知道商業規則怎麼運作
 *   - Service 負責「真正的商業邏輯」：帳密怎麼驗證、資料要怎麼組裝
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
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MAX_PASSWORD_LENGTH = 72; // BCrypt 只會實際使用密碼的前 72 bytes，太長沒有意義

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 依 id 查詢使用者的公開個人資料（不含密碼）。
     * 回傳 Optional，讓 Controller 自行決定「查無此人」時要回傳什麼樣的 HTTP 狀態碼（這裡是 404）。
     */
    public Optional<UserProfileResponse> getProfile(Long userId) {
        return userRepository.findById(userId).map(this::toProfileResponse);
    }

    /**
     * 註冊一個新帳號：名稱格式驗證、名稱不可重複、密碼長度驗證，
     * 密碼一律用 BCrypt 雜湊過後才存進資料庫。
     * 為了讓玩家「註冊完就能直接玩」不用再多按一次登入，回傳的資料格式跟 login() 一致，
     * Controller 收到回應後就會順便建立好登入 session（見 AuthController.register()）。
     */
    public UserProfileResponse register(RegisterRequest request) {
        String username = normalizeUsername(request == null ? null : request.username());
        String rawPassword = request == null ? null : request.password();
        validatePassword(rawPassword);

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "這個名稱已經有人使用了");
        }

        User user = new User(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        User saved = userRepository.save(user);
        return toProfileResponse(saved);
    }

    /**
     * 登入驗證：找出帳號、比對密碼雜湊。
     * 不論是「帳號不存在」還是「密碼錯誤」，都回傳同一種錯誤代碼跟訊息（INVALID_CREDENTIALS），
     * 不特別區分兩者——如果分開回應（例如「找不到這個帳號」vs「密碼錯誤」），
     * 等於讓外部有心人可以用這支 API 一個一個試出「哪些帳號名稱存在」，這是常見的安全考量。
     */
    public UserProfileResponse login(LoginRequest request) {
        String username = normalizeUsernameForLogin(request == null ? null : request.username());
        String rawPassword = request == null ? null : request.password();

        User user = userRepository.findByUsername(username)
                .filter(u -> rawPassword != null && passwordEncoder.matches(rawPassword, u.getPassword()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "帳號或密碼錯誤"));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return toProfileResponse(user);
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_USERNAME",
                    "玩家名稱須為 2～20 字，且只能包含中英文字母、數字、底線或連字號");
        }
        return normalized;
    }

    // 登入時的名稱正規化：只去除前後空白，「格式不對」跟「帳號不存在」都應該回傳同一種
    // INVALID_CREDENTIALS 錯誤（理由同 login() 的說明），所以這裡不重新檢查格式規則，
    // 只是單純把字串整理成跟註冊時一致的樣子，讓 findByUsername() 找得到人。
    private String normalizeUsernameForLogin(String username) {
        return username == null ? "" : username.trim();
    }

    private void validatePassword(String password) {
        int length = password == null ? 0 : password.length();
        if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD",
                    "密碼長度須為 " + MIN_PASSWORD_LENGTH + "～" + MAX_PASSWORD_LENGTH + " 字");
        }
    }

    // 把 Entity 轉換成對外的 DTO，只挑選畫面需要的玩家資料（不含 password 欄位）。
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
