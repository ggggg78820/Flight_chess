/*
  戰績頁專用的邏輯：等 window.Auth（見 auth.js）確認登入狀態後，
  已登入才呼叫 GET /api/games/history 取回最近 5 局紀錄並畫成表格；
  未登入則顯示提示文字，不呼叫後端（跟舊版 app.js 的 loadHistory() 邏輯相同）。
*/
(() => {
  const historyBody = document.getElementById('historyBody');

  async function loadHistory() {
    try {
      const res = await fetch('/api/games/history');
      const games = await res.json();
      renderHistory(games);
    } catch (e) {
      historyBody.innerHTML = '<p class="history-empty">無法載入歷史紀錄（連不上後端）。</p>';
    }
  }

  function renderHistory(games) {
    if (!Array.isArray(games) || games.length === 0) {
      historyBody.innerHTML = '<p class="history-empty">目前沒有歷史對局紀錄，玩完一局之後這裡就會出現。</p>';
      return;
    }
    const rows = games.map(g => {
      const win = g.result === 'WIN';
      const resultHtml = win ? '<span class="result-win">🎉 勝利</span>' : '<span class="result-lose">😔 落敗</span>';
      const time = g.createdAt ? new Date(g.createdAt).toLocaleString('zh-TW', { hour12: false }) : '—';
      return `<tr>
        <td>${resultHtml}</td>
        <td>${time}</td>
        <td>${g.turnCount ?? '—'}</td>
        <td>${g.playerMoves ?? '—'} / ${g.aiMoves ?? '—'}</td>
        <td>${g.usedTowerCount ?? '—'}</td>
      </tr>`;
    }).join('');
    historyBody.innerHTML = `
      <table class="history-table">
        <thead>
          <tr><th>結果</th><th>對局時間</th><th>回合數</th><th>步數（玩家/AI）</th><th>建塔數</th></tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;
  }

  window.Auth.onChange(user => {
    if (user) {
      loadHistory();
    } else {
      historyBody.innerHTML = '<p class="history-empty">尚未登入，登入後才能查看戰績。</p>';
    }
  });
})();
