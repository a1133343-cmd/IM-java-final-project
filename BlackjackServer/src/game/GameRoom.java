import java.util.ArrayList;
import java.util.List;

/**
 * 遊戲房間類別 - 管理遊戲流程
 */
public class GameRoom {
    private final String roomId;
    private final List<PlayerInfo> players = new ArrayList<>();

    private Deck deck;
    private int dealerIndex = 0;
    private int turnIndex = 0;
    private boolean gameInProgress = false;
    private boolean functionCardPhase = false; // 是否在機會卡階段
    private int functionCardTurnIndex = 0; // 機會卡階段當前輪到的玩家
    private boolean pendingVictory = false; // 是否有待發送的勝利訊息
    private String pendingWinnerName = null; // 待發送勝利訊息的贏家名稱（null 表示平局）

    public GameRoom(ClientHandler creator, String roomId) {
        this.roomId = roomId;
        addPlayer(creator);
    }

    public String getRoomId() {
        return roomId;
    }

    public boolean isFull() {
        return players.size() >= 5;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public boolean isGameInProgress() {
        return gameInProgress;
    }

    // ==================== 玩家管理 ====================

    /**
     * 檢查房間內是否已有相同名字的玩家
     */
    public boolean hasPlayer(String name) {
        for (PlayerInfo p : players) {
            if (p.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void addPlayer(ClientHandler handler) {
        if (!isFull()) {
            PlayerInfo newPlayer = new PlayerInfo(handler);

            // 如果遊戲正在進行中，設定為旁觀者（需等這場結束才恢復）
            if (gameInProgress) {
                newPlayer.setSpectator(true);
                newPlayer.setJoinedMidGame(true); // 標記為中途加入，需等場結束才恢復
                players.add(newPlayer);
                broadcast(Protocol.MSG + Protocol.DELIMITER + "玩家 " + handler.getName() + " 以旁觀者身份加入 (" + players.size()
                        + "/5)");
                handler.send(Protocol.MSG + Protocol.DELIMITER + "遊戲進行中，你將以旁觀者身份觀看，這場結束後才能參與");
                // 發送當前遊戲狀態給旁觀者
                sendStateToAll();
            } else {
                // 遊戲未開始，正常加入
                if (players.isEmpty()) {
                    newPlayer.setDealer(true);
                    dealerIndex = 0;
                }
                players.add(newPlayer);
                broadcast(Protocol.MSG + Protocol.DELIMITER + "玩家 " + handler.getName() + " 加入 (" + players.size()
                        + "/5)");
                // 給新加入的玩家發功能牌（如果房間已經有玩家有功能牌，表示不是第一局）
                // 非第一局加入的玩家需要確認戰績才能開始
                if (roomId != null && players.size() > 1 && !players.get(0).getFunctionCards().isEmpty()) {
                    newPlayer.setReady(false); // 非第一局，需要確認才能開始
                    dealFunctionCardsToPlayer(newPlayer);
                    sendFunctionCardsTo(newPlayer);
                    handler.send(Protocol.MSG + Protocol.DELIMITER + "請確認戰績以準備下一局");
                }
            }

            broadcast(Protocol.HP_UPDATE + Protocol.DELIMITER + getHpString());
            // 在玩家加入時發送功能牌狀態
            sendFunctionCardsToAll();
        }
    }

    public void removePlayer(ClientHandler handler) {
        // 1. 找到被移除玩家的索引
        int removeIndex = -1;
        PlayerInfo removedPlayer = null;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getHandler() == handler) {
                removeIndex = i;
                removedPlayer = players.get(i);
                break;
            }
        }

        if (removeIndex == -1) {
            return; // 玩家不存在
        }

        // 記錄移除前的狀態
        boolean wasSpectator = removedPlayer.isSpectator();
        boolean wasDealer = removedPlayer.isDealer();
        boolean wasCurrentTurn = (gameInProgress && !functionCardPhase && removeIndex == turnIndex);
        boolean wasFunctionCardTurn = (functionCardPhase && removeIndex == functionCardTurnIndex);

        // 2. 移除玩家
        players.remove(removeIndex);
        broadcast(Protocol.MSG + Protocol.DELIMITER + "玩家 " + handler.getName() + " 離開");

        // 如果房間空了，重置狀態
        if (players.isEmpty()) {
            gameInProgress = false;
            functionCardPhase = false;
            return;
        }

        // 3. 調整 dealerIndex
        if (removeIndex < dealerIndex) {
            dealerIndex--;
        } else if (removeIndex == dealerIndex) {
            // 莊家離開，dealerIndex 保持但可能需要調整範圍
            if (dealerIndex >= players.size()) {
                dealerIndex = 0;
            }
        }
        if (dealerIndex >= players.size()) {
            dealerIndex = 0;
        }

        // 4. 調整 turnIndex（遊戲行動階段）
        if (gameInProgress && !functionCardPhase) {
            if (removeIndex < turnIndex) {
                turnIndex--;
            } else if (removeIndex == turnIndex) {
                if (turnIndex >= players.size()) {
                    turnIndex = 0;
                }
            }
            if (turnIndex >= players.size()) {
                turnIndex = 0;
            }
        }

        // 5. 調整 functionCardTurnIndex（機會卡階段）
        if (functionCardPhase) {
            if (removeIndex < functionCardTurnIndex) {
                functionCardTurnIndex--;
            } else if (removeIndex == functionCardTurnIndex) {
                if (functionCardTurnIndex >= players.size()) {
                    functionCardTurnIndex = 0;
                }
            }
            if (functionCardTurnIndex >= players.size()) {
                functionCardTurnIndex = 0;
            }
        }

        // 6. 重新設定莊家標記
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setDealer(i == dealerIndex);
        }

        broadcast(Protocol.HP_UPDATE + Protocol.DELIMITER + getHpString());

        // 7. 處理遊戲中的狀態變化
        if (gameInProgress) {
            // 計算還有幾個非旁觀者
            int activeCount = 0;
            for (PlayerInfo p : players) {
                if (!p.isSpectator()) {
                    activeCount++;
                }
            }

            // === 莊家離開：取消本回合，不計分 ===
            if (wasDealer && !wasSpectator) {
                cancelRoundDueToDealerLeave();
                return;
            }

            // === 只剩旁觀者：靜默結束這場遊戲 ===
            if (activeCount == 0) {
                gameInProgress = false;
                functionCardPhase = false;
                // 重置所有玩家狀態（不顯示勝利通知）
                resetAllPlayersForNewGame();
                broadcast(Protocol.MSG + Protocol.DELIMITER + "所有活躍玩家已離開，遊戲結束");
                return;
            }

            // === 只剩一名活躍玩家：判定勝利 ===
            if (activeCount == 1 && roomId != null) {
                gameInProgress = false;
                functionCardPhase = false;
                handleSinglePlayerVictory();
                return;
            }

            // === 還有多人，繼續遊戲 ===
            if (wasFunctionCardTurn && !wasSpectator) {
                advanceFunctionCardPhaseAfterLeave();
            } else if (wasCurrentTurn && !wasSpectator) {
                checkAndNotifyTurn();
            } else {
                sendStateToAll();
            }
        }
    }

    /**
     * 莊家離開導致回合取消（不計分）
     */
    private void cancelRoundDueToDealerLeave() {
        gameInProgress = false;
        functionCardPhase = false;

        // 廣播回合取消通知
        broadcast(Protocol.ROUND_CANCEL + Protocol.DELIMITER + "莊家離開，本回合取消");
        broadcast(Protocol.MSG + Protocol.DELIMITER + "⚠️ 莊家離開，本回合取消，不計分");

        // 重置所有非旁觀者的手牌狀態（回合取消不扣血）
        for (PlayerInfo p : players) {
            if (!p.isSpectator()) {
                p.resetHand();
                p.setReady(true); // 準備下一回合
            }
        }

        // 設定新莊家（第一個非旁觀者）
        for (int i = 0; i < players.size(); i++) {
            if (!players.get(i).isSpectator()) {
                dealerIndex = i;
                break;
            }
        }
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setDealer(i == dealerIndex);
        }

        broadcast(Protocol.HP_UPDATE + Protocol.DELIMITER + getHpString());
        broadcast(Protocol.MSG + Protocol.DELIMITER + "新莊家是 " + players.get(dealerIndex).getName() + "，可開始下一局");
    }

