# 21點：博弈紛爭 - Agent 指南

## 專案概述

21點：博弈紛爭 - 支援單人練習 (PVE) 和多人對戰 (PVP) 模式的對戰遊戲。
採用 Java Socket 實作 Client-Server 架構。

## 目錄結構

```
im-java/
├── BlackjackServer/          # 伺服器程式
│   ├── src/
│   │   ├── Main.java         # 進入點
│   │   ├── server/           # 網路層
│   │   ├── game/             # 遊戲邏輯
│   │   ├── model/            # 資料模型
│   │   ├── protocol/         # 通訊協定
│   │   └── command/          # 命令模式 (Command Pattern)
│   └── out/                  # 編譯輸出
│
├── BlackjackClient/          # 客戶端程式
│   ├── src/
│   │   ├── Main.java         # 進入點
│   │   ├── client/           # 主視窗與網路
│   │   │   ├── BlackjackClient.java
│   │   │   ├── NetworkClient.java
│   │   │   └── UserConfig.java      # 本地配置管理（UID、暱稱、IP）
│   │   ├── handler/          # 訊息處理
│   │   ├── ui/               # UI 元件
│   │   ├── protocol/         # 通訊協定
│   │   └── command/          # 命令模式 (Command Pattern)
│   ├── out/                  # 編譯輸出
│   └── user_config.txt       # 本地配置檔（自動生成）
│
└── AGENTS.md                 # 本文件
```

## 編譯與執行

### Server

```bash
cd BlackjackServer
javac -encoding UTF-8 -d out -sourcepath src src/Main.java src/server/*.java src/game/*.java src/model/*.java src/protocol/*.java src/command/*.java
java -cp out Main
```

### Client

```bash
cd BlackjackClient
javac -encoding UTF-8 -d out -sourcepath src src/Main.java src/client/*.java src/handler/*.java src/ui/*.java src/protocol/*.java src/command/*.java
java -cp out Main
```

## 通訊協定

指令格式：`COMMAND|PARAM1|PARAM2|...`

**Client → Server:**
- `LOGIN|uid|name` - 登入（包含 UID 和暱稱）
- `PVE_START` - 開始單人練習
- `CREATE_ROOM` - 創建房間
- `JOIN_ROOM|roomId` - 加入房間
- `START` - 開始遊戲 (莊家限定)
- `HIT` / `STAND` - 遊戲行動
- `CHAT|message` - 聊天（訊息內容可包含 `|` 符號，處理時需將所有參數重新組合）
- `LEAVE` - 離開房間
- `USE_FUNC_CARD|cardId|targetUid` - 使用功能牌
- `SKIP_FUNC_CARD` - 跳過使用功能牌

**Server → Client:**
- `LOGIN_OK` - 登入成功
- `STATE|status|dealerHand|playerHand|playerList` - 遊戲狀態
- `TURN|YOUR/WAIT` - 輪次通知
- `GAME_OVER|dealerHand|playerHand|result` - 回合結束
- `FUNC_CARDS|id,type;id,type;...` - 功能牌列表
- `FUNC_CARD_USED|userName|cardType|targetName` - 功能牌使用通知

## 程式碼慣例

- 類別名稱：PascalCase
- 方法/變數：camelCase
- 常數：UPPER_SNAKE_CASE
- 縮排：4 空格
- 註解語言：中文

## 遊戲流程

### PVP 模式流程

PVP 模式分為兩個層級：**一場遊戲（Game）** 與 **一回合（Round）**。

#### 一場遊戲（Game）

- **開始條件**：房主第一次按下「開始遊戲」按鈕
- **結束條件**：只剩最後一名玩家 HP > 0（或全員同時淘汰判定平局）
- **勝利後重置**：所有玩家 HP 重置為 15，旁觀者恢復為正常玩家，功能牌清空並重新發放

#### 一回合（Round）

每一回合是一次完整的 21 點遊戲：

