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

    public PlayerInfo(ClientHandler handler) {
        this.handler = handler;
        this.hp = 15;
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
     * 發送訊息給此玩家
     */
    public void send(String message) {
        if (handler != null) {
            handler.send(message);
        }
    }
}