    /**
     * 重置所有玩家狀態準備新遊戲（用於無勝利提示的場結束情況）
     */
    private void resetAllPlayersForNewGame() {
        for (PlayerInfo p : players) {
            p.setSpectator(false);
            p.setJoinedMidGame(false);
            p.setHp(15);
            p.setReady(true);
            p.clearFunctionCards();
        }

        dealerIndex = 0;
        if (!players.isEmpty()) {
            players.get(0).setDealer(true);
        }

        broadcast(Protocol.HP_UPDATE + Protocol.DELIMITER + getHpString());
    }

    /**
     * 處理只剩一名玩家時的勝利邏輯
     */
    private void handleSinglePlayerVictory() {
        PlayerInfo winner = null;
        for (PlayerInfo p : players) {
            if (!p.isSpectator()) {
                winner = p;
                break;
            }
        }

        if (winner != null) {
            broadcast(Protocol.GAME_WIN + Protocol.DELIMITER + winner.getName());
            broadcast(Protocol.MSG + Protocol.DELIMITER + "🎉 遊戲結束！" + winner.getName() + " 獲得勝利！");

            // 重置所有玩家狀態，準備新遊戲（場結束）
            for (PlayerInfo p : players) {
                p.setSpectator(false);
                p.setJoinedMidGame(false); // 場結束，重置中途加入標記
                p.setHp(15);
                p.setReady(true);
                p.clearFunctionCards();
            }

            dealerIndex = 0;
            if (!players.isEmpty()) {
                players.get(0).setDealer(true);
            }
            broadcast(Protocol.HP_UPDATE + Protocol.DELIMITER + getHpString());
            broadcast(Protocol.MSG + Protocol.DELIMITER + "所有玩家 HP 已重置，等待莊家開始新一局...");
        }
    }

