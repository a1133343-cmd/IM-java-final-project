import javax.swing.*;
import java.awt.*;

/**
 * 訊息處理器 - 處理伺服器訊息並更新 UI
 */
public class MessageHandler {
    private final BlackjackClient client;

    public MessageHandler(BlackjackClient client) {
        this.client = client;
    }

    /**
     * 處理伺服器訊息
     */
    public void handleMessage(String msg) {
        String[] parts = msg.split("\\|");
        String cmd = parts[0];

        SwingUtilities.invokeLater(() -> {
            switch (cmd) {
                case Protocol.LOGIN_OK:
                    client.showPanel("LOBBY");
                    break;

                case Protocol.PVE_STARTED:
                    client.setPveMode(true);
                    client.getGamePanel().getStatusLabel().setText("單人練習模式");
                    client.getGamePanel().getRoomIdLabel().setText("PVE Mode");
                    client.showPanel("GAME");
                    client.getGamePanel().resetUI();
                    client.getGamePanel().getStartGameButton().setVisible(false);
                    break;

                case Protocol.ROOM_CREATED:
                    client.getGamePanel().getStatusLabel().setText("等待玩家加入...");
                    client.getGamePanel().getRoomIdLabel().setText("房號: " + parts[1]);
                    client.showPanel("GAME");
                    client.getGamePanel().resetUI();
                    client.getGamePanel().getStartGameButton().setVisible(true);
                    break;

                case Protocol.ROOM_JOINED:
                    client.getGamePanel().getStatusLabel().setText("等待房主開始...");
                    client.getGamePanel().getRoomIdLabel().setText("房號: " + parts[1]);
                    client.showPanel("GAME");
                    client.getGamePanel().resetUI();
                    client.getGamePanel().getStartGameButton().setVisible(false);
                    break;

                case Protocol.STATE:
                    if (parts.length >= 5) {
                        client.getGamePanel().updateGameTable(parts[2], parts[3]);
                        client.getGamePanel().updatePlayerList(parts[4], client.getPlayerName());
                    }
                    client.getGamePanel().getStartGameButton().setVisible(false);
                    // 遊戲進行中禁用功能牌
                    client.getGamePanel().setFunctionCardsEnabled(false);
                    break;

                case Protocol.TURN:
                    if ("YOUR".equals(parts[1])) {
                        client.getGamePanel().getStatusLabel()
                                .setText("輪到你了！ (目前點數: " + client.getGamePanel().getPlayerScoreStr() + ")");
                        client.getGamePanel().getStatusLabel().setForeground(Color.GREEN);
                        client.getGamePanel().unlockButtons();
                    } else {
                        client.getGamePanel().getStatusLabel().setText("等待其他玩家...");
                        client.getGamePanel().getStatusLabel().setForeground(Color.YELLOW);
                        client.getGamePanel().lockButtons();
                    }
                    // 遊戲進行中禁用功能牌
                    client.getGamePanel().setFunctionCardsEnabled(false);
                    break;

                case Protocol.GAME_OVER:
                    client.getGamePanel().updateGameTable(parts[1], parts[2]);
                    String result = parts[3].replace("\\n", "\n");
                    JOptionPane.showMessageDialog(client, result, "回合結果", JOptionPane.INFORMATION_MESSAGE);
                    client.getNetworkClient().send(Protocol.READY);
                    client.getGamePanel().getStatusLabel().setText("等待其他玩家確認...");
                    client.getGamePanel().getStatusLabel().setForeground(Color.WHITE);
                    client.getGamePanel().lockButtons();
                    client.checkStartButtonVisibility();
                    // 回合結束，允許使用功能牌
                    client.getGamePanel().setFunctionCardsEnabled(true);
                    break;

                case Protocol.HP_UPDATE:
                    client.getGamePanel().updatePlayerList(parts[1], client.getPlayerName());
                    client.checkStartButtonVisibility();
                    break;

                case Protocol.CHAT:
                    if (parts.length > 1) {
                        // 拼接所有訊息部分，避免 | 符號導致訊息被截斷
                        StringBuilder message = new StringBuilder(parts[1]);
                        for (int i = 2; i < parts.length; i++) {
                            message.append("|").append(parts[i]);
                        }
                        JTextArea chatArea = client.getGamePanel().getChatArea();
                        chatArea.append(message.toString() + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                    break;

                case Protocol.MSG:
                    client.getGamePanel().getStatusLabel().setText(parts[1]);
                    break;

                case Protocol.LOBBY:
                    client.setPveMode(false);
                    client.showPanel("LOBBY");
                    client.getGamePanel().clearFunctionCards();
                    break;

                case Protocol.FUNCTION_CARDS:
                    // 更新功能牌 UI
                    client.updateFunctionCards(parts.length > 1 ? parts[1] : "");
                    break;

                case Protocol.FUNCTION_CARD_USED:
                    // 功能牌使用通知（已在聊天室透過 MSG 顯示，此處可做額外處理）
                    if (parts.length >= 4) {
                        JTextArea chatArea = client.getGamePanel().getChatArea();
                        chatArea.append("[機會卡] " + parts[1] + " 使用了「" + parts[2] + "」對 " + parts[3] + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                    break;

                case Protocol.FUNCTION_CARD_PHASE:
                    // 機會卡階段輪次通知
                    if (parts.length > 1 && "YOUR".equals(parts[1])) {
                        // 輪到自己選擇機會卡
                        client.getGamePanel().getStatusLabel().setText("選擇使用機會卡或點「不使用機會卡」");
                        client.getGamePanel().getStatusLabel().setForeground(Color.ORANGE);
                        client.getGamePanel().setFunctionCardsEnabled(true);
                        client.getGamePanel().getSkipFunctionCardButton().setVisible(true);
                    } else {
                        // 等待其他玩家
                        client.getGamePanel().setFunctionCardsEnabled(false);
                        client.getGamePanel().getSkipFunctionCardButton().setVisible(false);
                    }
                    break;

                case Protocol.FUNCTION_CARD_PHASE_END:
                    // 機會卡階段結束
                    client.getGamePanel().getSkipFunctionCardButton().setVisible(false);
                    client.getGamePanel().setFunctionCardsEnabled(false);
                    break;

                case Protocol.GAME_WIN:
                    // 遊戲勝利通知
                    if (parts.length > 1) {
                        String winnerName = parts[1];
                        String winMessage;
                        if (winnerName.equals(client.getPlayerName())) {
                            winMessage = "🎉 恭喜！你是最後的贏家！";
                        } else {
                            winMessage = "🏆 遊戲結束！\n贏家是：" + winnerName;
                        }
                        JOptionPane.showMessageDialog(client, winMessage, "遊戲勝利", JOptionPane.INFORMATION_MESSAGE);
                        client.getGamePanel().getStatusLabel().setText("遊戲結束，等待新一局開始...");
                        client.getGamePanel().getStatusLabel().setForeground(Color.CYAN);
                        client.checkStartButtonVisibility();
                    }
                    break;

                case Protocol.ERROR:
                    JOptionPane.showMessageDialog(client, parts[1]);
                    break;
            }
        });
    }

    /**
     * 連線中斷時呼叫
     */
    public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(client, "與伺服器斷線");
            System.exit(0);
        });
    }
}
