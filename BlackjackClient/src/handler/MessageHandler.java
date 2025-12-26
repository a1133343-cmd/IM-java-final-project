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
                    break;

                case Protocol.TURN:
                    if ("YOUR".equals(parts[1])) {
                        client.getGamePanel().getStatusLabel().setText("輪到你了！ (目前點數: " + client.getGamePanel().getPlayerScoreStr() + ")");
                        client.getGamePanel().getStatusLabel().setForeground(Color.GREEN);
                        client.getGamePanel().unlockButtons();
                    } else {
                        client.getGamePanel().getStatusLabel().setText("等待其他玩家...");
                        client.getGamePanel().getStatusLabel().setForeground(Color.YELLOW);
                        client.getGamePanel().lockButtons();
                    }
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
                    break;

                case Protocol.HP_UPDATE:
                    client.getGamePanel().updatePlayerList(parts[1], client.getPlayerName());
                    client.checkStartButtonVisibility();
                    break;

                case Protocol.CHAT:
                    if (parts.length > 1) {
                        JTextArea chatArea = client.getGamePanel().getChatArea();
                        chatArea.append(parts[1] + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                    break;

                case Protocol.MSG:
                    client.getGamePanel().getStatusLabel().setText(parts[1]);
                    break;

                case Protocol.LOBBY:
                    client.setPveMode(false);
                    client.showPanel("LOBBY");
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