    /**
     * 玩家離開後推進機會卡階段
     */
    private void advanceFunctionCardPhaseAfterLeave() {
        // 檢查是否所有人都確認過了
        if (functionCardTurnIndex == dealerIndex) {
            // 可能需要檢查莊家是否也確認過了
            PlayerInfo dealer = players.get(dealerIndex);
            if (dealer.hasConfirmedFunctionCardPhase() || dealer.isSpectator()) {
                endFunctionCardPhase();
                return;
            }
        }

        // 找到下一個未確認的玩家
        int loopCount = 0;
        while (loopCount < players.size()) {
            PlayerInfo current = players.get(functionCardTurnIndex);
            if (!current.hasConfirmedFunctionCardPhase() && !current.isSpectator()) {
                notifyFunctionCardPhaseTurn();
                return;
            }
            functionCardTurnIndex = (functionCardTurnIndex + 1) % players.size();
            loopCount++;

            // 繞回莊家表示所有人都確認過了
            if (functionCardTurnIndex == dealerIndex && loopCount > 0) {
                endFunctionCardPhase();
                return;
            }
        }

        // 迴圈結束還沒 return，結束機會卡階段
        endFunctionCardPhase();
    }

    // ==================== 遊戲控制 ====================

    public void handlePlayerReady(ClientHandler handler) {
        boolean allReady = true;
        boolean isSpectator = false;

        for (PlayerInfo p : players) {
            if (p.getHandler() == handler) {
                if (p.isSpectator()) {
                    handler.send(Protocol.MSG + Protocol.DELIMITER + "旁觀者無需確認戰績");
                    isSpectator = true;
                    break;
                } else {
                    p.setReady(true);
                    broadcast(Protocol.MSG + Protocol.DELIMITER + p.getName() + " 已確認戰績");
                }
            }
        }

        if (isSpectator) {
            return;
        }

        for (PlayerInfo p : players) {
            // 旁觀者不計入 ready 檢查
            if (!p.isSpectator() && !p.isReady()) {
                allReady = false;
            }
        }

        if (allReady && players.size() > 1) {
            // 檢查是否有待發送的勝利訊息
            if (pendingVictory) {
                handlePendingVictory();
            } else {
                broadcast(Protocol.MSG + Protocol.DELIMITER + "所有玩家已確認完畢，莊家可開始下一局");
            }
        }
    }

    public void tryStartGame(ClientHandler requestor) {
        if (players.isEmpty())
            return;

        // PVE 模式
        if (roomId == null) {
            startGame();
            return;
        }

        if (dealerIndex >= players.size()) {
            dealerIndex = 0;
        }

        if (players.get(dealerIndex).getHandler() == requestor) {
            // 檢查玩家人數（至少需要 2 人才能開始 PVP）
            int activePlayerCount = 0;
            for (PlayerInfo p : players) {
                if (!p.isSpectator()) {
                    activePlayerCount++;
                }
            }
            if (activePlayerCount < 2) {
                requestor.send(Protocol.ERROR + Protocol.DELIMITER + "至少需要 2 位玩家才能開始遊戲");
                return;
            }

            List<String> notReadyList = new ArrayList<>();
            for (PlayerInfo p : players) {
                // 旁觀者不需要確認 ready
                if (!p.isSpectator() && !p.isReady()) {
                    notReadyList.add(p.getName());
                }
            }

            if (!notReadyList.isEmpty()) {
                requestor.send(
                        Protocol.ERROR + Protocol.DELIMITER + "無法開始！以下玩家尚未確認戰績: " + String.join(", ", notReadyList));
            } else {
                startGame();
            }
        } else {
            requestor.send(
                    Protocol.ERROR + Protocol.DELIMITER + "只有當前莊家 (" + players.get(dealerIndex).getName() + ") 可以開始");
        }
    }

