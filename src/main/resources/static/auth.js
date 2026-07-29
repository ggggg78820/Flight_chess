/*
  這支檔案被 5 個頁面（index/game/rules/history/about）共同引用，只負責兩件事：
    1. 提供 window.Auth：跨頁面共用的「目前登入者」查詢/登出工具（用 GET /api/auth/me、
       POST /api/auth/logout，跟 app.js 原本 checkSession()/logoutUser() 的邏輯相同，
       只是抽出來讓每個頁面都能用，不用各自重寫一份）。
    2. 如果頁面裡有 #navStatus / #navLogoutBtn 這兩個共用導覽列元素，自動把登入狀態畫上去、
       綁好登出按鈕——這樣每個頁面的 <nav> 只要照抄同一段標記，就能自動顯示正確的登入狀態，
       不用每頁各自寫一次判斷邏輯。
  各頁面若需要「目前登入者」資料做其他事（例如 game.html 要知道要不要把戰績存進資料庫），
  可以呼叫 window.Auth.refresh() 拿到 Promise，或用 window.Auth.onChange(fn) 訂閱後續變化。
*/
window.Auth = (() => {
  let user = null;       // null 代表未登入；已登入時是 {id, username, winCount, loseCount}
  const listeners = [];  // onChange() 註冊的回呼，登入狀態一變就通知全部

  function notify() { listeners.forEach(fn => fn(user)); }

  async function refresh() {
    try {
      const res = await fetch('/api/auth/me');
      user = res.ok ? await res.json() : null;
    } catch (e) {
      user = null; // 連不上後端也當作未登入，讓頁面能繼續用離線模式運作
    }
    notify();
    return user;
  }

  async function logout() {
    try { await fetch('/api/auth/logout', { method: 'POST' }); } catch (e) { /* 前端狀態照樣清空 */ }
    user = null;
    notify();
  }

  function onChange(fn) {
    listeners.push(fn);
    fn(user); // 立刻用目前已知的狀態呼叫一次，訂閱者不用額外處理「還沒收到第一次通知」的情況
  }

  return { refresh, logout, onChange, get user() { return user; } };
})();

// 導覽列的登入狀態顯示 + 登出按鈕，只有頁面裡真的有 #navStatus 這個元素時才會動作
document.addEventListener('DOMContentLoaded', () => {
  const statusEl  = document.getElementById('navStatus');
  const logoutBtn = document.getElementById('navLogoutBtn');
  if (!statusEl) return;

  window.Auth.onChange(user => {
    statusEl.textContent = user ? `已登入：${user.username}` : '尚未登入';
    if (logoutBtn) logoutBtn.style.display = user ? '' : 'none';
  });
  window.Auth.refresh();

  if (logoutBtn) {
    logoutBtn.addEventListener('click', async () => {
      await window.Auth.logout();
      location.href = 'index.html';
    });
  }
});