```
┌─────────────────────────────────────────────────────────────┐
│  發牌階段 → 機會卡階段 → 玩家輪流行動 → 回合結算 → 確認戰績  │
└─────────────────────────────────────────────────────────────┘
```

1. **發牌階段**：莊家按「開始」，所有非旁觀者玩家各發 2 張牌
2. **機會卡階段**：按順序輪流使用或跳過功能牌
3. **玩家行動階段**：從莊家下一位開始輪流 HIT / STAND
   - **莊家開牌時機**：當輪到莊家行動時，莊家的所有手牌自動揭示給所有玩家
4. **回合結算**：所有玩家完成行動後，計算勝負並扣血
5. **確認戰績**：所有玩家確認結果後，進入下一回合（或判定勝利）

#### 回合間狀態

- 莊家輪替：每回合結束後，莊家自動換到下一位非旁觀者
- 旁觀者轉換：HP 歸零者變為旁觀者；下回合開始前，旁觀者恢復為正常玩家

---

## 遊戲規則

- 玩家初始 15 HP
- 閒家爆牌或輸給莊家：-1 HP
- 莊家爆牌：-2 HP
- 莊家消極懲罰（不拿牌且未獲勝）：-1 HP
- HP 歸零則變為旁觀者（不被踢出房間）

### 勝利條件

- 當房間內只剩下一名非旁觀者玩家時，該玩家獲勝
- 勝利時會跳出「遊戲勝利」彈窗通知所有玩家
- 勝利後所有玩家（包含旁觀者）HP 重置為 15，恢復正常玩家狀態
- 若所有玩家同時 HP 歸零，則判定為平局並重置

### 功能牌（機會卡）

- **發牌時機**：第一場遊戲開始時，每位玩家獲得 3 張功能牌（隨機分配類型）
- **使用時機**：只能在機會卡階段使用（莊家點擊「開始遊戲」後，進入機會卡階段時）
- **按鈕狀態管理**：
  - 遊戲進行中（發牌階段、玩家行動階段）：機會牌按鈕禁用
  - 回合結束後：機會牌按鈕禁用（即使玩家已確認戰績）
  - 機會卡階段且輪到自己時：機會牌按鈕啟用
  - 機會卡階段但未輪到自己時：機會牌按鈕禁用
- **PVE 模式**：不發放功能牌
- **目前功能牌**：
  - **做個交易**：與一位玩家互換手牌（需選擇目標）
  - **我叫你抽**：強迫所有玩家抽一張牌（若有人爆牌，在 21 點遊戲前僅通知不扣血）
  - **我喝一口**：回復 1 HP（允許 HP 超過 15）
- **架構説明**：採用策略模式 (Strategy Pattern)，效果类別位於 `game/` 目錄：
  - `FunctionCardEffect.java`：策略介面
  - `FunctionCardEffectRegistry.java`：效果註冊表
  - `MakeADealEffect.java`、`ForceDrawEffect.java`、`HealOneHpEffect.java`：各效果實作


### 旁觀者模式

- **觸發條件**：
  - 玩家在遊戲進行中加入房間（中途加入）
  - 玩家 HP 歸零時自動轉為旁觀者
- **旁觀者類型**：
  - **中途加入旁觀者**：需等到**這場遊戲結束**才能參與
  - **HP 歸零旁觀者**：下一**回合**開始時恢復為正常玩家
- **旁觀者視角（上帝視角）**：
  - 上方莊家區域：顯示莊家完整手牌
  - 玩家列表：與莊家相同，包含所有閒家手牌
  - 下方手牌區：顯示當前活動玩家的手牌
- **旁觀者限制**：
  - 不會被發牌
  - 無法執行遊戲操作（HIT、STAND）
  - 無法確認戰績（READY）
  - 不參與回合結算和血量變化
- **識別標記**：旁觀者在玩家列表中會顯示 "(旁觀)" 標記

### 玩家離開規則

