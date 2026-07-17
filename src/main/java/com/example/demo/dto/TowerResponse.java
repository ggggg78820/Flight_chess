package com.example.demo.dto;

/**
 * 單一塔種設定的資料格式，對應 GET /api/towers 回傳陣列裡的每一個元素。
 *
 * type 是前端拿來當 key、比對遊戲邏輯用的穩定代號（cannon/freeze/radar）；
 * name/effectDesc/icon 則是純顯示用的文字/圖示，讓前端可以直接依照這份清單
 * 動態產生建塔選單的按鈕，不用再把「這個遊戲總共有哪幾種塔、名字叫什麼、圖示是什麼」
 * 寫死在 app.js 裡——這樣資料庫才是這些顯示內容真正的來源，不會有前端跟資料庫兜不起來的風險。
 */
public record TowerResponse(String type, String name, String effectDesc, String icon) {
}
