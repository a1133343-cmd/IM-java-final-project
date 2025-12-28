import java.util.ArrayList;
import java.util.List;

/**
 * 玩家資訊類別 - 封裝玩家狀態
 */
public class PlayerInfo {
    private final ClientHandler handler;
    private Hand hand;
    private int hp;
    private boolean isDealer;
    private boolean hasStayed;
    private boolean isReady;
    private boolean isSpectator;
    private boolean joinedMidGame = false; // 是否為中途加入的旁觀者（需等場結束才恢復）
    private List<FunctionCard> functionCards = new ArrayList<>();
    private boolean usedFunctionCardThisRound = false; // 本輪是否已使用機會卡
    private boolean confirmedFunctionCardPhase = false; // 是否已確認機會卡階段

    public PlayerInfo(ClientHandler handler) {
        this.handler = handler;
        this.hp = 10;
        this.hand = new Hand();
        this.isDealer = false;
        this.hasStayed = false;
        this.isReady = true;
        this.isSpectator = false;
    }

    public ClientHandler getHandler() {
        return handler;
    }

    public Hand getHand() {
        return hand;
    }

    public void setHand(Hand hand) {
        this.hand = hand;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void decreaseHp(int amount) {
        this.hp -= amount;
    }

    public boolean isDealer() {
        return isDealer;
    }

    public void setDealer(boolean dealer) {
        this.isDealer = dealer;
    }

    public boolean hasStayed() {
        return hasStayed;
    }

    public void setHasStayed(boolean hasStayed) {
        this.hasStayed = hasStayed;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    /**
     * 重置手牌 (新一局開始時)
     */
    public void resetHand() {
        this.hand = new Hand();
        this.hasStayed = false;
    }

    /**
     * 取得玩家名稱
     */
    public String getName() {
        return handler != null ? handler.getName() : "CPU";
    }

    /**
     * 取得玩家 UID（用於伺服器內部識別）
     */
    public String getUid() {
        return handler != null ? handler.getUid() : "cpu";
    }

    /**
     * 是否為旁觀者
     */
    public boolean isSpectator() {
        return isSpectator;
    }

    /**
     * 設定旁觀者狀態
     */
    public void setSpectator(boolean spectator) {
        this.isSpectator = spectator;
    }

    /**
     * 是否為中途加入的旁觀者（需等場結束才恢復）
     */
    public boolean isJoinedMidGame() {
        return joinedMidGame;
    }

    /**
     * 設定是否為中途加入的旁觀者
     */
    public void setJoinedMidGame(boolean joinedMidGame) {
        this.joinedMidGame = joinedMidGame;
    }

    /**
     * 發送訊息給此玩家
     */
    public void send(String message) {
        if (handler != null) {
            handler.send(message);
        }
    }

    // ==================== 機會牌管理 ====================

    /**
     * 取得機會牌列表
     */
    public List<FunctionCard> getFunctionCards() {
        return functionCards;
    }

    /**
     * 新增機會牌
     */
    public void addFunctionCard(FunctionCard card) {
        functionCards.add(card);
    }

    /**
     * 移除指定 ID 的機會牌
     */
    public FunctionCard removeFunctionCard(int cardId) {
        for (int i = 0; i < functionCards.size(); i++) {
            if (functionCards.get(i).getId() == cardId) {
                return functionCards.remove(i);
            }
        }
        return null;
    }

    /**
     * 清空機會牌
     */
    public void clearFunctionCards() {
        functionCards.clear();
    }

    /**
     * 機會牌列表轉協定字串
     */
    public String getFunctionCardsProtocol() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < functionCards.size(); i++) {
            if (i > 0)
                sb.append(";");
            sb.append(functionCards.get(i).toProtocol());
        }
        return sb.toString();
    }

    // ==================== 機會卡階段狀態 ====================

    public boolean hasUsedFunctionCardThisRound() {
        return usedFunctionCardThisRound;
    }

    public void setUsedFunctionCardThisRound(boolean used) {
        this.usedFunctionCardThisRound = used;
    }

    public boolean hasConfirmedFunctionCardPhase() {
        return confirmedFunctionCardPhase;
    }

    public void setConfirmedFunctionCardPhase(boolean confirmed) {
        this.confirmedFunctionCardPhase = confirmed;
    }

    /**
     * 重置機會卡階段狀態（每輪開始時呼叫）
     */
    public void resetFunctionCardPhase() {
        this.usedFunctionCardThisRound = false;
        this.confirmedFunctionCardPhase = false;
    }
}