    public void startGame() {
        if (roomId != null && players.size() < 2) {
            broadcast(Protocol.MSG + Protocol.DELIMITER + "至少需要 2 位玩家才能開始");
            return;
        }

        gameInProgress = true;
        deck = new Deck();
        deck.shuffle();

        // PVE 模式
        if (roomId == null) {
            startPVE();
            return;
        }

        if (dealerIndex >= players.size()) {
            dealerIndex = 0;
        }

        for (int i = 0; i < players.size(); i++) {
            PlayerInfo p = players.get(i);

            // 旁觀者不發牌
            if (p.isSpectator()) {
                continue;
            }

            p.resetHand();
            p.setDealer(i == dealerIndex);
            p.getHand().add(deck.draw());
            p.getHand().add(deck.draw());
            p.resetFunctionCardPhase(); // 重置機會卡階段狀態
        }

        // 發放功能牌（如果是第一局，玩家還沒有功能牌）
        boolean firstGame = players.stream().allMatch(p -> p.getFunctionCards().isEmpty());
        if (firstGame) {
            dealFunctionCards();
        }

        broadcast(Protocol.MSG + Protocol.DELIMITER + "=== 新局開始，莊家是 " + players.get(dealerIndex).getName() + " ===");

        sendStateToAll();
        sendFunctionCardsToAll();

        // 進入機會卡階段
        startFunctionCardPhase();
    }

    private void startPVE() {
        PlayerInfo player = players.get(0);
        player.resetHand();
        player.setDealer(false);
        player.getHand().add(deck.draw());
        player.getHand().add(deck.draw());

        PlayerInfo cpu = new PlayerInfo(null);
        cpu.setDealer(true);
        cpu.getHand().add(deck.draw());
        cpu.getHand().add(deck.draw());

        if (players.size() > 1) {
            players.remove(1);
        }
        players.add(cpu);
        turnIndex = 0;
        sendStateToAll();
        player.send(Protocol.TURN + Protocol.DELIMITER + "YOUR");
    }

    // ==================== 遊戲行動 ====================

    public void handleGameAction(ClientHandler handler, String action) {
        if (!gameInProgress)
            return;

        // 檢查玩家是否為旁觀者
        PlayerInfo actionPlayer = null;
        for (PlayerInfo p : players) {
            if (p.getHandler() == handler) {
                actionPlayer = p;
                break;
            }
        }

        if (actionPlayer != null && actionPlayer.isSpectator()) {
            handler.send(Protocol.MSG + Protocol.DELIMITER + "旁觀者無法進行遊戲操作");
            return;
        }

        PlayerInfo currentP = players.get(turnIndex);

        // PVE 模式
        if (roomId == null) {
            handlePVE(handler, action);
            return;
        }

        if (currentP.getHandler() != handler) {
            handler.send(Protocol.MSG + Protocol.DELIMITER + "還沒輪到你！");
            return;
        }

        if (action.equals(Protocol.HIT)) {
            currentP.getHand().add(deck.draw());
            if (currentP.getHand().isBust()) {
                handler.send(Protocol.MSG + Protocol.DELIMITER + "爆牌了！");
                currentP.setHasStayed(true);
                nextTurn();
            } else {
                sendStateToAll();
                handler.send(Protocol.TURN + Protocol.DELIMITER + "YOUR");
            }
        } else if (action.equals(Protocol.STAND)) {
            currentP.setHasStayed(true);
            handler.send(Protocol.MSG + Protocol.DELIMITER + "你選擇了停牌");
            nextTurn();
        }
    }

    private void handlePVE(ClientHandler handler, String action) {
        PlayerInfo player = players.get(0);
        PlayerInfo cpu = players.get(1);

        if (action.equals(Protocol.HIT)) {
            player.getHand().add(deck.draw());
            if (player.getHand().isBust()) {
                player.send(Protocol.GAME_OVER + Protocol.DELIMITER
                        + cpu.getHand().toString(true, false) + Protocol.DELIMITER
                        + player.getHand().toString(true, false) + Protocol.DELIMITER
                        + "你爆牌了");
                gameInProgress = false;
            } else {
                sendStateToAll();
            }
        } else {
            // CPU 的邏輯：點數 < 17 繼續拿牌
            while (cpu.getHand().bestValue() < 17) {
                cpu.getHand().add(deck.draw());
            }

            int pVal = player.getHand().bestValue();
            int cVal = cpu.getHand().bestValue();
            boolean cpuBust = cpu.getHand().isBust();

            String result;
            if (cpuBust) {
                result = "電腦爆牌，你贏了";
            } else if (pVal > cVal) {
                result = "你點數較大，贏了";
            } else if (pVal < cVal) {
                result = "電腦點數較大，輸了";
            } else {
                result = "平手";
            }

            player.send(Protocol.GAME_OVER + Protocol.DELIMITER
                    + cpu.getHand().toString(true, false) + Protocol.DELIMITER
                    + player.getHand().toString(true, false) + Protocol.DELIMITER
                    + result);
            gameInProgress = false;
        }

        if (!gameInProgress) {
            player.send(Protocol.HP_UPDATE + Protocol.DELIMITER + getHpString());
        }
    }

