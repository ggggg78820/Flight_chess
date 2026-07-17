package com.example.demo.controller;

import com.example.demo.dto.TowerResponse;
import com.example.demo.service.TowerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 負責處理「塔種設定」相關的 HTTP API。
 *
 * 目前只有一支端點：GET /api/towers。前端 app.js 會在頁面載入時呼叫它，
 * 用回傳的資料動態產生建塔選單，讓「這個遊戲有哪些塔、名字/說明文字是什麼」
 * 這件事以資料庫為準，不是前後端各自維護一份、容易兜不起來的兩份設定。
 */
@RestController
@RequestMapping("/api/towers")
public class TowerController {

    private final TowerService towerService;

    public TowerController(TowerService towerService) {
        this.towerService = towerService;
    }

    @GetMapping
    public List<TowerResponse> getAllTowers() {
        return towerService.getAllTowers();
    }
}
