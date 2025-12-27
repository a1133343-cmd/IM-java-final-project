import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * 21點客戶端主視窗
 */
public class BlackjackClient extends JFrame {
    private static final int SERVER_PORT = 12345;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    // Panels
    private final LoginPanel loginPanel;
    private final LobbyPanel lobbyPanel;
    private final GamePanel gamePanel;

    // Network
    private final NetworkClient networkClient;

    // State
    private String playerName;
    private boolean isPveMode = false;

    public BlackjackClient() {
        setTitle("Blackjack 21點: 絕地求生版");
        setSize(1000, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 初始化網路客戶端
        networkClient = new NetworkClient();
        networkClient.setMessageHandler(new MessageHandler(this));

        // 初始化 UI
        loginPanel = new LoginPanel();
        lobbyPanel = new LobbyPanel();
        gamePanel = new GamePanel();

        // 設定事件監聽
        setupEventListeners();

        // 加入 Panels
        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(gamePanel, "GAME");

        add(mainPanel);
    }

    private void setupEventListeners() {
        // 登入按鈕
        loginPanel.getLoginButton().addActionListener(e -> {
            String ip = loginPanel.getIp();
            playerName = loginPanel.getPlayerName();
            if (!ip.isEmpty() && !playerName.isEmpty()) {
                connectToServer(ip, playerName);
            } else {
                JOptionPane.showMessageDialog(this, "請輸入 IP 與 暱稱");
            }
        });

        // 大廳按鈕
        lobbyPanel.getPveButton().addActionListener(e -> networkClient.send(Protocol.PVE_START));

        lobbyPanel.getCreateRoomButton().addActionListener(e -> networkClient.send(Protocol.CREATE_ROOM));

        lobbyPanel.getJoinRoomButton().addActionListener(e -> {
            String roomId = lobbyPanel.getRoomId();
            if (!roomId.isEmpty()) {
                networkClient.send(Protocol.JOIN_ROOM + Protocol.DELIMITER + roomId);
            }
        });

        // 遊戲按鈕
        gamePanel.getStartGameButton().addActionListener(e -> networkClient.send(Protocol.START));

        gamePanel.getHitButton().addActionListener(e -> networkClient.send(Protocol.HIT));

        gamePanel.getStandButton().addActionListener(e -> networkClient.send(Protocol.STAND));

        gamePanel.getLeaveButton().addActionListener(e -> networkClient.send(Protocol.LEAVE));

        // 不使用機會卡按鈕
        gamePanel.getSkipFunctionCardButton().addActionListener(e -> networkClient.send(Protocol.SKIP_FUNCTION_CARD));

        // 聊天
        gamePanel.getSendChatButton().addActionListener(e -> sendChat());
        gamePanel.getChatInput().addActionListener(e -> sendChat());
    }

    private void sendChat() {
        String text = gamePanel.getChatInput().getText();
        if (!text.isEmpty()) {
            networkClient.send(Protocol.CHAT + Protocol.DELIMITER + text);
            gamePanel.getChatInput().setText("");
        }
    }

    private void connectToServer(String ip, String name) {
        new Thread(() -> {
            try {
                String uid = loginPanel.getUid();
                networkClient.connect(ip, SERVER_PORT, uid, name);
                // 連線成功後儲存配置
                SwingUtilities.invokeLater(() -> loginPanel.saveCurrentConfig());
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "無法連線至 " + ip + "\n請確認 IP 正確且防火牆已開啟 Port 12345");
                });
            }
        }).start();
    }

    // ==================== Public API ====================

    public void showPanel(String name) {
        cardLayout.show(mainPanel, name);
    }

    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isPveMode() {
        return isPveMode;
    }

    public void setPveMode(boolean pveMode) {
        this.isPveMode = pveMode;
    }

    public void checkStartButtonVisibility() {
        String txt = gamePanel.getPlayerListArea().getText();
        JButton startBtn = gamePanel.getStartGameButton();

        if (txt.contains(playerName + "(莊)") || isPveMode) {
            startBtn.setVisible(true);
            if (isPveMode) {
                startBtn.setText("開始下一局");
            } else {
                startBtn.setText("開始下一局 (你是莊家)");
            }
        } else {
            startBtn.setVisible(false);
        }
    }

    // ==================== 功能牌相關 ====================

    /**
     * 更新功能牌 UI
     * 
     * @param cardsData 格式: id,type;id,type;...
     */
    public void updateFunctionCards(String cardsData) {
        gamePanel.clearFunctionCards();

        if (cardsData == null || cardsData.isEmpty()) {
            return;
        }

        String[] cards = cardsData.split(";");
        for (String cardData : cards) {
            if (cardData.isEmpty())
                continue;

            FunctionCardPanel cardPanel = FunctionCardPanel.fromProtocol(cardData);
            if (cardPanel != null) {
                // 設定使用按鈕事件
                final int cardId = cardPanel.getCardId();
                final String cardType = cardPanel.getCardType();

                cardPanel.getUseButton().addActionListener(e -> {
                    useFunctionCard(cardId, cardType);
                });

                gamePanel.getFunctionCardArea().add(cardPanel);
            }
        }

        gamePanel.getFunctionCardArea().revalidate();
        gamePanel.getFunctionCardArea().repaint();
    }

    /**
     * 使用功能牌
     */
    private void useFunctionCard(int cardId, String cardType) {
        // 根據功能牌類型決定如何選擇目標
        if ("MAKE_A_DEAL".equals(cardType)) {
            // 需要選擇目標玩家
            String targetUid = promptSelectPlayer();
            if (targetUid != null) {
                networkClient.send(
                        Protocol.USE_FUNCTION_CARD + Protocol.DELIMITER + cardId + Protocol.DELIMITER + targetUid);
            }
        } else {
            // 其他功能牌可能不需要目標
            networkClient.send(Protocol.USE_FUNCTION_CARD + Protocol.DELIMITER + cardId + Protocol.DELIMITER + "");
        }
    }

    /**
     * 彈出玩家選擇對話框
     * 
     * @return 選中玩家的 UID，取消返回 null
     */
    private String promptSelectPlayer() {
        // 從玩家列表解析其他玩家
        String listText = gamePanel.getPlayerListArea().getText();
        java.util.List<String> otherPlayers = new java.util.ArrayList<>();

        for (String line : listText.split("\n")) {
            if (line.isEmpty())
                continue;
            // 格式: Name(莊):15HP 或 Name:15HP
            String name = line.split(":")[0].replace("(莊)", "").replace("(旁觀)", "").trim();
            if (!name.equals(playerName) && !name.isEmpty()) {
                otherPlayers.add(name);
            }
        }

        if (otherPlayers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "沒有可選擇的玩家");
            return null;
        }

        String[] options = otherPlayers.toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(
                this,
                "選擇要交換手牌的玩家：",
                "做個交易",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        // 注意：這裡簡化處理，將名稱作為 UID 傳送
        // 實際應該從 Server 獲取 UID，但目前架構中玩家列表只顯示名稱
        // Server 端 executeMakeADeal 會根據 UID 或名稱進行匹配
        return selected;
    }
}
