package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 負責回應「網頁本身」的 Controller（跟前面幾個回傳 JSON 的 @RestController 不一樣）。
 *
 * 這裡用的是 @Controller 而不是 @RestController，代表方法回傳的字串
 * 不會被直接當成回應內容輸出，而是會被 Spring MVC 拿去當作「視圖名稱（view name）」，
 * 交給 Thymeleaf 樣板引擎去找對應的 HTML 檔案並渲染成完整網頁回傳給瀏覽器。
 */
@Controller
public class HomeController {

    /**
     * 使用者在瀏覽器打開網站首頁（例如 http://localhost:8080/）時會呼叫到這裡。
     * 回傳字串 "index"，依照 application.properties 設定的
     * spring.mvc.view.prefix=/templates/ 、 spring.mvc.view.suffix=.html，
     * 最後會去找 src/main/resources/templates/index.html 這個檔案來渲染，
     * 也就是整個飛行棋塔防遊戲的畫面。
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