    private void nextTurn() {
        sendStateToAll();
        PlayerInfo current = players.get(turnIndex);
        if (current.isDealer() && (current.hasStayed() || current.getHand().isBust())) {
            endRound();
            return;
        }
        turnIndex = (turnIndex + 1) % players.size();
        checkAndNotifyTurn();
    }

    private void checkAndNotifyTurn() {
        PlayerInfo current = players.get(turnIndex);
        int checkCount = 0;

        // 跳過已經停牌、爆牌或旁觀者的玩家
        while (current.hasStayed() || current.getHand().isBust() || current.isSpectator()) {
            if (current.isDealer()) {
                endRound();
                return;
            }
            turnIndex = (turnIndex + 1) % players.size();
            current = players.get(turnIndex);
            checkCount++;
            if (checkCount > players.size() + 1) {
                endRound();
                return;
            }
        }

        for (PlayerInfo p : players) {
            if (p == current) {
                p.send(Protocol.TURN + Protocol.DELIMITER + "YOUR");
                p.send(Protocol.MSG + Protocol.DELIMITER + "輪到你了！");
            } else {
                p.send(Protocol.TURN + Protocol.DELIMITER + "WAIT");
                if (p.isDealer()) {
                    p.send(Protocol.MSG + Protocol.DELIMITER + "等待閒家行動...");
                } else {
                    p.send(Protocol.MSG + Protocol.DELIMITER + "等待 " + current.getName() + " 行動...");
                }
            }
        }
    }

    // ==================== 回合結算 ====================

    private void endRound() {
        PlayerInfo dealer = players.get(dealerIndex);
        int dScore = dealer.getHand().bestValue();
        boolean dBust = dealer.getHand().isBust();

        // 判斷莊家是否拿過牌 (大於2張表示有HIT過)
        boolean dealerHit = (dealer.getHand().size() > 2);
        int dealerWinCount = 0;

        StringBuilder sb = new StringBuilder("本局結算:\\n");
        if (dBust) {
            dealer.decreaseHp(2);
            sb.append("[莊] ").append(dealer.getName()).append(" 爆牌 (-2 HP)\\n");
        } else {
            sb.append("[莊] ").append(dealer.getName()).append(" ").append(dScore).append("點\\n");
        }

        for (int i = 0; i < players.size(); i++) {
            if (i == dealerIndex)
                continue;

            PlayerInfo p = players.get(i);

            // 旁觀者不參與結算
            if (p.isSpectator()) {
                continue;
            }

            int pScore = p.getHand().bestValue();
            boolean pBust = p.getHand().isBust();

            if (pBust) {
                // 閒家爆牌：無論莊家如何都是閒家輸
                dealerWinCount++;
                p.decreaseHp(1);
                sb.append(p.getName()).append(" 爆牌 (-1 HP)\\n");
            } else if (dBust) {
                // 莊家爆牌且閒家沒爆：閒家贏
                sb.append(p.getName()).append(" 贏莊家（莊家爆牌）\\n");
            } else if (pScore > dScore) {
                // 雙方都沒爆，閒家點數大：閒家贏
                sb.append(p.getName()).append(" 贏莊家\\n");
            } else if (pScore < dScore) {
                // 雙方都沒爆，閒家點數小：閒家輸
                dealerWinCount++;
                p.decreaseHp(1);
                sb.append(p.getName()).append(" 輸莊家 (-1 HP)\\n");
            } else {
                // 雙方都沒爆，點數相同：平手
                sb.append(p.getName()).append(" 平手\\n");
            }
        }

        // 莊家消極懲罰：不拿牌且沒有贏任何人
        if (!dBust && !dealerHit && dealerWinCount == 0) {
            dealer.decreaseHp(1);
            sb.append(">>> [莊] 消極避戰且未獲勝，懲罰 (-1 HP) <<<\\n");
        }

        for (PlayerInfo p : players) {
            // 旁觀者只能看到莊家的第一張牌（蓋牌狀態），無法看到莊家開牌
            String dHand;
            if (p.isSpectator()) {
                dHand = dealer.getHand().toString(false, true); // 只顯示莊家第一張牌
            } else {
                dHand = dealer.getHand().toString(true, false); // 正常玩家可以看到全部牌
            }
            String mHand = p.getHand().toString(true, false);
            p.send(Protocol.GAME_OVER + Protocol.DELIMITER + dHand + Protocol.DELIMITER + mHand + Protocol.DELIMITER
                    + sb.toString());
        }

        for (PlayerInfo p : players) {
            p.setReady(false);
            // 只恢復因 HP 歸零的旁觀者（非中途加入者）
            // 中途加入的旁觀者需等到場結束才恢復
            if (p.isSpectator() && !p.isJoinedMidGame()) {
                p.setSpectator(false);
                p.send(Protocol.MSG + Protocol.DELIMITER + "下一局你將可以參與遊戲");
            }
        }

        // 血量歸零的玩家轉為旁觀者（不移除）
        for (PlayerInfo p : players) {
            if (p.getHp() <= 0 && !p.isSpectator()) {
                p.setSpectator(true);
                p.send(Protocol.MSG + Protocol.DELIMITER + "你的血量歸零，變成旁觀者！");
            }
        }

        // 計算非旁觀者玩家數量
        long activeCount = players.stream().filter(p -> !p.isSpectator()).count();

        gameInProgress = false;

        if (activeCount == 1) {
            // 只剩一名玩家獲勝 - 標記待發送勝利訊息
            PlayerInfo winner = players.stream().filter(p -> !p.isSpectator()).findFirst().orElse(null);
            if (winner != null) {
                pendingVictory = true;
                pendingWinnerName = winner.getName();
                broadcast(Protocol.MSG + Protocol.DELIMITER + "請所有玩家確認回合結果...");
            }
        } else if (activeCount == 0) {
            // 極端情況：所有人同時歸零（平局）- 標記待發送平局訊息
            pendingVictory = true;
            pendingWinnerName = null; // null 表示平局
            broadcast(Protocol.MSG + Protocol.DELIMITER + "請所有玩家確認回合結果...");
        } else if (activeCount > 1) {
            // 還有多名玩家存活，正常輪換莊家
            // 找到下一個非旁觀者作為莊家
            int nextDealer = (dealerIndex + 1) % players.size();
            int searchCount = 0;
            while (players.get(nextDealer).isSpectator() && searchCount < players.size()) {
                nextDealer = (nextDealer + 1) % players.size();
                searchCount++;
            }
            dealerIndex = nextDealer;

            for (int i = 0; i < players.size(); i++) {
                players.get(i).setDealer(i == dealerIndex);
            }

            broadcast(Protocol.HP_UPDATE + Protocol.DELIMITER + getHpString());
            broadcast(Protocol.MSG + Protocol.DELIMITER + "請所有玩家確認結算視窗，以進行下一局...");
        }
    }