- **莊家離開（遊戲中）**：取消本回合，**不計分**，通知所有玩家
- **閒家離開（遊戲中）**：遊戲繼續，自動調整輪次
- **旁觀者離開**：靜默移除
- **離開後只剩一名活躍玩家**：顯示勝利通知
- **離開後只剩旁觀者**：結束這場遊戲，**無勝利通知**

## 用戶識別與本地儲存

### UID（用戶識別碼）
- 客戶端首次啟動時自動生成 UUID 格式的 UID
- UID 儲存於本地配置檔 `user_config.txt`
- 伺服器使用 UID 作為玩家的唯一標識符
- **限制**：為避免混淆，同一個房間內不允許出現重複的玩家暱稱
- **注意**：UID 僅用於後端邏輯，所有 GUI 顯示都使用玩家暱稱

### 本地配置儲存
- 配置檔案：`user_config.txt`（位於客戶端根目錄）
- 儲存內容：`uid|name|serverIp`
- 客戶端啟動時自動載入上次使用的暱稱和伺服器 IP
- 連線成功後自動儲存當前配置

## 設計模式

### Command Pattern（命令模式）

專案採用命令模式處理客戶端與伺服器之間的通訊，將 switch-case 區塊重構為獨立的命令物件。

#### 伺服器端架構

```
command/
├── Command.java              # 命令介面
├── CommandContext.java       # 命令執行上下文
├── CommandRegistry.java      # 命令註冊表
├── LoginCommand.java         # 登入命令
├── PveStartCommand.java      # PVE 開始命令
├── CreateRoomCommand.java    # 創建房間命令
├── JoinRoomCommand.java      # 加入房間命令
├── LeaveCommand.java         # 離開房間命令
├── StartGameCommand.java     # 開始遊戲命令
├── ReadyCommand.java         # 準備命令
├── ChatCommand.java          # 聊天命令
├── HitCommand.java           # 要牌命令
├── StandCommand.java         # 停牌命令
├── SkipFunctionCardCommand.java  # 跳過功能牌命令
└── UseFunctionCardCommand.java   # 使用功能牌命令
```

**機會牌效果類別**：
```
game/
├── FunctionCardEffect.java        # 效果策略介面
├── FunctionCardEffectRegistry.java# 效果註冊表
├── MakeADealEffect.java           # 「做個交易」效果
├── ForceDrawEffect.java           # 「我叫你抽」效果
└── HealOneHpEffect.java           # 「我喝一口」效果

**核心類別**：
- `Command`：定義 `execute(CommandContext context)` 方法
- `CommandContext`：封裝 `ClientHandler`、命令參數、房間列表
- `CommandRegistry`：透過 `getCommand(action)` 取得對應命令

#### 客戶端架構

```
command/
├── ServerMessageHandler.java     # 訊息處理器介面
├── MessageContext.java           # 訊息處理上下文
├── MessageHandlerRegistry.java   # 訊息處理器註冊表
├── LoginOkHandler.java           # 登入成功處理器
├── PveStartedHandler.java        # PVE 開始處理器
├── RoomCreatedHandler.java       # 房間創建處理器
├── RoomJoinedHandler.java        # 加入房間處理器
├── StateHandler.java             # 狀態更新處理器
├── TurnHandler.java              # 輪次通知處理器
├── GameOverHandler.java          # 回合結束處理器
├── HpUpdateHandler.java          # HP 更新處理器
├── ChatHandler.java              # 聊天處理器
├── MsgHandler.java               # 系統訊息處理器
├── LobbyHandler.java             # 返回大廳處理器
├── FunctionCardsHandler.java     # 功能牌列表處理器
├── FunctionCardUsedHandler.java  # 功能牌使用通知處理器
├── FunctionCardPhaseHandler.java # 功能牌階段處理器
├── FunctionCardPhaseEndHandler.java  # 功能牌階段結束處理器
├── GameWinHandler.java           # 遊戲勝利處理器
├── RoundCancelHandler.java       # 回合取消處理器
└── ErrorHandler.java             # 錯誤處理器
```

**核心類別**：
- `ServerMessageHandler`：定義 `handle(MessageContext context)` 方法
- `MessageContext`：封裝 `BlackjackClient`、訊息參數
- `MessageHandlerRegistry`：透過 `getHandler(cmd)` 取得對應處理器

#### 新增命令步驟

**伺服器端新增命令**：
1. 建立新命令類別實作 `Command` 介面
2. 在 `CommandRegistry.registerCommands()` 中註冊
3. 在 `Protocol.java` 新增協定常數（如有需要）

**客戶端新增處理器**：
1. 建立新處理器類別實作 `ServerMessageHandler` 介面
2. 在 `MessageHandlerRegistry.registerHandlers()` 中註冊
3. 在 `Protocol.java` 新增協定常數（如有需要）

---

## 測試與驗證

### 單元測試

目前專案尚未建立正式的單元測試框架，但建議針對以下模組進行測試：

**核心邏輯測試**：
- `model/Hand.java`：測試 21 點計算、A 值轉換邏輯
- `model/Deck.java`：測試洗牌、發牌功能
- `model/FunctionCard.java`：測試功能牌效果

**命令模式測試**：
- 各個 `Command` 類別：測試命令執行結果
- `CommandRegistry`：測試命令註冊與查找
- 各個 `ServerMessageHandler`：測試訊息處理邏輯

### 整合測試流程

**PVE 模式測試**：
```bash
# 1. 啟動 Server
cd BlackjackServer
java -cp out Main

