package com.example.demo.service;

import com.example.demo.dto.TowerResponse;
import com.example.demo.entity.Tower;
import com.example.demo.repository.TowerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 塔種相關的商業邏輯層。目前只有一件事：把資料庫裡的塔種設定，轉成前端可以直接拿來用的格式。
 */
@Service
public class TowerService {

    private final TowerRepository towerRepository;

    public TowerService(TowerRepository towerRepository) {
        this.towerRepository = towerRepository;
    }

    // 查詢所有塔種設定，轉換成對外的 DTO 陣列。前端會在頁面載入時呼叫這個，
    // 依照回傳的清單動態產生建塔選單的按鈕，取代原本寫死在 app.js 裡的塔種資料。
    public List<TowerResponse> getAllTowers() {
        return towerRepository.findAll().stream().map(this::toResponse).toList();
    }

    private TowerResponse toResponse(Tower tower) {
        return new TowerResponse(tower.getType(), tower.getName(), tower.getEffectDesc(), tower.getIconUrl());
    }
}
