/**
 * 通訊協定常數
 */
public final class Protocol {
    private Protocol() {} // 禁止實例化

    // === Client -> Server ===
    public static final String LOGIN = "LOGIN";
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

    // === 協定分隔符號 ===
    public static final String DELIMITER = "|";
}