# 2. 啟動 Client
cd BlackjackClient  
java -cp out Main

# 3. 測試步驟
- 登入
- 選擇「單人練習」
- 執行數回合遊戲
- 驗證：點數計算、爆牌判定、勝負結果
```

**PVP 模式測試**：
```bash
# 1. 啟動 Server
cd BlackjackServer
java -cp out Main

# 2. 啟動多個 Client（至少 3 個）
cd BlackjackClient
java -cp out Main  # 重複開啟 3 次

# 3. 測試步驟
- Client A 創建房間
- Client B、C 加入房間
- 測試場景：
  ✓ 正常遊戲流程（發牌 → 機會卡 → 行動 → 結算）
  ✓ 玩家中途離開
  ✓ HP 歸零變為旁觀者
  ✓ 勝利條件觸發
  ✓ 莊家輪替
  ✓ 功能牌使用
```

### 驗證清單

執行以下檢查確保功能正常：

- [ ] 伺服器啟動成功，監聽 Port 12345
- [ ] 客戶端能夠連線到伺服器
- [ ] 登入系統正常（UID 自動生成與儲存）
- [ ] PVE 模式能夠正常開始遊戲
- [ ] PVP 房間創建與加入功能正常
- [ ] 遊戲中聊天功能正常（包含 `|` 符號）
- [ ] 發牌功能正常（2 張牌給每位玩家）
- [ ] 機會卡階段按順序執行
- [ ] HIT/STAND 功能正常
- [ ] 21 點計算正確（A 值自動調整）
- [ ] 回合結算 HP 扣除正確
- [ ] 莊家消極懲罰正確觸發
- [ ] HP 歸零後轉為旁觀者
- [ ] 旁觀者視角（上帝視角）顯示正確
- [ ] 勝利條件正確判定
- [ ] 玩家離開後遊戲流程正常
- [ ] 功能牌使用效果正確
- [ ] 中文字體顯示正常

---

## 調試技巧

### 伺服器端調試

**啟用詳細日誌**：
```java
// 在 ClientHandler.java 中加入調試訊息
System.out.println("[DEBUG] 收到命令: " + command);
System.out.println("[DEBUG] 玩家 " + player.getName() + " 狀態: " + player.getHp());
```

**追蹤房間狀態**：
```java
// 在 GameRoom.java 中輸出房間狀態
System.out.println("[DEBUG] 房間 " + roomId + " 玩家數: " + players.size());
System.out.println("[DEBUG] 當前輪次: " + currentPlayerIndex + "/" + players.size());
```

**網路層調試**：
```java
// 在 BlackjackServer.java 中追蹤連線
System.out.println("[DEBUG] 新客戶端連線: " + socket.getInetAddress());
```

### 客戶端調試

**GUI 狀態檢查**：
```java
// 在 GamePanel.java 中輸出狀態
System.out.println("[DEBUG] 當前狀態: " + currentState);
System.out.println("[DEBUG] 玩家手牌: " + playerHandArea.getText());
```

**訊息處理追蹤**：
```java
// 在 MessageHandler 中輸出收到的訊息
System.out.println("[DEBUG] 收到伺服器訊息: " + message);
```

### 常用調試指令

```bash
# 監聽網路連線（Windows）
netstat -ano | findstr :12345

