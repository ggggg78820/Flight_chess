package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 整個 Spring Boot 應用程式的進入點（entry point）。
 *
 * @SpringBootApplication 是三個常用註解的組合：
 *   1. @Configuration       -> 這個類別本身也可以定義 Bean
 *   2. @EnableAutoConfiguration -> 讓 Spring Boot 依照 classpath 上的依賴，自動幫你組態好常見設定
 *                                  （例如偵測到 spring-boot-starter-webmvc 就自動啟動內嵌 Tomcat）
 *   3. @ComponentScan       -> 自動掃描同一個套件（com.example.demo）以及子套件底下所有的
 *                              @Component / @Controller / @Service / @Repository 等類別並註冊成 Bean
 */
@SpringBootApplication
public class DemoApplication {

	/**
	 * Java 程式的標準進入點。
	 * SpringApplication.run() 會啟動整個 Spring 容器：
	 *   - 建立內嵌的 Web 伺服器（Tomcat）
	 *   - 掃描並初始化所有 Bean（Controller、Repository、DataInitializer...）
	 *   - 連線資料庫、建立/更新資料表結構
	 * 執行完這行之後，應用程式就會持續運作，等待瀏覽器的 HTTP 請求進來。
	 */
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	/**
	 * 密碼雜湊工具，整個應用程式共用同一個 Bean（UserService 註冊/登入驗證密碼、
	 * DataInitializer 建立預設帳號時都會注入使用）。BCrypt 是業界標準的密碼雜湊演算法，
	 * 特性是「同一組密碼每次雜湊出來的字串都不一樣（內含隨機 salt），但 matches() 依然能正確驗證」，
	 * 資料庫裡永遠只存雜湊後的字串，不會存明碼密碼。
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