    /**
     * 處理待發送的勝利訊息（在所有玩家確認回合結果後調用）
     */
    private void handlePendingVictory() {
        if (pendingWinnerName != null) {
            // 有贏家
            broadcast(Protocol.GAME_WIN + Protocol.DELIMITER + pendingWinnerName);
            broadcast(Protocol.MSG + Protocol.DELIMITER + "🎉 遊戲結束！" + pendingWinnerName + " 獲得勝利！");
        } else {
            // 平局
            broadcast(Protocol.MSG + Protocol.DELIMITER + "所有玩家同時被淘汰，平局！HP 已重置。");
        }

        // 重置所有玩家狀態，準備新遊戲（場結束）
        for (PlayerInfo p : players) {
            p.setSpectator(false);
            p.setJoinedMidGame(false); // 場結束，重置中途加入標記
            p.setHp(15);
            p.setReady(true);
            p.clearFunctionCards();
        }

        dealerIndex = 0;
        if (!players.isEmpty()) {
            players.get(0).setDealer(true);
        }

        broadcast(Protocol.HP_UPDATE + Protocol.DELIMITER + getHpString());
        broadcast(Protocol.MSG + Protocol.DELIMITER + "所有玩家 HP 已重置，等待莊家開始新一局...");

        // 清除待發送標記
        pendingVictory = false;
        pendingWinnerName = null;
    }

    // ==================== 廣播與工具 ====================

    public void broadcastChat(String sender, String message) {
        broadcast(Protocol.CHAT + Protocol.DELIMITER + sender + ": " + message);
    }

    private void broadcast(String message) {
        for (PlayerInfo p : players) {
            p.send(message);
        }
    }

    private String getHpString() {
        StringBuilder sb = new StringBuilder();
        for (PlayerInfo p : players) {
            if (p.getHandler() != null) {
                sb.append(p.getName())
                        .append(p.isDealer() ? "(莊)" : "")
                        .append(p.isSpectator() ? "(旁觀)" : "")
                        .append(":")
                        .append(p.getHp())
                        .append("HP,");
            }
        }
        return sb.toString();
    }