# 查看 Java 進程
jps -l

# 強制終止進程
taskkill /F /PID <PID>
```

---

## 常見問題與解法

### 連線問題

**問題：客戶端無法連線到伺服器**
- **檢查**：確認伺服器已啟動並顯示「伺服器啟動於 Port 12345」
- **檢查**：確認防火牆未阻擋 Port 12345
- **檢查**：本機測試使用 `127.0.0.1`，區域網路測試使用伺服器的區網 IP
- **解決**：執行 `netstat -ano | findstr :12345` 確認 Port 已被監聽

**問題：連線後立即斷開**
- **檢查**：客戶端與伺服器的 Protocol 版本是否一致
- **檢查**：是否有未捕捉的例外導致線程終止
- **解決**：查看伺服器與客戶端的錯誤輸出

### 遊戲邏輯問題

**問題：點數計算錯誤**
- **檢查**：`Hand.java` 中的 `getValue()` 方法
- **重點**：A 值應在 1 和 11 之間動態調整以避免爆牌
- **測試**：建立包含 A 的手牌並驗證計算結果

**問題：莊家第二張牌始終隱藏**
- **檢查**：伺服器端是否在莊家行動時發送完整手牌
- **檢查**：`STATE` 訊息中的 `dealerHand` 參數格式
- **解決**：確保莊家輪次時 `dealerHand` 包含所有牌（非隱藏）

**問題：機會牌按鈕無法點擊**
- **檢查**：當前遊戲階段是否為「機會卡階段」
- **檢查**：是否輪到該玩家使用機會卡
- **檢查**：`FunctionCardPanel` 的 `setEnabled()` 狀態
- **解決**：確保收到 `FUNC_CARD_PHASE|YOUR` 時啟用按鈕

### UI 問題

**問題：中文字體無法顯示（顯示為方塊）**
- **原因**：預設字體不支援中文
- **解決**：在 UI 元件中明確設定支援中文的字體
```java
Font chineseFont = new Font("Microsoft JhengHei", Font.PLAIN, 14);
component.setFont(chineseFont);
```

**問題：視窗大小不足以顯示所有元件**
- **檢查**：`setPreferredSize()` 是否設定正確
- **解決**：調整面板大小或使用 `pack()` 自動調整

### 編譯問題

**問題：`javac` 編碼錯誤**
```
error: unmappable character for encoding MS950
```
- **解決**：加入 `-encoding UTF-8` 參數
```bash
javac -encoding UTF-8 -d out -sourcepath src src/Main.java ...
```

**問題：找不到類別**
```
Error: Could not find or load main class Main
```
- **解決**：確認編譯輸出目錄為 `out`，執行時使用 `-cp out`
```bash
java -cp out Main
```

---

## 安全性考量

### 輸入驗證

**伺服器端必須驗證**：
- 玩家暱稱長度限制（建議 2-20 字元）
- 聊天訊息長度限制（建議最多 200 字元）
- 房間 ID 格式驗證
- 命令參數數量驗證

**防止注入攻擊**：
```java
// 不安全：直接使用用戶輸入
String message = parts[1]; // 可能包含惡意內容

