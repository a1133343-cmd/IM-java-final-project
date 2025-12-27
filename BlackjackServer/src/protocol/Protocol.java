/**
 * 通訊協定常數
 */
public final class Protocol {
    private Protocol() {
    } // 禁止實例化

    // === Client -> Server ===
    public static final String LOGIN = "LOGIN"; // LOGIN|uid|name
    public static final String PVE_START = "PVE_START";
    public static final String CREATE_ROOM = "CREATE_ROOM";
    public static final String JOIN_ROOM = "JOIN_ROOM";
    public static final String START = "START";
    public static final String READY = "READY";
    public static final String CHAT = "CHAT";
    public static final String HIT = "HIT";
    public static final String STAND = "STAND";
    public static final String LEAVE = "LEAVE";

    // === Server -> Client ===
    public static final String LOGIN_OK = "LOGIN_OK";
    public static final String PVE_STARTED = "PVE_STARTED";
    public static final String ROOM_CREATED = "ROOM_CREATED";
    public static final String ROOM_JOINED = "ROOM_JOINED";
    public static final String STATE = "STATE";
    public static final String TURN = "TURN";
    public static final String GAME_OVER = "GAME_OVER";
    public static final String HP_UPDATE = "HP_UPDATE";
    public static final String MSG = "MSG";
    public static final String LOBBY = "LOBBY";
    public static final String ERROR = "ERROR";
    public static final String GAME_WIN = "GAME_WIN"; // Server: GAME_WIN|winnerName

    // === 功能牌相關 ===
    public static final String USE_FUNCTION_CARD = "USE_FUNC_CARD"; // Client: USE_FUNC_CARD|cardId|targetUid
    public static final String SKIP_FUNCTION_CARD = "SKIP_FUNC_CARD"; // Client: 跳過使用機會卡
    public static final String FUNCTION_CARDS = "FUNC_CARDS"; // Server: FUNC_CARDS|id,type;id,type;...
    public static final String FUNCTION_CARD_USED = "FUNC_CARD_USED"; // Server:
                                                                      // FUNC_CARD_USED|userName|cardType|targetName
    public static final String FUNCTION_CARD_PHASE = "FUNC_CARD_PHASE"; // Server: FUNC_CARD_PHASE|YOUR/WAIT
    public static final String FUNCTION_CARD_PHASE_END = "FUNC_PHASE_END"; // Server: 機會卡階段結束

    // === 協定分隔符號 ===
    public static final String DELIMITER = "|";
}
