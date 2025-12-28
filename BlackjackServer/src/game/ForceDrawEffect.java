import java.util.List;

/**
 * 「我叫你抽」效果
 * 強迫所有玩家（包含自己）抽一張牌
 * 注意：若有玩家爆牌，僅通知不扣血（在 21 點遊戲前使用）
 */
public class ForceDrawEffect implements FunctionCardEffect {

    @Override
    public void execute(GameRoom room, PlayerInfo user, String targetUid, List<PlayerInfo> allPlayers) {
        StringBuilder bustPlayers = new StringBuilder();
        int bustCount = 0;

        // 強迫所有非旁觀者玩家各抽一張牌
        for (PlayerInfo p : allPlayers) {
            if (p.isSpectator()) {
                continue;
            }

            // 從牌堆抽一張牌
            Card card = room.drawCard();
            if (card != null) {
                p.getHand().add(card);

                // 檢查是否爆牌
                if (p.getHand().isBust()) {
                    if (bustCount > 0) {
                        bustPlayers.append("、");
                    }
                    bustPlayers.append(p.getName());
                    bustCount++;
                }
            }
        }

        // 廣播使用通知
        room.broadcast(Protocol.FUNCTION_CARD_USED + Protocol.DELIMITER
                + user.getName() + Protocol.DELIMITER
                + getDisplayName() + Protocol.DELIMITER
                + "所有玩家");
        room.broadcast(Protocol.MSG + Protocol.DELIMITER
                + user.getName() + " 使用了「" + getDisplayName() + "」！所有玩家各抽了一張牌！");

        // 若有玩家爆牌，發送通知（但不扣血）
        if (bustCount > 0) {
            room.broadcast(Protocol.MSG + Protocol.DELIMITER
                    + "⚠️ " + bustPlayers.toString() + " 爆牌了！（將在回合結算時處理）");
        }
    }

    @Override
    public boolean requiresTarget() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return "我叫你抽";
    }
}
