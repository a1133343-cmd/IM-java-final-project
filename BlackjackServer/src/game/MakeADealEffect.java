import java.util.List;

/**
 * 「做個交易」效果
 * 與目標玩家互換手牌
 */
public class MakeADealEffect implements FunctionCardEffect {

    @Override
    public void execute(GameRoom room, PlayerInfo user, String targetUid, List<PlayerInfo> allPlayers) {
        // 找到目標玩家（支援 UID 或名稱匹配）
        PlayerInfo target = null;
        for (PlayerInfo p : allPlayers) {
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
        room.broadcast(Protocol.FUNCTION_CARD_USED + Protocol.DELIMITER
                + user.getName() + Protocol.DELIMITER
                + getDisplayName() + Protocol.DELIMITER
                + target.getName());
        room.broadcast(Protocol.MSG + Protocol.DELIMITER
                + user.getName() + " 使用了「" + getDisplayName() + "」與 " + target.getName() + " 互換手牌！");
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "做個交易";
    }
}