// 安全：驗證與過濾
if (message.length() > 200) {
    message = message.substring(0, 200);
}
// 移除潛在的控制字元
message = message.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
```

### 訊息分隔符號處理

**重要**：聊天訊息可能包含 `|` 符號，必須特殊處理：
```java
// ChatCommand.java 中的正確處理方式
if (parts.length < 2) {
    return;
}
// 將除第一個參數外的所有部分重組為訊息
StringBuilder msgBuilder = new StringBuilder();
for (int i = 1; i < parts.length; i++) {
    if (i > 1) msgBuilder.append("|");
    msgBuilder.append(parts[i]);
}
String message = msgBuilder.toString();
```

### 資源管理

**防止記憶體洩漏**：
- 玩家離開時必須清理資源：
  ```java
  // 關閉 Socket
  socket.close();
  // 從房間移除玩家
  room.removePlayer(player);
  // 清除玩家引用
  players.remove(uid);
  ```

**連線數量限制**：
```java
// 建議在 Server 中限制最大連線數
private static final int MAX_CONNECTIONS = 100;
private final Semaphore connectionLimit = new Semaphore(MAX_CONNECTIONS);

// accept 前檢查
if (!connectionLimit.tryAcquire()) {
    socket.close();
    continue;
}
```

### 並發安全

**共享資源必須同步**：
```java
// GameRoom.java 中的玩家列表
private final List<Player> players = Collections.synchronizedList(new ArrayList<>());

// 或使用明確的鎖
private final Object lock = new Object();
synchronized (lock) {
    // 修改共享狀態
}
```

---

## 性能優化

### 網路優化

**批次發送訊息**：
```java
// 避免對每位玩家單獨發送
for (Player p : players) {
    p.send("STATE|...");  // ❌ 效率低
}

// 優先：準備好訊息後批次發送
String stateMsg = buildStateMessage();
broadcast(stateMsg);  // ✅ 一次性廣播
```

**減少不必要的狀態更新**：
- 只在狀態實際改變時發送 `STATE` 訊息
- 使用增量更新而非完整狀態

### 記憶體優化

**重用物件**：
```java
// Deck.java 中重用 Card 物件
private static final List<Card> CARD_POOL = initializeCardPool();

