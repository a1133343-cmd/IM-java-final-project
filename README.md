# 21點：博弈紛爭

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![License](https://img.shields.io/badge/License-MIT-blue)
![Status](https://img.shields.io/badge/Status-Active-success)

多人線上 21 點對戰遊戲，支援 PVE 單人練習與 PVP 多人亂鬥模式。採用 Java Socket 架構，命令模式設計，提供流暢的多人對戰體驗。

---

## 目錄

- [功能特色](#功能特色)
- [遊戲規則](#遊戲規則)
- [系統需求](#系統需求)
- [安裝與設定](#安裝與設定)
- [快速開始](#快速開始)
- [遊戲指南](#遊戲指南)
- [專案結構](#專案結構)
- [技術細節](#技術細節)
- [設計模式](#設計模式)
- [疑難排解](#疑難排解)
- [常見問題](#常見問題)
- [開發團隊](#開發團隊)
- [貢獻指南](#貢獻指南)
- [授權條款](#授權條款)

---

## 功能特色

- 🎮 **PVE 單人練習** - 與電腦對戰，磨練技巧
- 👥 **PVP 多人對戰** - 支援最多 5 人同房對戰
- 💬 **即時聊天** - 遊戲中與其他玩家即時交流
- ❤️ **生存機制** - 15 HP 血量系統，輸牌扣血，歸零轉為旁觀
- ⚖️ **莊家輪替** - 每回合結束後莊家自動輪換，公平競技
- 🎯 **消極懲罰** - 莊家不拿牌且未獲勝將被扣血，防止消極避戰
- 🃏 **機會卡系統** - 功能牌為對局增添策略變化與戰術深度
- 👁️ **旁觀者模式** - 上帝視角觀戰，掌握全局
- 🔄 **自動重連** - 斷線重連保留遊戲進度
- 💾 **本地配置** - 自動保存玩家資料與伺服器設定

---

## 遊戲規則

### PVP 遊戲流程

PVP 模式使用 **場 (Game)** 與 **回合 (Round)** 雙層結構：

| 層級       | 定義                                         | 勝利條件                       |
| ---------- | -------------------------------------------- | ------------------------------ |
| **一場遊戲** | 房主首次按「開始」→ 只剩一名玩家 HP > 0     | 勝利後所有玩家 HP 重置為 15 |
| **一回合**   | 發牌 → 機會卡階段 → 玩家行動 → 結算         | 根據點數大小與爆牌判定勝負    |

#### 回合流程

```
┌────────────────────────────────────────────────────────────┐
│  發牌階段 → 機會卡階段 → 玩家輪流行動 → 回合結算 → 確認戰績 │
└────────────────────────────────────────────────────────────┘
```

1. **發牌階段**：莊家按「開始」，每位非旁觀者玩家發 2 張牌
2. **機會卡階段**：按順序輪流使用或跳過功能牌
3. **玩家行動階段**：從莊家下一位開始輪流 HIT / STAND
   - **莊家開牌時機**：輪到莊家行動時，莊家手牌自動揭示給所有玩家
4. **回合結算**：所有玩家完成行動後，計算勝負並扣血
5. **確認戰績**：所有玩家確認結果後，進入下一回合（或判定勝利）

### 血量機制

| 情況                     | HP 變化          |
| ------------------------ | ---------------- |
| 閒家爆牌或輸給莊家       | -1 HP           |
| 莊家爆牌                 | -2 HP           |
| 莊家消極避戰且未獲勝     | -1 HP           |
| HP 歸零                  | 變為旁觀者（非淘汰出局） |

### 機會卡（功能牌）

- 第一**場**遊戲開始時，每位玩家發放 **3 張機會卡**
- 只能在**機會卡階段**使用（發牌後、玩家行動前）
- PVE 模式不發放機會卡

| 卡片名稱   | 效果                   | 策略用途                       |
| ---------- | ---------------------- | ------------------------------ |
| 做個交易   | 與一位玩家互換手牌     | 替換不利手牌或破壞對手優勢     |

### 旁觀者模式

**觸發條件**：
- 玩家在遊戲進行中加入房間（中途加入）
- 玩家 HP 歸零時自動轉為旁觀者

**旁觀者視角（上帝視角）**：
- 上方莊家區域：顯示莊家完整手牌
- 玩家列表：包含所有閒家手牌
- 下方手牌區：顯示當前活動玩家的手牌

**旁觀者限制**：
- 不會被發牌、無法執行遊戲操作（HIT、STAND）
- 無法確認戰績（READY）、不參與回合結算和血量變化

---

## 系統需求

### 最低需求

- **作業系統**：Windows 10+、macOS 10.14+、Linux（任意發行版）
- **Java 版本**：JDK 8 或更高版本
- **記憶體**：至少 256 MB RAM
- **磁碟空間**：約 10 MB
- **網路**：支援 TCP 連線（區域網路或網際網路）

### 推薦環境

- **作業系統**：Windows 11、macOS 13+、Ubuntu 20.04+
- **Java 版本**：JDK 17 或更高版本
- **記憶體**：512 MB RAM 或更多
- **網路**：穩定的區域網路或網際網路連線

### 開發環境（僅供開發者）

- **IDE**：IntelliJ IDEA、Eclipse 或 VS Code + Java Extension Pack
- **版本控制**：Git
- **推薦工具**：JUnit（測試）、Maven/Gradle（未來考慮）

---

## 安裝與設定

### 1. 安裝 Java

**檢查是否已安裝 Java**：
```bash
java -version
```

如果未安裝或版本過低，請訪問 [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 或 [OpenJDK](https://openjdk.org/) 下載並安裝。

**驗證安裝**：
```bash
java -version
javac -version
```

### 2. 下載專案

**方法 A：使用 Git（推薦）**
```bash
git clone https://github.com/your-repo/IM-java-final-project.git
cd IM-java-final-project
```

**方法 B：直接下載 ZIP**
1. 點擊專案頁面的「Download ZIP」
2. 解壓縮到任意目錄
3. 在終端機中進入專案目錄

### 3. 編譯專案

**編譯伺服器**：
```bash
cd BlackjackServer
javac -encoding UTF-8 -d out -sourcepath src src/Main.java src/server/*.java src/game/*.java src/model/*.java src/protocol/*.java src/command/*.java
```

**編譯客戶端**：
```bash
cd BlackjackClient
javac -encoding UTF-8 -d out -sourcepath src src/Main.java src/client/*.java src/handler/*.java src/ui/*.java src/protocol/*.java src/command/*.java
```

**編譯成功標誌**：
- 沒有錯誤訊息
- `out/` 目錄下生成 `.class` 檔案

---

## 快速開始

### Step 1：啟動伺服器

```bash
cd BlackjackServer
java -cp out Main
```

**成功啟動時會顯示**：
```
伺服器啟動於 Port 12345
等待客戶端連線...
```

**（可選）自訂 Port**：
```bash
java -cp out Main 8888  # 使用 Port 8888
```

### Step 2：啟動客戶端

開啟**新的終端視窗**：
```bash
cd BlackjackClient
java -cp out Main
```

客戶端視窗將會彈出。

### Step 3：連線與登入

1. **輸入伺服器 IP**：
   - 本機測試：`127.0.0.1`
   - 區域網路：輸入伺服器的區網 IP（如 `192.168.1.100`）
   
2. **輸入暱稱**：長度 2-20 字元

3. **點擊「連線」**

### Step 4：選擇遊戲模式

**PVE 單人練習**：
- 點擊「單人練習」按鈕
- 立即開始與電腦對戰

**PVP 多人對戰**：
- **創建房間**：點擊「創建房間」，您將成為莊家
- **加入房間**：輸入房間 ID（由創建者告知），點擊「加入房間」

### Step 5：開始遊戲

- **PVE 模式**：系統自動開始
- **PVP 模式**：莊家點擊「開始遊戲」按鈕

---

## 遊戲指南

### PVE 模式操作

1. 系統自動發牌（您與電腦各 2 張）
2. 輪到您時，選擇：
   - **要牌（HIT）**：再抽一張牌
   - **停牌（STAND）**：結束本回合
3. 查看結果，點擊「準備」進入下一回合

### PVP 模式操作

#### 作為莊家

1. 等待其他玩家加入（至少 2 人）
2. 點擊「開始遊戲」
3. **機會卡階段**：輪到您時，可選擇使用機會牌或跳過
4. **行動階段**：選擇 HIT 或 STAND
5. 回合結束後，確認戰績

#### 作為閒家

1. 加入房間後，等待莊家開始
2. **機會卡階段**：輪到您時，可選擇使用機會牌或跳過
3. **行動階段**：輪到您時，選擇 HIT 或 STAND
4. 回合結束後，確認戰績

### 聊天功能

在遊戲視窗下方的聊天輸入框輸入訊息，按 Enter 發送。

---

## 專案結構

```
IM-java-final-project/
├── BlackjackServer/          # 伺服器程式
│   ├── src/
│   │   ├── Main.java         # 進入點
│   │   ├── server/           # 網路層
│   │   │   ├── BlackjackServer.java  # Server 主類別
│   │   │   └── ClientHandler.java    # 客戶端處理器
│   │   ├── game/             # 遊戲邏輯
│   │   │   ├── GameRoom.java         # 房間管理
│   │   │   └── PVEGame.java          # PVE 遊戲邏輯
│   │   ├── model/            # 資料模型
│   │   │   ├── Card.java             # 卡牌
│   │   │   ├── Deck.java             # 牌組
│   │   │   ├── Hand.java             # 手牌
│   │   │   ├── FunctionCard.java     # 功能牌
│   │   │   └── FunctionCardType.java # 功能牌類型
│   │   ├── protocol/         # 通訊協定
│   │   │   └── Protocol.java         # 協定常數
│   │   └── command/          # 命令模式 (Command Pattern)
│   │       ├── Command.java          # 命令介面
│   │       ├── CommandContext.java   # 命令上下文
│   │       ├── CommandRegistry.java  # 命令註冊表
│   │       └── [各種命令類別...]
│   └── out/                  # 編譯輸出
│
├── BlackjackClient/          # 客戶端程式
│   ├── src/
│   │   ├── Main.java         # 進入點
│   │   ├── client/           # 主視窗與網路
│   │   │   ├── BlackjackClient.java  # 主視窗
│   │   │   ├── NetworkClient.java    # 網路客戶端
│   │   │   └── UserConfig.java       # 本地配置管理
│   │   ├── handler/          # 訊息處理
│   │   │   └── MessageHandler.java   # 訊息處理入口
│   │   ├── ui/               # UI 元件
│   │   │   ├── LoginPanel.java       # 登入面板
│   │   │   ├── LobbyPanel.java       # 大廳面板
│   │   │   ├── GamePanel.java        # 遊戲面板
│   │   │   ├── CardPanel.java        # 卡牌面板
│   │   │   └── FunctionCardPanel.java # 功能牌面板
│   │   ├── protocol/         # 通訊協定
│   │   │   └── Protocol.java         # 協定常數
│   │   └── command/          # 命令模式 (Command Pattern)
│   │       ├── ServerMessageHandler.java     # 訊息處理器介面
│   │       ├── MessageContext.java           # 訊息上下文
│   │       ├── MessageHandlerRegistry.java   # 處理器註冊表
│   │       └── [各種處理器類別...]
│   ├── out/                  # 編譯輸出
│   └── user_config.txt       # 本地配置檔（自動生成）
│
├── README.md                 # 本文件
├── AGENTS.md                 # AI Agent 開發指南
├── VERSION                   # 版本號
└── .gitignore                # Git 忽略清單
```

---

## 技術細節

### 核心技術

- **語言**：Java（支援 JDK 8+，推薦 JDK 17+）
- **網路**：Java Socket（TCP 協定）
- **UI 框架**：Java Swing
- **預設 Port**：12345（可自訂）
- **設計模式**：Command Pattern（命令模式）

### 網路架構

```
┌─────────────┐         TCP Socket          ┌─────────────┐
│   Client A  │ ←─────────────────────────→ │             │
├─────────────┤                              │   Server    │
│   Client B  │ ←─────────────────────────→ │  (Port      │
├─────────────┤                              │   12345)    │
│   Client C  │ ←─────────────────────────→ │             │
└─────────────┘                              └─────────────┘
```

- **通訊協定**：文字型協定，使用 `|` 作為分隔符號
- **並發處理**：每個客戶端連線由獨立執行緒處理
- **編碼**：UTF-8

---

## 設計模式

### Command Pattern（命令模式）

專案採用命令模式處理客戶端與伺服器之間的通訊，將每個操作封裝為獨立的命令物件。

#### 伺服器端架構

```
Client Request → ClientHandler → CommandRegistry → Command.execute() → Business Logic
```

**核心類別**：
- `Command`：定義 `execute(CommandContext context)` 方法
- `CommandContext`：封裝 `ClientHandler`、命令參數、房間列表
- `CommandRegistry`：透過 `getCommand(action)` 取得對應命令
- 各命令類別：`LoginCommand`、`HitCommand`、`StandCommand` 等

#### 客戶端架構

```
Server Message → MessageHandler → MessageHandlerRegistry → Handler.handle() → UI Update
```

**核心類別**：
- `ServerMessageHandler`：定義 `handle(MessageContext context)` 方法
- `MessageContext`：封裝 `BlackjackClient`、訊息參數
- `MessageHandlerRegistry`：透過 `getHandler(cmd)` 取得對應處理器
- 各處理器類別：`LoginOkHandler`、`GameOverHandler` 等

#### 優點

✅ **單一職責**：每個命令/處理器只負責一種操作  
✅ **開放封閉**：新增命令無需修改現有程式碼  
✅ **可測試性**：命令可獨立進行單元測試  
✅ **可維護性**：避免大型 switch-case 區塊  

---

## 疑難排解

### 編譯問題

**問題：編碼錯誤**
```
error: unmappable character for encoding MS950
```
**解決方法**：
```bash
# 加入 -encoding UTF-8 參數
javac -encoding UTF-8 -d out -sourcepath src src/Main.java ...
```

**問題：找不到類別**
```
Error: Could not find or load main class Main
```
**解決方法**：
```bash
# 確認編譯輸出目錄為 out，執行時使用 -cp out
java -cp out Main
```

### 連線問題

**問題：客戶端無法連線到伺服器**

**檢查清單**：
1. 確認伺服器已啟動並顯示「伺服器啟動於 Port 12345」
2. 確認防火牆未阻擋 Port 12345
3. 本機測試使用 `127.0.0.1`，區域網路測試使用伺服器的區網 IP
4. 執行 `netstat -ano | findstr :12345`（Windows）或 `lsof -i :12345`（macOS/Linux）確認 Port 已被監聽

**問題：連線後立即斷開**

**可能原因**：
- 客戶端與伺服器的 Protocol 版本不一致
- 未捕捉的例外導致線程終止

**解決方法**：
- 查看伺服器與客戶端的錯誤輸出
- 確保伺服器和客戶端版本一致

### 遊戲邏輯問題

**問題：中文字體無法顯示（顯示為方塊）**

**解決方法**：
在 `LoginPanel.java` 等 UI 元件中，明確設定支援中文的字體：
```java
Font chineseFont = new Font("Microsoft JhengHei", Font.PLAIN, 14);
component.setFont(chineseFont);
```

**問題：機會牌按鈕無法點擊**

**檢查**：
- 當前遊戲階段是否為「機會卡階段」
- 是否輪到該玩家使用機會卡
- 收到 `FUNC_CARD_PHASE|YOUR` 訊息後才能啟用按鈕

---

## 常見問題

**Q1：可以在網際網路上與朋友對戰嗎？**  
A：可以，但需要伺服器有公網 IP 或使用 Port 轉發（NAT）。建議使用 VPN 或 Hamachi 等工具建立虛擬區域網路。

**Q2：支援多少人同時遊玩？**  
A：每個房間最多支援 5 名玩家。伺服器可同時支援多個房間。

**Q3：遊戲進度會保存嗎？**  
A：目前不支援遊戲進度持久化。斷線重連後，玩家會回到大廳。

**Q4：如何修改預設 Port？**  
A：啟動伺服器時加上 Port 參數：
```bash
java -cp out Main 8888  # 使用 Port 8888
```

**Q5：可以自訂功能牌嗎？**  
A：可以。請參考 `AGENTS.md` 中的「擴展建議」章節。

**Q6：為什麼莊家爆牌扣 2 HP？**  
A：莊家擁有後手優勢，因此承擔更高的風險與責任。

---

## 開發團隊

本專案是一個教學用途的 Java Socket 多人遊戲開發實踐。

**開發者**：[您的團隊名稱]  
**課程**：[課程名稱]  
**學期**：[學期]  

---

## 貢獻指南

我們歡迎任何形式的貢獻！

### 如何貢獻

1. **Fork 專案**到您的 GitHub 帳號
2. **建立功能分支**：
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **提交變更**：
   ```bash
   git commit -m "新增：超棒的功能"
   ```
4. **推送到分支**：
   ```bash
   git push origin feature/amazing-feature
   ```
5. **建立 Pull Request**

### 開發規範

- **程式碼風格**：遵循 Java 慣例（PascalCase 類別名、camelCase 方法名）
- **註解**：使用中文註解，清晰說明邏輯
- **測試**：新增功能時，確保現有功能不受影響
- **文件**：更新 `AGENTS.md`（如有架構變更）和 `README.md`（如有用戶可見變更）

---

## 授權條款

本專案採用 **MIT License** 授權。

```
MIT License

Copyright (c) 2025 [Your Name/Team]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 致謝

感謝所有參與開發與測試的同學，以及提供指導的老師。

**特別感謝**：
- Java Socket 教學資源
- Swing GUI 設計指南
- 所有測試玩家的寶貴建議

---

## 更新日誌

### v0.0.1（2025-12-28）

初始開發版本。

✨ **核心功能**：
- 完整的 PVE 與 PVP 遊戲模式
- 機會卡系統（做個交易）
- 旁觀者模式（上帝視角）
- 即時聊天功能
- 本地配置自動保存

🏗️ **架構**：
- 採用命令模式重構通訊層
- 優化並發安全性

---

**最後更新**：2025-12-28