    private void sendStateToAll() {
        // PVE 模式
        if (roomId == null) {
            PlayerInfo player = players.get(0);
            PlayerInfo cpu = players.get(1);
            player.send(Protocol.STATE + Protocol.DELIMITER + "PLAYING" + Protocol.DELIMITER
                    + cpu.getHand().toString(false, true) + Protocol.DELIMITER
                    + player.getHand().toString(true, false) + Protocol.DELIMITER
                    + "CPU:??,YOU:" + player.getHp());
            return;
        }

        PlayerInfo dealer = players.get(dealerIndex);

        // 一般玩家列表
        StringBuilder normalListSb = new StringBuilder();
        for (PlayerInfo p : players) {
            normalListSb.append(p.getName())
                    .append(p.isDealer() ? "(莊)" : "")
                    .append(":").append(p.getHp()).append("HP,");
        }
        String normalListStr = normalListSb.toString();

        // 莊家看到的列表 (含閒家手牌)
        StringBuilder dealerListSb = new StringBuilder();
        for (PlayerInfo p : players) {
            dealerListSb.append(p.getName())
                    .append(p.isDealer() ? "(莊)" : "")
                    .append(":").append(p.getHp()).append("HP");

            if (!p.isDealer()) {
                dealerListSb.append(p.getHand().formatForDisplay());
            }
            dealerListSb.append(",");
        }
        String dealerListStr = dealerListSb.toString();

        for (PlayerInfo p : players) {
            // 旁觀者特殊處理：只能看到莊家的第一張牌，自己手牌區為空
            if (p.isSpectator()) {
                String dHand = dealer.getHand().toString(false, true); // 只顯示莊家第一張牌
                String mHand = ""; // 旁觀者沒有手牌
                String spectatorList = normalListStr + "(旁觀)";
                p.send(Protocol.STATE + Protocol.DELIMITER + "PLAYING" + Protocol.DELIMITER
                        + dHand + Protocol.DELIMITER + mHand + Protocol.DELIMITER + spectatorList);
            } else {
                // 正常玩家
                String dHand = (p == dealer)
                        ? dealer.getHand().toString(true, false)
                        : dealer.getHand().toString(false, true);
                String mHand = p.getHand().toString(true, false);
                String listData = (p == dealer) ? dealerListStr : normalListStr;

                p.send(Protocol.STATE + Protocol.DELIMITER + "PLAYING" + Protocol.DELIMITER
                        + dHand + Protocol.DELIMITER + mHand + Protocol.DELIMITER + listData);
            }
        }
    }

    // ==================== 功能牌管理 ====================

    /**
     * 給所有玩家發放功能牌（第一局開始時）
     */
    private void dealFunctionCards() {
        // PVE 模式不發功能牌
        if (roomId == null)
            return;

        for (PlayerInfo p : players) {
            if (p.isSpectator())
                continue;
            dealFunctionCardsToPlayer(p);
        }
        sendFunctionCardsToAll();
        broadcast(Protocol.MSG + Protocol.DELIMITER + "已發放 3 張機會卡，可在回合開始前使用");
    }

    /**
     * 給單一玩家發功能牌
     */
    private void dealFunctionCardsToPlayer(PlayerInfo player) {
        // 發 3 張功能牌
        for (int i = 0; i < 3; i++) {
            // 目前只有一種功能牌，未來可以隨機或從牌堆抽
            player.addFunctionCard(new FunctionCard(FunctionCardType.MAKE_A_DEAL));
        }
    }

    /**
     * 發送功能牌狀態給所有玩家
     */
    private void sendFunctionCardsToAll() {
        for (PlayerInfo p : players) {
            sendFunctionCardsTo(p);
        }
    }

    /**
     * 發送功能牌狀態給指定玩家
     */
    private void sendFunctionCardsTo(PlayerInfo player) {
        String cardsData = player.getFunctionCardsProtocol();
        player.send(Protocol.FUNCTION_CARDS + Protocol.DELIMITER + cardsData);
    }

