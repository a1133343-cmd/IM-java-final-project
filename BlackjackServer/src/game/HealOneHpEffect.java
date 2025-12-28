import java.util.List;

/**
 * 「我喝一口」效果
 * 使用者回復 1 HP（允許超過 15）
 */
public class HealOneHpEffect implements FunctionCardEffect {

    @Override
    public void execute(GameRoom room, PlayerInfo user, String targetUid, List<PlayerInfo> allPlayers) {
        // 回復 1 HP（無上限限制）
        int oldHp = user.getHp();
        user.setHp(oldHp + 1);
        int newHp = user.getHp();

        // 發送 HP 更新給使用者
        user.send(Protocol.HP_UPDATE + Protocol.DELIMITER + newHp);

        // 廣播使用通知
        room.broadcast(Protocol.FUNCTION_CARD_USED + Protocol.DELIMITER
                + user.getName() + Protocol.DELIMITER
                + getDisplayName() + Protocol.DELIMITER
                + user.getName());
        room.broadcast(Protocol.MSG + Protocol.DELIMITER
                + user.getName() + " 使用了「" + getDisplayName() + "」！HP " + oldHp + " → " + newHp);
    }

    @Override
    public boolean requiresTarget() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return "我喝一口";
    }
}
