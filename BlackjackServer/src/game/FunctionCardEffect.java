import java.util.List;

/**
 * 機會牌效果策略介面
 * 定義機會牌效果的執行方法
 */
public interface FunctionCardEffect {
    
    /**
     * 執行機會牌效果
     * @param room 遊戲房間
     * @param user 使用者
     * @param targetUid 目標玩家 UID（若需要）
     * @param allPlayers 所有玩家列表（供效果存取）
     */
    void execute(GameRoom room, PlayerInfo user, String targetUid, List<PlayerInfo> allPlayers);
    
    /**
     * 此效果是否需要選擇目標玩家
     * @return true 表示需要選擇目標，false 表示不需要
     */
    boolean requiresTarget();
    
    /**
     * 取得效果的顯示名稱
     */
    String getDisplayName();
}
