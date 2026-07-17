package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 最基本的「應用程式上下文（context）能不能正常啟動」測試。
 *
 * @SpringBootTest 會在跑測試時，把整個 Spring 應用程式（含資料庫連線、所有 Bean）真的啟動起來一次，
 * 如果任何 Bean 因為設定錯誤、資料庫連不上等原因初始化失敗，這個測試就會失敗。
 *
 * 注意：因為專案用的是 MySQL（見 application.properties），
 * 跑這個測試前必須先確保本機的 MySQL 服務有啟動，且 flight_chess 資料庫可以連線，
 * 否則測試會因為連不上資料庫而失敗，這跟程式碼邏輯本身無關。
 */
@SpringBootTest
class DemoApplicationTests {

	/**
	 * contextLoads()：方法內容是空的，這是刻意的設計。
	 * 這個測試的重點不在方法「內容」，而是 @SpringBootTest 這個註解本身——
	 * 只要 Spring 容器能成功啟動、跑到這個空方法而沒有拋出例外，測試就算通過。
	 * 這是 Spring Boot 專案裡最常見、最基本的「煙霧測試（smoke test）」。
	 */
	@Test
	void contextLoads() {
	}

}
