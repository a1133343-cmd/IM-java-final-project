# Blackjack 21點遊戲 - Agent 指南

## 專案概述

21點 PVP 對戰遊戲，支援單人練習 (PVE) 和多人對戰 (PVP) 模式。
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
│   │   └── protocol/         # 通訊協定
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
│   │   └── protocol/         # 通訊協定
│   ├── out/                  # 編譯輸出
│   └── user_config.txt       # 本地配置檔（自動生成）
│
└── AGENTS.md                 # 本文件
```

## 編譯與執行

### Server

```bash
cd BlackjackServer
javac -d out -sourcepath src src/Main.java src/server/*.java src/game/*.java src/model/*.java src/protocol/*.java
java -cp out Main
```

### Client

```bash
cd BlackjackClient
javac -d out -sourcepath src src/Main.java src/client/*.java src/handler/*.java src/ui/*.java src/protocol/*.java
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

- **發牌時機**：第一場遊戲開始時，每位玩家獲得 3 張功能牌
- **使用時機**：只能在機會卡階段使用（莊家點擊「開始遊戲」後，進入機會卡階段時）
- **按鈕狀態管理**：
  - 遊戲進行中（發牌階段、玩家行動階段）：機會牌按鈕禁用
  - 回合結束後：機會牌按鈕禁用（即使玩家已確認戰績）
  - 機會卡階段且輪到自己時：機會牌按鈕啟用
  - 機會卡階段但未輪到自己時：機會牌按鈕禁用
- **PVE 模式**：不發放功能牌
- **目前功能牌**：
  - **做個交易**：與一位玩家互換手牌


### 旁觀者模式

- **觸發條件**：
  - 玩家在遊戲進行中加入房間（中途加入）
  - 玩家 HP 歸零時自動轉為旁觀者
- **旁觀者類型**：
  - **中途加入旁觀者**：需等到**這場遊戲結束**才能參與
  - **HP 歸零旁觀者**：下一**回合**開始時恢復為正常玩家
- **旁觀者限制**：
  - 不會被發牌，手牌區為空
  - 只能看到莊家的第一張牌（蓋牌狀態）
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