    /**
     * 處理功能牌使用請求
     */
    public void handleUseFunctionCard(ClientHandler handler, int cardId, String targetUid) {
        // 必須在機會卡階段
        if (!functionCardPhase) {
            handler.send(Protocol.ERROR + Protocol.DELIMITER + "目前不是機會卡使用階段");
            return;
        }

        // 找到使用者
        PlayerInfo user = null;
        int userIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getHandler() == handler) {
                user = players.get(i);
                userIndex = i;
                break;
            }
        }

        if (user == null || user.isSpectator()) {
            handler.send(Protocol.ERROR + Protocol.DELIMITER + "無法使用機會卡");
            return;
        }

        // 檢查是否輪到此玩家
        if (userIndex != functionCardTurnIndex) {
            handler.send(Protocol.ERROR + Protocol.DELIMITER + "還沒輪到你使用機會卡");
            return;
        }

        // 檢查本輪是否已使用
        if (user.hasUsedFunctionCardThisRound()) {
            handler.send(Protocol.ERROR + Protocol.DELIMITER + "本輪已使用過機會卡");
            return;
        }

        // 移除功能牌
        FunctionCard card = user.removeFunctionCard(cardId);
        if (card == null) {
            handler.send(Protocol.ERROR + Protocol.DELIMITER + "找不到指定的機會卡");
            return;
        }

        // 標記已使用
        user.setUsedFunctionCardThisRound(true);
        user.setConfirmedFunctionCardPhase(true);

        // 根據功能牌類型執行效果
        switch (card.getType()) {
            case MAKE_A_DEAL:
                executeMakeADeal(user, targetUid);
                break;
        }

        // 更新功能牌狀態
        sendFunctionCardsToAll();
        sendStateToAll();

        // 輪到下一位玩家
        advanceFunctionCardPhase();
    }

    /**
     * 處理跳過機會卡使用
     */
    public void handleSkipFunctionCard(ClientHandler handler) {
        if (!functionCardPhase) {
            handler.send(Protocol.ERROR + Protocol.DELIMITER + "目前不是機會卡使用階段");
            return;
        }

        PlayerInfo user = null;
        int userIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getHandler() == handler) {
                user = players.get(i);
                userIndex = i;
                break;
            }
        }

        if (user == null || user.isSpectator()) {
            return;
        }

        if (userIndex != functionCardTurnIndex) {
            handler.send(Protocol.ERROR + Protocol.DELIMITER + "還沒輪到你");
            return;
        }

        user.setConfirmedFunctionCardPhase(true);
        broadcast(Protocol.MSG + Protocol.DELIMITER + user.getName() + " 不使用機會卡");

        advanceFunctionCardPhase();
    }

    /**
     * 執行「做個交易」效果：與目標玩家互換手牌
     */
    private void executeMakeADeal(PlayerInfo user, String targetUid) {
        // 找到目標玩家（支援 UID 或名稱匹配）
        PlayerInfo target = null;
        for (PlayerInfo p : players) {
            if (p != user && (p.getUid().equals(targetUid) || p.getName().equals(targetUid))) {
                target = p;
                break;
            }
        }

        if (target == null || target.isSpectator()) {
            user.send(Protocol.ERROR + Protocol.DELIMITER + "無效的目標玩家");
            return;
        }

        // 交換手牌
        Hand tempHand = user.getHand();
        user.setHand(target.getHand());
        target.setHand(tempHand);

        // 廣播通知
        broadcast(Protocol.FUNCTION_CARD_USED + Protocol.DELIMITER
                + user.getName() + Protocol.DELIMITER
                + "做個交易" + Protocol.DELIMITER
                + target.getName());
        broadcast(Protocol.MSG + Protocol.DELIMITER
                + user.getName() + " 使用了「做個交易」與 " + target.getName() + " 互換手牌！");
    }

    // ==================== 機會卡階段控制 ====================

    /**
     * 開始機會卡階段
     */
    private void startFunctionCardPhase() {
        // PVE 模式跳過機會卡階段
        if (roomId == null) {
            turnIndex = 0;
            checkAndNotifyTurn();
            return;
        }

        functionCardPhase = true;
        functionCardTurnIndex = dealerIndex; // 從莊家開始

        broadcast(Protocol.MSG + Protocol.DELIMITER + "=== 機會卡階段開始，可選擇使用一張機會卡或點「不使用」 ===");
        notifyFunctionCardPhaseTurn();
    }

    /**
     * 通知機會卡階段輪次
     */
    private void notifyFunctionCardPhaseTurn() {
        PlayerInfo current = players.get(functionCardTurnIndex);

        // 跳過旁觀者
        if (current.isSpectator()) {
            current.setConfirmedFunctionCardPhase(true);
            advanceFunctionCardPhase();
            return;
        }

        for (PlayerInfo p : players) {
            if (p == current) {
                p.send(Protocol.FUNCTION_CARD_PHASE + Protocol.DELIMITER + "YOUR");
                p.send(Protocol.MSG + Protocol.DELIMITER + "輪到你選擇是否使用機會卡");
            } else {
                p.send(Protocol.FUNCTION_CARD_PHASE + Protocol.DELIMITER + "WAIT");
                p.send(Protocol.MSG + Protocol.DELIMITER + "等待 " + current.getName() + " 選擇機會卡...");
            }
        }
    }

    /**
     * 機會卡階段輪到下一人
     */
    private void advanceFunctionCardPhase() {
        functionCardTurnIndex = (functionCardTurnIndex + 1) % players.size();

        // 檢查是否繞回莊家（所有人都確認過了）
        if (functionCardTurnIndex == dealerIndex) {
            endFunctionCardPhase();
            return;
        }

        // 跳過已確認的玩家
        PlayerInfo next = players.get(functionCardTurnIndex);
        if (next.hasConfirmedFunctionCardPhase() || next.isSpectator()) {
            advanceFunctionCardPhase();
            return;
        }

        notifyFunctionCardPhaseTurn();
    }

    /**
     * 結束機會卡階段，進入玩家行動階段
     */
    private void endFunctionCardPhase() {
        functionCardPhase = false;
        broadcast(Protocol.FUNCTION_CARD_PHASE_END);
        broadcast(Protocol.MSG + Protocol.DELIMITER + "=== 機會卡階段結束，開始遊戲行動 ===");

        // 設定行動輪次，從莊家下一位開始
        turnIndex = (dealerIndex + 1) % players.size();
        checkAndNotifyTurn();
    }
}
