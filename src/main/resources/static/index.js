/*
  首頁專用的登入/註冊邏輯。跟舊版 app.js 裡 registerUser()/loginUser() 的邏輯相同
  （呼叫 POST /api/auth/register、/api/auth/login，成功後端會建立 HttpSession），
  差別是這裡登入成功後直接導向 game.html，不用在同一頁切換畫面狀態。
*/
(() => {
  const usernameInput = document.getElementById('usernameInput');
  const passwordInput = document.getElementById('passwordInput');
  const registerBtn    = document.getElementById('registerBtn');
  const loginBtn       = document.getElementById('loginBtn');
  const goGameBtn      = document.getElementById('goGameBtn');
  const authMessage    = document.getElementById('authMessage');

  async function apiPost(url, body) {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || '請求失敗');
    return data;
  }

  async function submit(url, successVerb) {
    const username = (usernameInput.value || '').trim();
    const password = passwordInput.value || '';
    if (!username || !password) { authMessage.textContent = '請輸入帳號與密碼。'; return; }
    try {
      await apiPost(url, { username, password });
      await window.Auth.refresh();
      authMessage.textContent = `${successVerb}成功，正在前往遊戲畫面...`;
      location.href = 'game.html';
    } catch (e) {
      authMessage.textContent = e.message || `${successVerb}失敗。`;
    }
  }

  registerBtn.addEventListener('click', () => submit('/api/auth/register', '註冊'));
  loginBtn.addEventListener('click', () => submit('/api/auth/login', '登入'));
  passwordInput.addEventListener('keydown', e => { if (e.key === 'Enter') submit('/api/auth/login', '登入'); });
  goGameBtn.addEventListener('click', () => { location.href = 'game.html'; });
})();