public void reset() {
    cards.clear();
    cards.addAll(CARD_POOL);
    shuffle();
}
```

**及時清理**：
```java
// 回合結束後清理手牌
hand.clear();
// 遊戲結束後清理房間
rooms.remove(roomId);
```

### UI 性能

**SwingUtilities.invokeLater 使用**：
```java
// 所有 UI 更新必須在 EDT 中執行
SwingUtilities.invokeLater(() -> {
    updateGameState(state);
});
```

**減少重繪**：
```java
// 批次更新，減少重繪次數
panel.setVisible(false);
// ... 多個 UI 修改 ...
panel.setVisible(true);
panel.revalidate();
panel.repaint();
```

---

## 擴展建議

### 新增功能牌

1. 在 `FunctionCardType.java` 中新增類型：
```java
STEAL_CARD("偷取卡牌", "從對手抽一張牌"),
DOUBLE_DOWN("雙倍下注", "本回合輸贏 HP 翻倍");
```

2. 在伺服器的功能牌處理邏輯中實作效果

3. 在客戶端 `FunctionCardPanel` 中新增對應的 UI

### 新增遊戲模式

**可能的擴展方向**：
- 計分模式（非 HP 制）
- 錦標賽模式（多房間淘汰賽）
- 觀戰模式（純觀看，不參與）
- 排名系統（記錄勝場與積分）

### 資料持久化

**建議實作**：
- 玩家統計資料保存（勝率、場次）
- 歷史紀錄查詢
- 排行榜系統

**實作方式**：
- 使用 JSON 檔案儲存（適合小型專案）
- 使用 SQLite 資料庫（適合中型專案）
- 使用 MySQL/PostgreSQL（適合大型專案）

---

## 開發環境建議

### 推薦 IDE

- **IntelliJ IDEA**（推薦）：強大的 Java 開發工具
- **Eclipse**：輕量且免費
- **VS Code** + Java Extension Pack：適合輕量開發

### 推薦工具

- **Git**：版本控制
- **Postman**（選用）：測試 Socket 通訊
- **JUnit**（計劃中）：單元測試框架
- **Maven/Gradle**（未來考慮）：依賴管理與建構工具

### 開發流程建議

1. **功能開發**：
   - 在專案根目錄建立功能分支：`git checkout -b feature/功能名稱`
   - 實作並測試功能
   - 提交變更：`git commit -m "描述"`

2. **程式碼審查**：
   - 檢查程式碼風格是否一致
   - 驗證錯誤處理是否完善
   - 確認註解清晰易懂

3. **合併主分支**：
   - 測試通過後合併到主分支
   - 更新 `AGENTS.md`（如有架構變更）

---

## 附錄

### 完整協定列表

詳見 `protocol/Protocol.java`，以下為完整清單：

**Client → Server**：
- `LOGIN|uid|name` - 登入
- `PVE_START` - 開始單人練習
- `CREATE_ROOM` - 創建房間
- `JOIN_ROOM|roomId` - 加入房間
- `START` - 開始遊戲
- `READY` - 確認戰績
- `CHAT|message` - 聊天
- `HIT` - 要牌
- `STAND` - 停牌
- `LEAVE` - 離開房間
- `USE_FUNC_CARD|cardId|targetUid` - 使用功能牌
- `SKIP_FUNC_CARD` - 跳過功能牌

**Server → Client**：
- `LOGIN_OK` - 登入成功
- `PVE_STARTED` - PVE 開始
- `ROOM_CREATED|roomId` - 房間已創建
- `ROOM_JOINED|roomId|playersInfo` - 加入房間成功
- `STATE|status|dealerHand|playerHand|playerList` - 遊戲狀態
- `TURN|YOUR/WAIT` - 輪次通知
- `GAME_OVER|dealerHand|playerHand|result` - 回合結束
- `HP_UPDATE|hp` - HP 更新
- `MSG|message` - 系統訊息
- `LOBBY` - 返回大廳
- `ERROR|message` - 錯誤訊息
- `GAME_WIN|winnerName` - 遊戲勝利
- `ROUND_CANCEL|reason` - 回合取消
- `FUNC_CARDS|id,type;id,type;...` - 功能牌列表
- `FUNC_CARD_USED|userName|cardType|targetName` - 功能牌使用通知
- `FUNC_CARD_PHASE|YOUR/WAIT` - 機會卡階段
- `FUNC_PHASE_END` - 機會卡階段結束

### 資料模型

**Card（卡牌）**：
- `suit`：花色（♠ ♥ ♦ ♣）
- `rank`：點數（A, 2-10, J, Q, K）
- `value`：數值（A=1/11, J/Q/K=10）

**Hand（手牌）**：
- `cards`：卡牌列表
- `getValue()`：計算總點數（自動處理 A 值）
- `isBust()`：是否爆牌（點數 > 21）

**FunctionCard（功能牌）**：
- `id`：唯一識別碼
- `type`：功能牌類型（`FunctionCardType`）

**Player（玩家，伺服器端）**：
- `uid`：唯一識別碼
- `name`：暱稱
- `hp`：血量
- `hand`：當前手牌
- `isSpectator`：是否為旁觀者
- `functionCards`：擁有的功能牌

### 錯誤碼定義

目前使用文字錯誤訊息，未來可考慮定義錯誤碼：

```java
// 建議的錯誤碼系統
public enum ErrorCode {
    E001("房間已滿"),
    E002("房間不存在"),
    E003("非法操作"),
    E004("未輪到您"),
    E005("暱稱重複");
    
    private final String message;
    ErrorCode(String message) {
        this.message = message;
    }
}
```

---

## 聯絡資訊

如需協助或回報問題，請聯絡開發團隊或提交 Issue。

---

**當前版本**：v0.1.1  
**最後更新**：2025-12-28