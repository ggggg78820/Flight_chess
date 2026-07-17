package com.example.demo.dto;

/**
 * 登入 API 的請求資料格式，對應 POST /api/users/login 的 request body。
 *
 * 這是一個 Java record：只要宣告欄位（這裡是 username、password），
 * 編譯器就會自動幫忙產生建構子、getter（叫做 username()、password() 而不是 getUsername()）、
 * equals()、hashCode()、toString()，很適合拿來當「純粹裝資料、沒有商業邏輯」的 DTO（Data Transfer Object）用。
 *
 * Spring 收到前端送來的 JSON（例如 {"username":"player1","password":"password"}）時，
 * 會自動用 Jackson 把它轉換成一個 LoginRequest 物件，Controller 方法只要用
 * @RequestBody LoginRequest request 就能直接拿到型別安全的資料，不用再像以前用 Map<String,String> 手動取值。
 */
public record LoginRequest(String username, String password) {
}
