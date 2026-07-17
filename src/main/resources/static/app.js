/*
  這支檔案是整個「飛行棋塔防 AI 對戰」遊戲的大腦，全部的遊戲規則、畫面更新、
  跟後端資料庫溝通的邏輯都寫在這裡。瀏覽器載入 index.html 時，最後一行
  <script src="/app.js"></script> 會執行這支程式。

  整體架構可以分成五個部分（由上到下）：
    1. 抓取 HTML 元素、定義棋盤座標與遊戲規則常數
    2. 跟後端 API 溝通的函式（表明身分、載入塔種設定、回報一局遊戲的最終結果）
    3. 畫面繪製相關函式（畫棋盤、更新文字、寫紀錄）
    4. 遊戲規則邏輯（移動、塔的效果、判斷輸贏、AI 行為）
    5. 按鈕事件綁定（把使用者的點擊跟上面的邏輯串起來）

  最外層用 (() => { ... })() 這種「立即執行函式（IIFE）」包起來，
  好處是裡面宣告的所有變數（board、state、currentUser...）都只存在於這個函式的
  局部作用域裡，不會汙染到全域的 window 物件，避免跟其他程式或瀏覽器套件互相衝突。
*/
(() => {
  // ------------------------------------------------------------------
  // 第 1 部分：一開始就把畫面上會用到的 DOM 元素都抓出來存成常數，
  // 之後整支程式都直接用這些變數操作畫面，不用每次都重新 getElementById。
  // 這些 id 全部對應 index.html 裡定義的元素。
  // ------------------------------------------------------------------
  const board            = document.getElementById('board');           // 棋盤容器
  const logEl            = document.getElementById('log');             // 事件紀錄面板
  const startBtn         = document.getElementById('startBtn');        // 開始/重新開始按鈕
  const rollBtn          = document.getElementById('rollBtn');         // 擲骰子按鈕
  const moveBtn          = document.getElementById('moveBtn');         // 起飛/移動按鈕
  const endBtn           = document.getElementById('endBtn');          // 結束回合按鈕
  const diceLabel        = document.getElementById('diceLabel');       // 骰子點數顯示
  const turnLabel        = document.getElementById('turnLabel');       // 目前輪到誰顯示
  const roundLabel       = document.getElementById('roundLabel');      // 回合數顯示
  const towerCountLabel  = document.getElementById('towerCountLabel'); // 已建塔數顯示
  const tipTitle         = document.getElementById('tipTitle');        // 教學提示標題
  const tipText          = document.getElementById('tipText');         // 教學提示內文
  const towerChoices     = document.getElementById('towerChoices');    // 建塔選單容器
  const resultModal      = document.getElementById('resultModal');     // 結算彈窗
  const resultText       = document.getElementById('resultText');      // 彈窗裡的「勝利/失敗」大字
  const resultDetail     = document.getElementById('resultDetail');    // 彈窗裡的詳細數據
  const closeModal       = document.getElementById('closeModal');      // 彈窗的關閉按鈕
  const usernameInput    = document.getElementById('usernameInput');   // 身分輸入框（只填名字，不用密碼）
  const loginBtn         = document.getElementById('loginBtn');        // 「登入」按鈕
  const loginStatus      = document.getElementById('loginStatus');     // 登入狀態小字提示
  const historyBody      = document.getElementById('historyBody');     // 最近戰績列表容器

  /*
    棋盤路徑座標表：一個長度 29 的陣列（index 0 ~ 28），每個元素是 [x, y]，
    代表該格子在棋盤上的「百分比座標」（0~100，對應 CSS 的 left/top 百分比）。
    整條路徑是沿著正方形棋盤的四個邊繞一圈：
      index 0~7   沿著下邊往右走
      index 8~14  沿著右邊往上走
      index 15~21 沿著上邊往左走
      index 22~28 沿著左邊往下走
    飛機的「位置」在程式裡都是用這個陣列的 index（也就是走了第幾格）來表示，
    -1 代表還停在機坪（尚未起飛），數字 >= finishIndex（29）代表已經抵達終點。
  */
  const path = [
    [18,92],[28,92],[38,92],[48,92],[58,92],[68,92],[78,92],[88,92],
    [92,82],[92,72],[92,62],[92,52],[92,42],[92,32],[92,18],
    [82,8],[72,8],[62,8],[52,8],[42,8],[32,8],[18,8],
    [8,18],[8,28],[8,38],[8,48],[8,58],[8,68],[8,78]
  ];

  const jumpCells  = new Set([5,12,19,26]);   // 飛躍格的 index：飛機停在這些格子上會額外前進 4 格
  const towerSpots = new Set([3,8,15,21,25]); // 可建塔格的 index：只有停在這些格子上才能建塔
  const finishIndex = path.length;            // 終點的「位置代號」＝路徑長度（29），比路徑最後一格 index 大 1

  // 塔種設定的離線備援資料：只有在 GET /api/towers 連不上時才會拿來用，
  // 確保就算後端掛了，遊戲畫面上還是有塔可以選、可以蓋，不會整個壞掉。
  // 正常情況下 towerTypes 的實際內容會在頁面載入時被 loadTowerTypes() 換成資料庫回傳的版本，
  // 讓「有哪幾種塔、名稱/圖示/說明文字是什麼」以資料庫為準，不是這裡寫死的內容說了算。
  // 注意：這裡的 key（cannon/freeze/radar）是塔的「效果邏輯」比對用的字串常數，這個不會、
  // 也不該被資料庫覆蓋——實際效果邏輯寫在 applyTowerEffects() / moveSide() 裡。
  const FALLBACK_TOWER_TYPES = {
    cannon:{ name:'砲塔',   icon:'🏰', desc:'敵方飛機經過，立即回機坪' },
    freeze:{ name:'冰霧塔', icon:'❄️', desc:'敵方下回合骰子 -1' },
    radar: { name:'雷達塔', icon:'📡', desc:'敵方下一次不能飛躍' }
  };
  let towerTypes = FALLBACK_TOWER_TYPES;

  // state：整個遊戲「目前的狀態」，所有動態資料都放在這一個物件裡（詳見 newState() 的說明）。
  // 一開始是 undefined，要等玩家按下「開始」按鈕、呼叫 newState() 之後才會有內容。
  let state;

  // currentUser：目前表明身分的使用者資訊（{id, username, winCount, loseCount}）。
  // 由 identifyUser() 在頁面載入時、或玩家按「登入」時向後端要來；如果後端連不上，會保持 null（離線模式）。
  let currentUser = null;

  // ------------------------------------------------------------------
  // 第 2 部分：跟後端 Spring Boot API 溝通的函式
  // ------------------------------------------------------------------

  /**
   * 通用的「用 JSON 格式發送 POST 請求」小工具函式。
   * fetch 是瀏覽器內建的 API，用來跟伺服器發送 HTTP 請求；
   * 這裡統一把 method、Content-Type、JSON.stringify(body) 包好，避免每個呼叫的地方都要重複寫一樣的樣板程式碼。
   * 回傳的是一個 Promise，呼叫端要用 await 或 .then() 取得後端回應的 JSON 內容。
   */
  async function apiPost(url, body) {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    return res.json();
  }

  /**
   * 表明身分：把 usernameInput 裡的名字送到後端 POST /api/users/identify。
   * 這不是真正的帳號密碼登入——後端收到名字後，如果這個名字已經存在就直接沿用，
   * 不存在就自動建立一個新帳號，目的單純是「讓後端知道這次是誰在玩」，
   * 好把之後的遊戲結果掛在正確的名字底下（見 GameService.endGame()）。
   *
   * 呼叫時機有兩個：
   *   1. 頁面一載入就用輸入框的預設值（player1）自動呼叫一次，維持「打開就能玩」的體驗
   *   2. 玩家自己在輸入框改名字、按下「登入」按鈕時
   *
   * 成功：把後端回傳的使用者 id、累計勝敗場次存進 currentUser，之後開始/結束遊戲時都會用到。
   * 失敗或連不上後端：不會讓整個遊戲壞掉，只是在紀錄區跟 loginStatus 提示「離線模式」，
   *   currentUser 維持 null，後續呼叫後端 API 的地方都會先檢查 currentUser 存不存在再決定要不要送出。
   */
  async function identifyUser(username) {
    username = (username || '').trim();
    if (!username) { loginStatus.textContent = '請輸入名字才能登入。'; return; }
    try {
      const data = await apiPost('/api/users/identify', { username });
      currentUser = { id: data.userId, username: data.username, winCount: data.winCount, loseCount: data.loseCount };
      loginStatus.textContent = `已登入為 ${currentUser.username}（累計 勝 ${currentUser.winCount} / 敗 ${currentUser.loseCount}）`;
      log(`已連線資料庫，${data.isNewUser ? '建立新帳號並' : ''}登入為 ${currentUser.username}（累計 勝 ${currentUser.winCount} / 敗 ${currentUser.loseCount}）。`, 's');
      loadHistory(); // 換了身分（或第一次載入頁面）之後，最近戰績列表也要跟著換成這個人的紀錄
    } catch (e) {
      // fetch 在網路完全不通、後端沒啟動時會直接 reject（拋出例外），用 try/catch 接住，避免整支程式中斷
      loginStatus.textContent = '無法連線後端，將以離線模式進行（戰績不會儲存）。';
      log('無法連線後端資料庫，將以離線模式進行（戰績不會儲存）。', 's');
    }
  }

  /**
   * 從後端 GET /api/towers 拿回目前資料庫裡設定的塔種（名稱、效果說明、圖示），
   * 取代掉 FALLBACK_TOWER_TYPES 這份離線備援資料，成為畫面上實際顯示的內容，
   * 並呼叫 renderTowerChoiceButtons() 依照最新資料重新產生建塔選單的按鈕。
   *
   * 連不上後端的話就維持使用 FALLBACK_TOWER_TYPES（頁面一開始就已經用它 render 過一次選單了），
   * 遊戲仍然可以正常玩，只是塔的名稱/說明文字不保證跟資料庫最新設定一致。
   */
  async function loadTowerTypes() {
    try {
      const res = await fetch('/api/towers');
      const list = await res.json();
      if (Array.isArray(list) && list.length > 0) {
        const next = {};
        list.forEach(t => { next[t.type] = { name: t.name, icon: t.icon, desc: t.effectDesc }; });
        towerTypes = next;
        renderTowerChoiceButtons();
        log('已從資料庫載入塔種設定。', 's');
      }
    } catch (e) {
      log('無法載入塔種設定，改用內建的預設塔種資料。', 's');
    }
  }

  // ------------------------------------------------------------------
  // 第 3 部分：畫面繪製相關函式
  // ------------------------------------------------------------------

  /**
   * 產生一架飛機圖示的 SVG 字串。
   * 用純 SVG 畫飛機（而不是用圖片檔），好處是可以直接用參數控制顏色（玩家綠、AI 紅）、
   * 旋轉角度（依照飛機在棋盤上的行進方向）、縮放比例（機坪裡的小圖示縮小一點），不用準備多張圖片。
   */
  function planeSVG(color, rotate=0, scale=1) {
    return `<svg viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg"
      style="transform:rotate(${rotate}deg);width:${28*scale}px;height:${28*scale}px">
      <ellipse cx="16" cy="16" rx="3.5" ry="12" fill="${color}" opacity="0.95"/>
      <ellipse cx="16" cy="15" rx="13"  ry="3.5" fill="${color}" opacity="0.85"/>
      <ellipse cx="16" cy="25" rx="6"   ry="2"   fill="${color}" opacity="0.8"/>
      <ellipse cx="16" cy="6"  rx="2"   ry="2.5" fill="rgba(255,255,255,0.5)"/>
    </svg>`;
  }

  /**
   * 依照飛機目前所在的棋盤位置（pos），決定飛機圖示要旋轉幾度，讓機頭朝向前進方向。
   * 對照 path 陣列的分段：0~7 沿下邊往右飛（機頭朝右，0 度）、8~14 沿右邊往上飛（機頭朝上，270 度）、
   * 15~21 沿上邊往左飛（機頭朝左，0 度但整體已經翻轉過，這裡沿用畫面觀察得出的角度）、22~28 沿左邊往下飛（90 度）。
   * pos < 0（還在機坪）則統一給 270 度。
   */
  function planeRotation(pos) {
    if (pos < 0)   return 270;
    if (pos <= 7)  return 0;
    if (pos <= 14) return 270;
    if (pos <= 21) return 0;
    return 90;
  }

  /**
   * 建立一份全新的遊戲狀態物件。每次按下「開始/重新開始」按鈕時都會呼叫這個函式，
   * 產生的物件會整個取代掉舊的 state，等於把遊戲重置成初始狀態。
   *
   * 回傳的物件欄位說明：
   *   active        - 遊戲是否正在進行中（結束後會設成 false，用來擋住按鈕繼續操作）
   *   turn          - 目前輪到誰：'player' 或 'ai'
   *   dice          - 這一回合擲出的骰子點數，尚未擲骰時是 null
   *   round         - 目前第幾回合（雙方各走一輪算一回合結束，見 nextTurn()）
   *   moved         - 這一回合玩家是否已經移動過（目前程式碼有設定但沒有實際拿來做判斷，保留給未來擴充用）
   *   towerCount    - 目前棋盤上總共蓋了幾座塔（雙方加總）
   *   user          - 顯示用的使用者資訊，初始的勝敗場次會從 currentUser（登入結果）帶入，
   *                   如果還沒表明身分成功就先用 0/0，等 identifyUser() 完成後下一次開新局才會是正確數字
   *   games         - 這個瀏覽器分頁這次執行期間，已經打完的對局摘要陣列（僅存在記憶體，重新整理頁面就會消失）
   *   player / ai   - 雙方陣營各自的狀態：
   *                     planes   - 長度 4 的陣列，代表 4 架飛機各自的位置；-1=在機坪，0~28=跑道上的格子，>=29=已抵達終點
   *                     freeze   - 是否處於「被冰霧塔影響，下一次骰子 -1」的狀態
   *                     jam      - 是否處於「被雷達塔影響，下一次不能飛躍」的狀態
   *                     finished - 已經抵達終點的飛機數量
   *   towers        - 棋盤上目前所有塔的陣列，每個元素是 {owner, type, pos}
   *   currentGame   - 這一局遊戲的統計資料（含 player_moves/ai_moves：雙方各自實際移動飛機的次數），
   *                   結束時會拿來組成結算畫面內容、也會送給後端 /api/games/end
   */
  function newState() {
    return {
      active:false, turn:'player', dice:null, round:1, moved:false, towerCount:0,
      user:{ username: currentUser?.username ?? 'player1', win_count: currentUser?.winCount ?? 0, lose_count: currentUser?.loseCount ?? 0 }, games:[],
      player:{ planes:[-1,-1,-1,-1], freeze:false, jam:false, finished:0 },
      ai:    { planes:[-1,-1,-1,-1], freeze:false, jam:false, finished:0 },
      towers:[],
      currentGame:{ id:Date.now(), result:null, turn_count:0, used_tower_count:0, player_moves:0, ai_moves:0 }
    };
  }

  /**
   * 在右側「事件紀錄」面板新增一則訊息。
   * type 決定訊息前面標籤顯示什麼字：'p'=玩家（綠色系）、'a'=AI（紅色系）、其他（例如預設值 's'）=系統訊息。
   * 用 prepend（插入到最前面）而不是 appendChild，讓最新的訊息永遠顯示在最上面，不用使用者往下捲。
   */
  function log(msg, type='s') {
    const p = document.createElement('p');
    p.innerHTML = `<span class="tag ${type}">${type==='p'?'玩家':type==='a'?'AI':'系統'}</span>${msg}`;
    logEl.prepend(p);
  }

  // 更新左側教學提示區塊的標題與內文，貫穿整個遊戲流程用來一步步告訴玩家「現在該做什麼」
  function setTip(t, x) { tipTitle.textContent=t; tipText.innerHTML=x; }

  /**
   * 把畫面上方四個統計數字（目前回合、骰子點數、回合數、已建塔數）同步成 state 裡的最新資料。
   * 用了 ?. （optional chaining）跟 ?? （nullish coalescing）：
   *   state?.active 表示如果 state 還是 undefined（遊戲還沒開始過），不會噴錯，直接得到 undefined
   *   ?? '—' / ?? 0 則是「如果左邊的值是 null 或 undefined，就用右邊的預設值代替」
   * 這樣即使在遊戲開始之前呼叫這個函式，畫面也只會顯示预設的「—」「0」，不會出現 JS 例外。
   */
  function updateLabels() {
    turnLabel.textContent       = state?.active ? (state.turn==='player'?'玩家':'AI') : '—';
    diceLabel.textContent       = state?.dice ?? '—';
    roundLabel.textContent      = state?.round ?? 0;
    towerCountLabel.textContent = state?.towerCount ?? 0;
  }

  /**
   * 重新繪製整個棋盤：這是這支程式裡最長的一個函式，但邏輯很直觀——
   * 每次呼叫都「先把棋盤內容整個清空（board.innerHTML = ''），再依照目前的 state 從頭畫一次」，
   * 而不是去追蹤「哪個東西改變了，只更新那個東西」。對這個規模的遊戲來說，整個重畫的效能完全足夠，
   * 程式碼也單純很多，不用處理「新舊畫面怎麼比對」的複雜邏輯。
   *
   * 繪製順序（後畫的會疊在先畫的上面，因為都是 position:absolute）：
   *   1. 29 個路徑格子 + 終點格
   *   2. 玩家 / AI 機坪（以及裡面尚未起飛的飛機小圖示）
   *   3. 目前棋盤上所有的塔
   *   4. 雙方所有「已經起飛」的飛機（含已抵達終點、正在跑道上兩種情況）
   */
  /**
   * 依照目前的 towerTypes（可能是離線備援資料，也可能是從資料庫載回來的最新資料）
   * 重新產生建塔選單（#towerChoices）裡的按鈕，取代原本寫死在 index.html 裡的三顆按鈕。
   * 每顆按鈕的 data-tower 屬性存塔種代號，towerChoices 的點擊事件代理（見第 5 部分）就是靠
   * 這個屬性判斷玩家選了哪一種塔，這部分邏輯完全不受按鈕是不是動態產生的影響。
   */
  function renderTowerChoiceButtons() {
    const btnClasses = ['warn', '', 'secondary']; // 沿用原本三顆按鈕的配色，超過三種塔就循環使用
    towerChoices.innerHTML = '';
    Object.keys(towerTypes).forEach((type, i) => {
      const t = towerTypes[type];
      const btn = document.createElement('button');
      const cls = btnClasses[i % btnClasses.length];
      if (cls) btn.className = cls;
      btn.dataset.tower = type;
      btn.textContent = `${t.icon} ${t.name}`;
      towerChoices.appendChild(btn);
    });
  }

  function drawBoard() {
    board.innerHTML = '';

    // 步驟 1：畫出路徑上的 29 個格子，並依照格子類型加上對應的 CSS class（start / jump / towerSpot）
    path.forEach(([x,y], i) => {
      const c = document.createElement('div');
      c.className = 'cell path';
      if (i === 0)            c.classList.add('start');
      if (jumpCells.has(i))  c.classList.add('jump');
      if (towerSpots.has(i)) c.classList.add('towerSpot');
      // 座標稍微減掉一點（-4.4%）是為了讓格子的「中心點」對準 path 陣列裡記錄的座標，
      // 因為 CSS 的 left/top 是設定元素左上角的位置，而不是中心點
      c.style.left = `${x-4.4}%`;
      c.style.top  = `${y-4.4}%`;
      c.textContent = i; // 格子上顯示的數字就是它的 index，方便玩家對照路徑
      board.appendChild(c);
    });

    // 終點格：固定畫在棋盤正中央
    const finish = document.createElement('div');
    finish.className = 'cell finish';
    finish.style.left = '43%';
    finish.style.top  = '43%';
    finish.innerHTML  = '終點';
    board.appendChild(finish);

    // 步驟 2：先畫出玩家機坪跟 AI 機坪的「外框」，裡面尚未起飛的飛機圖示等一下才會塞進去
    const pb = document.createElement('div');
    pb.className = 'hangar player';
    pb.innerHTML = `<span class="hangar-label">玩家機坪</span><div class="hangar-planes" id="pbase"></div>`;
    board.appendChild(pb);

    const ab = document.createElement('div');
    ab.className = 'hangar ai';
    ab.innerHTML = `<span class="hangar-label">AI 機坪</span><div class="hangar-planes" id="abase"></div>`;
    board.appendChild(ab);

    // 如果遊戲根本還沒開始過（state 是 undefined），畫完空棋盤跟空機坪就結束，
    // 不執行下面依賴 state 內容的繪製邏輯（避免出現 undefined 相關的錯誤）
    if (!state) return;

    // 把「還停在機坪」的飛機（位置 < 0）畫成小圖示塞進剛剛建立好的機坪容器裡
    const pbaseEl = document.getElementById('pbase');
    const abaseEl = document.getElementById('abase');
    state.player.planes.forEach((p, i) => {
      if (p < 0) {
        const icon = document.createElement('div');
        icon.className = 'hangar-plane';
        icon.innerHTML = planeSVG('#34d399', 270, 0.8); // 玩家用綠色
        icon.title = `玩家 ${i+1}號機`; // 滑鼠移上去會顯示的提示文字
        pbaseEl.appendChild(icon);
      }
    });
    state.ai.planes.forEach((p, i) => {
      if (p < 0) {
        const icon = document.createElement('div');
        icon.className = 'hangar-plane';
        icon.innerHTML = planeSVG('#f87171', 90, 0.8); // AI 用紅色
        icon.title = `AI ${i+1}號機`;
        abaseEl.appendChild(icon);
      }
    });

    // 步驟 3：把 state.towers 裡記錄的每一座塔，依照它的棋盤位置畫出來
    state.towers.forEach(t => {
      const [x,y] = path[t.pos];
      const el = document.createElement('div');
      el.className = 'tower';
      el.style.left  = `${x-3.8}%`;
      el.style.top   = `${y-3.8}%`;
      el.textContent = towerTypes[t.type].icon;
      el.title = `${t.owner==='player'?'玩家':'AI'} ${towerTypes[t.type].name}`;
      board.appendChild(el);
    });

    // 步驟 4：畫出雙方所有「已經離開機坪」的飛機（跑道上或已抵達終點）
    ['player','ai'].forEach(side => {
      const color = side==='player' ? '#34d399' : '#f87171';
      state[side].planes.forEach((pos, idx) => {
        if (pos < 0) return; // 還在機坪的飛機已經在步驟 2 畫過了，這裡跳過
        let x, y;
        if (pos >= finishIndex) {
          // 已抵達終點的飛機，畫在終點格中心附近。
          // 偏移量分成「玩家」「AI」兩組各自的小方陣（玩家偏左上、AI 偏右下），
          // 而不是單純用 idx 從同一組偏移表取值——如果雙方剛好都是 idx 相同的那架飛機抵達終點
          // （例如雙方都先讓 1 號機抵達），用同一組偏移表會算出完全相同的座標，
          // 後畫的 AI（紅色）就會整個蓋住先畫的玩家（綠色）飛機，畫面上看起來像玩家的飛機憑空消失。
          const finishOffsets = {
            player: [[-4,-4],[-1,-4],[-4,-1],[-1,-1]],
            ai:     [[1,1],[4,1],[1,4],[4,4]]
          };
          [x, y] = finishOffsets[side][idx];
          x += 50; y += 50;
        } else {
          // 還在跑道上的飛機，依照 path 座標定位，並依陣營往內/外側各自偏移一點，
          // 這樣同一格如果雙方剛好都有飛機經過時，兩架飛機不會完全重疊
          [x,y] = path[pos];
          x += side==='player' ? -1.5 : 1.5;
          y += side==='player' ?  1.5 : -1.5;
        }
        const rot = pos >= finishIndex ? 270 : planeRotation(pos);
        const p = document.createElement('div');
        p.className = `plane ${side}${pos>=finishIndex?' finished':''}`;
        p.style.left = `${x-2.2}%`;
        p.style.top  = `${y-2.2}%`;
        p.innerHTML  = planeSVG(color, rot);
        p.title      = `${side==='player'?'玩家':'AI'} ${idx+1}號機`;
        board.appendChild(p);
      });
    });
  }

  // ------------------------------------------------------------------
  // 第 4 部分：遊戲規則邏輯
  // ------------------------------------------------------------------

  // 擲骰子：回傳 1~6 的隨機整數（Math.random() 產生 0~1 的小數，乘以 6 取整數再 +1）
  function randomDice() { return Math.floor(Math.random()*6)+1; }

  /**
   * 決定某一方這一回合要移動哪一架飛機。
   * AI 行為規則：優先移動「目前跑在最前面（位置數字最大）的那架還沒到終點的飛機」，
   *   如果所有飛機都還沒起飛（都在機坪），才會挑一架起飛。
   * 這個函式對玩家、AI 都適用（玩家按下「移動」按鈕時，系統也是用同一套規則自動挑飛機，
   * 玩家不用自己選哪一架飛機移動，簡化操作）。
   */
  function choosePlane(side) {
    const planes = state[side].planes;
    let best=-1, bestPos=-999;
    // 找出「已經在跑道上（p>=0）且還沒到終點（p<finishIndex）」的飛機裡，位置最大（最接近終點）的那一架
    planes.forEach((p,i) => { if(p>=0&&p<finishIndex&&p>bestPos){ best=i; bestPos=p; } });
    if (best >= 0) return best;
    // 如果沒有任何一架在跑道上，就找第一架還在機坪（p<0）的飛機準備起飛
    return planes.findIndex(p => p < 0);
  }

  /**
   * 找出某一方目前「跑最前面」的飛機是第幾架（回傳陣列 index），找不到則回傳 -1。
   * 用途：當飛機經過敵方砲塔被擊落時，砲塔的效果是「送最前面的飛機回機坪」，需要用這個函式決定打誰。
   */
  function chooseMostAdvancedPlane(side) {
    let best=-1, bestPos=-999;
    state[side].planes.forEach((p,i) => { if(p>=0&&p<finishIndex&&p>bestPos){ best=i; bestPos=p; } });
    return best;
  }

  /**
   * 計算飛機從 from 移動到 to 的過程中，「實際踩過」的所有格子 index（不含起點 from 本身，含終點 to）。
   * 用來判斷這趟移動有沒有經過敵方的塔（塔的效果是「被經過」時才觸發，不是「停在」那一格才觸發）。
   * Math.max(0, from+1)：如果飛機是剛從機坪起飛（from=-1），要從第 0 格開始算，不能算到 -1+1=0 以前的格子。
   * Math.min(to, finishIndex-1)：避免算到超出棋盤陣列範圍的格子（終點本身不算在塔的判定範圍內）。
   */
  function passedPositions(from, to) {
    const arr=[], start=Math.max(0,from+1), end=Math.min(to,finishIndex-1);
    for(let i=start;i<=end;i++) arr.push(i);
    return arr;
  }

  /**
   * 檢查這趟移動有沒有觸發「敵方」建造的塔，並套用對應效果。
   * mover：正在移動的這一方（'player' 或 'ai'）；opponent 則是另一方（敵方的塔才會生效，自己蓋的塔不會傷到自己）。
   * 依序檢查移動路徑上每一格，只要找到敵方的塔就套用效果：
   *   砲塔（cannon）：把 mover 目前跑最前面的飛機直接送回機坪（位置設回 -1），
   *                    並且 return true 提早結束整個函式——因為飛機已經被打回機坪了，不需要再檢查後面經過的格子。
   *   冰霧塔（freeze）：標記 mover 的 freeze 狀態，下次換 mover 移動時骰子點數會 -1（在 moveSide() 裡處理）
   *   雷達塔（radar）：標記 mover 的 jam 狀態，下次 mover 停在飛躍格時不會觸發額外飛躍（在 moveSide() 裡處理）
   * 回傳值 true/false 代表「這趟移動有沒有被砲塔擊中」，呼叫端 moveSide() 會用這個結果判斷要不要繼續處理飛躍格邏輯。
   */
  function applyTowerEffects(mover, from, to) {
    const opponent = mover==='player'?'ai':'player';
    for (const pos of passedPositions(from,to)) {
      const t = state.towers.find(t=>t.owner===opponent && t.pos===pos);
      if (!t) continue; // 這一格沒有敵方的塔，檢查下一格
      if (t.type==='cannon') {
        const idx = chooseMostAdvancedPlane(mover);
        if (idx>=0) { state[mover].planes[idx]=-1; log(`經過 ${opponent==='player'?'玩家':'AI'} 的砲塔，飛機返回機坪。`, mover==='player'?'p':'a'); return true; }
      }
      if (t.type==='freeze') { state[mover].freeze=true; log(`經過冰霧塔，${mover==='player'?'玩家':'AI'} 下回合骰子 -1。`,'s'); }
      if (t.type==='radar')  { state[mover].jam=true;    log(`經過雷達塔，${mover==='player'?'玩家':'AI'} 下一次不能飛躍。`,'s'); }
    }
    return false;
  }

  /**
   * 執行「某一方這一回合的移動」，是整個遊戲規則裡最核心的一個函式。玩家、AI 都是呼叫這個函式來移動飛機，
   * 差別只在於 side 參數傳 'player' 還是 'ai'，內部邏輯完全共用，確保雙方規則一致。
   *
   * 執行流程：
   *   1. 如果這一方處於「被冰霧塔影響」狀態，本次骰子點數 -1（最少算 1 步），並清除冰霧狀態（只影響一次）
   *   2. 用 choosePlane() 決定要移動哪一架飛機；如果找不到能動的飛機就直接結束（正常遊戲流程不會發生）
   *   3a. 如果這架飛機還在機坪（from<0）：直接起飛到第 0 格（不管骰子點數是多少，只要有擲骰就能起飛）
   *   3b. 如果這架飛機已經在跑道上：
   *       - 計算新位置 to = from + dice
   *       - 如果超過終點，用「折返」規則處理（超過幾格就往回退幾格）
   *       - 如果剛好等於終點，直接標記抵達終點、增加 finished 計數，然後結束（不用再判斷塔或飛躍格）
   *       - 呼叫 applyTowerEffects() 檢查有沒有經過敵方的塔；如果被砲塔打回機坪（陣列裡的位置變成 -1），就直接結束，
   *         不要再往下把 to 覆蓋回飛機的位置（不然就會把「已經被打回機坪」的 -1 又蓋掉）
   *       - 如果最終停在飛躍格：只要沒被雷達干擾，就額外前進 4 格（但不會超過終點）；
   *         如果剛好處於被雷達干擾狀態，則消耗掉這次干擾、不觸發飛躍效果
   *   4. 把算出來的最終位置寫回 actor.planes[idx]
   */
  function moveSide(side) {
    let dice = state.dice;
    const actor = state[side];
    if (actor.freeze) { dice=Math.max(1,dice-1); actor.freeze=false; log(`受到冰霧影響，本回合實際走 ${dice} 步。`, side==='player'?'p':'a'); }
    const idx = choosePlane(side);
    if (idx < 0) { log(`已無可移動飛機。`, side==='player'?'p':'a'); return; }
    let from = actor.planes[idx], to;
    if (from < 0) {
      to = 0; log(`${idx+1}號飛機起飛到起點。`, side==='player'?'p':'a');
    } else {
      to = from + dice;
      if (to > finishIndex) { const over=to-finishIndex; to=finishIndex-over; log(`點數超過終點，折返 ${over} 格。`,'s'); }
      if (to === finishIndex) { actor.planes[idx]=finishIndex; actor.finished++; log(`${idx+1}號飛機抵達終點。`, side==='player'?'p':'a'); return; }
      applyTowerEffects(side, from, to);
      if (actor.planes[idx]===-1) return; // 被砲塔打回機坪了，位置已經在 applyTowerEffects 裡設定過，這裡不用再覆蓋
      if (jumpCells.has(to) && !actor.jam) {
        log(`停在飛躍格，額外飛躍 4 格。`, side==='player'?'p':'a');
        to = Math.min(finishIndex, to+4);
      } else if (jumpCells.has(to) && actor.jam) {
        actor.jam = false; log(`被雷達干擾，本次不能飛躍。`, side==='player'?'p':'a');
      }
    }
    actor.planes[idx] = to;
  }

  // 判斷某一方目前有沒有飛機「停在可建塔格，而且那一格還沒有被蓋過塔」——有的話才能建塔
  function canBuild(side) { return state[side].planes.some(p => towerSpots.has(p) && !state.towers.some(t=>t.pos===p)); }

  /**
   * 在某一方目前停留的可建塔格上，建造指定類型的塔。
   * 找到符合條件（在可建塔格、該格還沒被蓋塔）的飛機位置後，就把一筆新的塔資料 push 進 state.towers，
   * 同時更新已建塔計數（畫面上顯示用的 towerCount，跟送給後端統計用的 currentGame.used_tower_count）。
   * 回傳 true/false 代表這次建塔是否成功（呼叫端目前沒有特別處理失敗的情況，但保留這個回傳值方便未來擴充）。
   */
  function buildTower(side, type) {
    const pos = state[side].planes.find(p => towerSpots.has(p) && !state.towers.some(t=>t.pos===p));
    if (pos===undefined) return false;
    state.towers.push({owner:side,type,pos}); state.towerCount++; state.currentGame.used_tower_count++;
    log(`在 ${pos} 號塔位建立 ${towerTypes[type].name}。`, side==='player'?'p':'a');
    drawBoard(); updateLabels(); return true;
  }

  /**
   * 檢查雙方是否已經有一方獲勝（4 架飛機全部抵達終點）。
   * 這個函式會在每次移動之後被呼叫；一旦判定出勝負，就呼叫 endGame() 結束遊戲並回傳 true，
   * 呼叫端（例如 nextTurn()）看到 true 就知道要提早 return，不要再往下執行換人邏輯。
   */
  function checkWin() {
    if (state.player.planes.every(p=>p>=finishIndex)) return endGame(true);
    if (state.ai.planes.every(p=>p>=finishIndex))     return endGame(false);
    return false;
  }

  /**
   * 結束這一局遊戲，處理所有「結算」相關的畫面與資料更新：
   *   1. 把 state.active 設成 false，並把三顆操作按鈕都 disabled，玩家不能再繼續操作
   *   2. 把這一局的結果記錄進 state.games（本次瀏覽器分頁的歷史紀錄，只存在記憶體）
   *   3. 先在「本地」把玩家的累計勝/敗數 +1，讓結算彈窗可以立刻顯示數字，不用等後端回應
   *   4. 顯示結算彈窗（勝利/失敗大字 + 詳細數據）
   *   5. 呼叫 syncGameEnd()，非同步地把這局結果送去後端存進資料庫、更新真正的累計勝敗數字
   * playerWin 是布林值：true 代表玩家獲勝，false 代表 AI 獲勝。
   */
  function endGame(playerWin) {
    state.active=false; state.currentGame.result=playerWin?1:0; state.currentGame.turn_count=state.round;
    state.games.push({...state.currentGame});
    if (playerWin) state.user.win_count++; else state.user.lose_count++;
    rollBtn.disabled=moveBtn.disabled=endBtn.disabled=true;
    resultText.textContent = playerWin ? '🎉 勝利！' : '😔 失敗';
    resultDetail.innerHTML = `本局回合：<b>${state.round}</b>，建塔數：<b>${state.currentGame.used_tower_count}</b><br>玩家累計：勝 ${state.user.win_count} / 敗 ${state.user.lose_count}`;
    resultModal.classList.add('show');
    log(`遊戲結束：${playerWin?'玩家勝利':'AI勝利'}。`,'s');
    syncGameEnd(playerWin);
    updateLabels(); return true;
  }

  /**
   * 把這一局的結果送到後端 POST /api/games/end，讓伺服器把這局寫進 games 資料表，
   * 同時把整局收集到的 state.towers（雙方建過的每一座塔：類型、棋盤位置、擁有者）一起送過去，
   * 後端會依此逐一建立 GameTower 關聯紀錄，並更新這個使用者在 users 資料表裡的累計勝敗場次
   * （實際邏輯都在 GameService.endGame() 裡）。
   *
   * 如果目前是離線模式（currentUser 是 null，代表一開始沒表明身分成功），就直接跳過、不呼叫後端，
   * 這一局的結果只會留在瀏覽器記憶體裡（state.user 本地端的數字），不會被儲存。
   *
   * 拿到後端回應後，會用「伺服器端算出來的最新累計勝敗數字」去覆蓋畫面上顯示的數字，
   * 確保畫面顯示的是資料庫裡真正的數字（而不是只靠本地端 +1、+1 累加，可能因為重新整理網頁而跟資料庫的值兜不起來）。
   */
  async function syncGameEnd(playerWin) {
    if (!currentUser) return;
    try {
      const data = await apiPost('/api/games/end', {
        userId: currentUser.id,
        result: playerWin ? 'WIN' : 'LOSE',
        turnCount: state.round,
        usedTowerCount: state.currentGame.used_tower_count,
        playerMoves: state.currentGame.player_moves,
        aiMoves: state.currentGame.ai_moves,
        towers: state.towers.map(t => ({ type: t.type, pos: t.pos, owner: t.owner }))
      });
      if (data.success && data.winCount != null) {
        currentUser.winCount = data.winCount;
        currentUser.loseCount = data.loseCount;
        state.user.win_count = data.winCount;
        state.user.lose_count = data.loseCount;
        // 用最新數字重新寫一次結算彈窗的內容（此時彈窗通常還開著，玩家會看到數字從「本地暫算值」更新成「資料庫真實值」）
        resultDetail.innerHTML = `本局回合：<b>${state.round}</b>，建塔數：<b>${state.currentGame.used_tower_count}</b><br>玩家累計：勝 ${state.user.win_count} / 敗 ${state.user.lose_count}`;
        log('本局戰績已儲存至資料庫。', 's');
        loadHistory(); // 這局剛存進資料庫，重新載入一次最近戰績列表，讓畫面立刻反映最新這一局
      }
    } catch (e) {
      log('本局戰績儲存失敗（無法連線後端）。', 's');
    }
  }

  /**
   * 向後端 GET /api/games/history?userId= 取回目前登入者最近 5 局的紀錄，
   * 並呼叫 renderHistory() 把結果畫進「最近戰績」面板。
   * 離線模式（currentUser 是 null）直接跳過，畫面維持顯示原本的提示文字。
   */
  async function loadHistory() {
    if (!currentUser) return;
    try {
      const res = await fetch(`/api/games/history?userId=${currentUser.id}`);
      const games = await res.json();
      renderHistory(games);
    } catch (e) {
      historyBody.innerHTML = '<p class="history-empty">無法載入歷史紀錄（連不上後端）。</p>';
    }
  }

  /**
   * 把最近戰績資料畫成一個表格塞進 #historyBody。
   * 每一列對應一局：結果、對局時間、回合數、玩家/AI 步數、建塔數。
   */
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

  /**
   * 換手：把回合交給下一位行動者，是驅動整場遊戲「一直玩下去」的核心排程函式。
   *
   * 每次呼叫都先呼叫 checkWin()，如果已經分出勝負就直接結束，不再往下換手。
   * 否則依照目前輪到誰，做兩種不同的處理：
   *   - 如果現在是「玩家」剛結束回合：把 turn 切成 'ai'，畫面提示「AI 回合執行中」，
   *     然後用 setTimeout 延遲 650 毫秒後才真正執行 aiTurn()——
   *     這個延遲純粹是為了「使用者體驗」：讓玩家有時間看清楚畫面切換，
   *     不會覺得 AI 是瞬間、跳躍式地完成動作，感覺更像在「真的等對手下棋」。
   *   - 如果現在是「AI」剛結束回合：把 turn 切回 'player'、回合數 +1，
   *     重新啟用「擲骰子」按鈕，讓玩家可以開始下一回合。
   */
  function nextTurn() {
    if (checkWin()) return;
    if (state.turn==='player') {
      state.turn='ai'; state.dice=null; state.moved=false; updateLabels(); drawBoard();
      setTip('AI 回合執行中','AI 會自動擲骰、移動，若停在塔位會隨機建塔。');
      setTimeout(aiTurn, 650);
    } else {
      state.turn='player'; state.round++; state.dice=null; state.moved=false; updateLabels(); drawBoard();
      setTip('輪到玩家','請按「🎲 擲骰子」，再按「✈ 起飛 / 移動」。');
      rollBtn.disabled=false; moveBtn.disabled=true; endBtn.disabled=true;
    }
  }

  /**
   * AI 的完整回合行為，由 nextTurn() 用 setTimeout 延遲呼叫。
   * 依序做：擲骰子 -> 移動（呼叫跟玩家共用的 moveSide('ai')）-> 如果停在可建塔格就隨機選一種塔蓋下去 ->
   * 更新畫面 -> 如果還沒分出勝負，再用 setTimeout 延遲 700 毫秒後呼叫 nextTurn() 把回合交還給玩家
   * （這個延遲同樣是為了讓玩家能看清楚 AI 剛做了什麼動作，才切換回玩家回合）。
   */
  function aiTurn() {
    state.dice=randomDice(); log(`擲出 ${state.dice}。`,'a'); moveSide('ai'); state.currentGame.ai_moves++;
    if (canBuild('ai')) { const types=Object.keys(towerTypes); buildTower('ai', types[Math.floor(Math.random()*types.length)]); }
    updateLabels(); drawBoard(); if (!checkWin()) setTimeout(nextTurn, 700);
  }

  // ------------------------------------------------------------------
  // 第 5 部分：把使用者的按鈕點擊跟上面的遊戲邏輯串起來（事件綁定）
  // ------------------------------------------------------------------

  // 「開始 / 重新開始」：不論目前有沒有進行中的遊戲，按下去都會重新產生一份全新的 state，等於重開一局。
  // 開始遊戲不會呼叫後端——這個專題只需要保存「結束時的最終戰績」，遊戲進行中的狀態全部留在瀏覽器
  // 記憶體裡就好，等 endGame() 分出勝負時才一次把完整結果（含建過的塔）送給後端，見 syncGameEnd()。
  startBtn.addEventListener('click', () => {
    state=newState(); logEl.innerHTML=''; state.active=true;
    rollBtn.disabled=false; moveBtn.disabled=true; endBtn.disabled=true; towerChoices.style.display='none';
    log('遊戲開始。玩家先手。','s');
    setTip('第 1 步：擲骰子','按下「🎲 擲骰子」產生本回合點數。');
    updateLabels(); drawBoard();
  });

  // 「擲骰子」：只有輪到玩家、且遊戲進行中才會生效（避免 AI 回合時玩家亂按）
  rollBtn.addEventListener('click', () => {
    if (!state?.active || state.turn!=='player') return;
    state.dice=randomDice(); log(`擲出 ${state.dice}。`,'p');
    rollBtn.disabled=true; moveBtn.disabled=false; // 擲完骰子後換「移動」按鈕可以按，擲骰按鈕先關閉避免重複擲
    setTip('第 2 步：起飛 / 移動','按下「✈ 起飛 / 移動」，系統會自動選擇最前面的飛機；若都未起飛，會先起飛一架。');
    updateLabels();
  });

  // 「起飛 / 移動」：呼叫 moveSide('player') 執行移動邏輯，移動完檢查能不能建塔、有沒有分出勝負
  moveBtn.addEventListener('click', () => {
    if (!state?.dice) return; moveSide('player'); state.currentGame.player_moves++; state.moved=true; moveBtn.disabled=true; endBtn.disabled=false;
    drawBoard(); updateLabels();
    if (canBuild('player')) { towerChoices.style.display='grid'; setTip('可建立路障塔','你停在固定塔位，可以選一種塔。若不想建塔，可直接結束回合。'); }
    else { setTip('第 3 步：結束回合','本回合移動完成。按「⏎ 結束回合」交給 AI。'); }
    checkWin();
  });

  // 建塔選單：用「事件代理（event delegation）」的方式，把 click 監聽器綁在外層容器 towerChoices 上，
  // 而不是分別綁在三顆按鈕上。點擊時透過 e.target（實際被點到的元素）讀取它的 data-tower 屬性值，
  // 藉此知道玩家選的是哪一種塔；如果點到的地方沒有 data-tower（例如點到容器空白處），直接 return 不處理。
  towerChoices.addEventListener('click', e => {
    const type = e.target.dataset.tower; if (!type) return;
    buildTower('player', type); towerChoices.style.display='none';
    setTip('塔已建立','塔只會在敵方飛機經過該格時觸發效果。現在可以結束回合。');
  });

  // 「結束回合」：把建塔選單收起來、三顆按鈕都先關閉，然後呼叫 nextTurn() 把回合交給 AI
  endBtn.addEventListener('click', () => {
    towerChoices.style.display='none'; rollBtn.disabled=moveBtn.disabled=endBtn.disabled=true; nextTurn();
  });

  // 結算彈窗的「回到遊戲」按鈕：單純把 .show 這個 class 移除，讓彈窗依照 CSS 規則變回隱藏狀態
  closeModal.addEventListener('click', () => resultModal.classList.remove('show'));

  // 「登入」按鈕：拿輸入框目前的名字重新表明身分。如果玩家在遊戲玩到一半才改名字按登入，
  // 只會影響「之後」開新局時 newState() 帶入的顯示資料，不會打斷正在進行中的這一局。
  loginBtn.addEventListener('click', () => identifyUser(usernameInput.value));
  // Enter 鍵也能觸發登入，不用一定要用滑鼠點按鈕
  usernameInput.addEventListener('keydown', e => { if (e.key === 'Enter') identifyUser(usernameInput.value); });

  // ------------------------------------------------------------------
  // 整支程式最後實際執行的部分：
  // 1. 先畫一次空棋盤（讓使用者一打開網頁就看到棋盤外觀，即使還沒按開始）
  // 2. 用 FALLBACK_TOWER_TYPES 先把建塔選單畫出來，確保就算後面兩個非同步請求都失敗，
  //    畫面上還是有完整、可以互動的塔種選單
  // 3. 非同步向後端載入真正的塔種設定（成功的話會覆蓋掉步驟 2 畫出來的內容）
  // 4. 用輸入框預設值（player1）非同步向後端表明身分
  // 這三個非同步呼叫互不相依，不會互相卡住，也都不會擋住畫面渲染。
  // ------------------------------------------------------------------
  drawBoard();
  renderTowerChoiceButtons();
  loadTowerTypes();
  identifyUser(usernameInput.value);
})();
