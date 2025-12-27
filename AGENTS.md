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
- `CHAT|message` - 聊天
- `LEAVE` - 離開房間

**Server → Client:**
- `LOGIN_OK` - 登入成功
- `STATE|status|dealerHand|playerHand|playerList` - 遊戲狀態
- `TURN|YOUR/WAIT` - 輪次通知
- `GAME_OVER|dealerHand|playerHand|result` - 回合結束

## 程式碼慣例

- 類別名稱：PascalCase
- 方法/變數：camelCase
- 常數：UPPER_SNAKE_CASE
- 縮排：4 空格
- 註解語言：中文

## 遊戲規則

- 玩家初始 15 HP
- 閒家爆牌或輸給莊家：-1 HP
- 莊家爆牌：-2 HP
- 莊家消極懲罰（不拿牌且未獲勝）：-1 HP
- HP 歸零則淘汰

### 旁觀者模式

- **觸發條件**：玩家在遊戲進行中加入房間（包含中途加入或斷線重連）
- **旁觀者限制**：
  - 不會被發牌，手牌區為空
  - 只能看到莊家的第一張牌（蓋牌狀態）
  - 無法執行遊戲操作（HIT、STAND）
  - 無法確認戰績（READY）
  - 不參與回合結算和血量變化
  - 不被計入輪次順序
- **轉為正常玩家**：當前局結束後，旁觀者自動轉為正常玩家，可參與下一局遊戲
- **識別標記**：旁觀者在玩家列表中會顯示 "(旁觀)" 標記

## 用戶識別與本地儲存

### UID（用戶識別碼）
- 客戶端首次啟動時自動生成 UUID 格式的 UID
- UID 儲存於本地配置檔 `user_config.txt`
- 伺服器使用 UID 作為玩家的唯一標識符，避免重名問題
- **注意**：UID 僅用於後端邏輯，所有 GUI 顯示都使用玩家暱稱

### 本地配置儲存
- 配置檔案：`user_config.txt`（位於客戶端根目錄）
- 儲存內容：`uid|name|serverIp`
- 客戶端啟動時自動載入上次使用的暱稱和伺服器 IP
- 連線成功後自動儲存當前配置
