import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * 遊戲主畫面
 */
public class GamePanel extends JPanel {
    // UI 元件
    private final JPanel dealerPanel;
    private final JPanel playerPanel;
    private final JLabel statusLabel;
    private final JLabel roomIdLabel;
    private final JTextArea playerListArea;
    private final JTextArea chatArea;
    private final JTextField chatInput;
    private final JButton sendChatButton;
    private final JButton startGameButton;
    private final JButton hitButton;
    private final JButton standButton;
    private final JButton leaveButton;
    private final JPanel functionCardArea; // 功能牌區域
    private final JButton skipFunctionCardButton; // 不使用機會卡按鈕

    // 狀態
    private String dealerTitleBase = "莊家區";
    private String playerTitleBase = "你的手牌";
    private String dealerScoreStr = "?";
    private String playerScoreStr = "0";

    public GamePanel() {
        setLayout(new BorderLayout());

        // 莊家區
        dealerPanel = new JPanel();
        dealerPanel.setBackground(new Color(30, 100, 30));
        dealerPanel.setPreferredSize(new Dimension(800, 180));
        dealerPanel.setBorder(createTitledBorder("莊家區"));

        // 中央區
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(30, 100, 30));

        // 頂部資訊列
        JPanel topInfoBar = new JPanel(new BorderLayout());
        topInfoBar.setBackground(new Color(30, 80, 30));

        statusLabel = new JLabel("準備中...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("微軟正黑體", Font.BOLD, 24));
        statusLabel.setForeground(Color.YELLOW);

        roomIdLabel = new JLabel("房號: --   ", SwingConstants.RIGHT);
        roomIdLabel.setFont(new Font("微軟正黑體", Font.BOLD, 18));
        roomIdLabel.setForeground(Color.WHITE);

        topInfoBar.add(statusLabel, BorderLayout.CENTER);
        topInfoBar.add(roomIdLabel, BorderLayout.EAST);

        // 玩家列表與聊天區
        JPanel infoSplit = new JPanel(new GridLayout(1, 2));

        // 左側：玩家列表 + 功能牌區
        JPanel leftPanel = new JPanel(new BorderLayout());

        playerListArea = new JTextArea();
        playerListArea.setEditable(false);
        playerListArea.setFont(new Font("Monospaced", Font.BOLD, 14));
        playerListArea.setBackground(Color.BLACK);
        playerListArea.setForeground(Color.GREEN);
        JScrollPane listScroll = new JScrollPane(playerListArea);
        listScroll.setBorder(createTitledBorder("存活玩家"));

        // 功能牌區域
        functionCardArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        functionCardArea.setBackground(new Color(40, 40, 60));
        functionCardArea.setBorder(createTitledBorder("機會卡"));
        functionCardArea.setPreferredSize(new Dimension(0, 120));

        // 不使用機會卡按鈕
        skipFunctionCardButton = new JButton("不使用機會卡");
        skipFunctionCardButton.setBackground(new Color(100, 100, 100));
        skipFunctionCardButton.setForeground(Color.WHITE);
        skipFunctionCardButton.setVisible(false);
        functionCardArea.add(skipFunctionCardButton);

        leftPanel.add(listScroll, BorderLayout.CENTER);
        leftPanel.add(functionCardArea, BorderLayout.SOUTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(Color.BLACK);
        chatArea.setForeground(Color.WHITE);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(createTitledBorder("聊天室"));

        infoSplit.add(leftPanel);
        infoSplit.add(chatScroll);

        // 聊天輸入
        JPanel chatBox = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        sendChatButton = new JButton("送出");
        chatBox.add(chatInput, BorderLayout.CENTER);
        chatBox.add(sendChatButton, BorderLayout.EAST);

        JPanel midWrap = new JPanel(new BorderLayout());
        midWrap.add(infoSplit, BorderLayout.CENTER);
        midWrap.add(chatBox, BorderLayout.SOUTH);

        centerPanel.add(topInfoBar, BorderLayout.NORTH);
        centerPanel.add(midWrap, BorderLayout.CENTER);

        // 玩家手牌區
        playerPanel = new JPanel();
        playerPanel.setBackground(new Color(30, 100, 30));
        playerPanel.setPreferredSize(new Dimension(800, 180));
        playerPanel.setBorder(createTitledBorder("你的手牌"));

        // 按鈕區
        JPanel btnPanel = new JPanel();
        startGameButton = new JButton("開始遊戲");
        startGameButton.setBackground(Color.ORANGE);
        startGameButton.setVisible(false);

        hitButton = new JButton("Hit (要牌)");
        standButton = new JButton("Stand (停牌)");
        leaveButton = new JButton("離開房間");

        btnPanel.add(startGameButton);
        btnPanel.add(hitButton);
        btnPanel.add(standButton);
        btnPanel.add(leaveButton);

        JPanel bottomWrap = new JPanel(new BorderLayout());
        bottomWrap.add(playerPanel, BorderLayout.CENTER);
        bottomWrap.add(btnPanel, BorderLayout.SOUTH);

        add(dealerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomWrap, BorderLayout.SOUTH);
    }

    // ==================== Getters ====================

    public JPanel getDealerPanel() {
        return dealerPanel;
    }

    public JPanel getPlayerPanel() {
        return playerPanel;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JLabel getRoomIdLabel() {
        return roomIdLabel;
    }

    public JTextArea getPlayerListArea() {
        return playerListArea;
    }

    public JTextArea getChatArea() {
        return chatArea;
    }

    public JTextField getChatInput() {
        return chatInput;
    }

    public JButton getSendChatButton() {
        return sendChatButton;
    }

    public JButton getStartGameButton() {
        return startGameButton;
    }

    public JButton getHitButton() {
        return hitButton;
    }

    public JButton getStandButton() {
        return standButton;
    }

    public JButton getLeaveButton() {
        return leaveButton;
    }

    public JButton getSkipFunctionCardButton() {
        return skipFunctionCardButton;
    }

    // ==================== UI 更新方法 ====================

    public void lockButtons() {
        hitButton.setEnabled(false);
        standButton.setEnabled(false);
    }

    public void unlockButtons() {
        hitButton.setEnabled(true);
        standButton.setEnabled(true);
    }

    public void resetUI() {
        dealerPanel.removeAll();
        playerPanel.removeAll();
        dealerScoreStr = "?";
        playerScoreStr = "0";
        refreshTitles();
        dealerPanel.repaint();
        playerPanel.repaint();
        lockButtons();
    }

    public void updatePlayerList(String data, String myName) {
        String txt = data.replace(",", "\n");
        playerListArea.setText(txt);

        if (txt.contains(myName + "(莊)")) {
            playerTitleBase = "你的手牌 (你是莊家)";
            dealerTitleBase = "自己 (莊家區)";
        } else {
            playerTitleBase = "你的手牌 (你是閒家)";
            String dealerName = "Unknown";
            for (String s : data.split(",")) {
                if (s.contains("(莊)")) {
                    dealerName = s.split(":")[0];
                    break;
                }
            }
            dealerTitleBase = "本局莊家: " + dealerName;
        }
        refreshTitles();
    }

    public void updateGameTable(String dealerHandStr, String playerHandStr) {
        dealerPanel.removeAll();
        playerPanel.removeAll();

        drawCards(dealerPanel, dealerHandStr);
        drawCards(playerPanel, playerHandStr);

        dealerScoreStr = calcScore(dealerHandStr);
        playerScoreStr = calcScore(playerHandStr);

        refreshTitles();

        dealerPanel.repaint();
        playerPanel.repaint();
    }

    public String getPlayerScoreStr() {
        return playerScoreStr;
    }

    private void refreshTitles() {
        setPanelTitle(dealerPanel, dealerTitleBase + " [" + dealerScoreStr + "點]");
        setPanelTitle(playerPanel, playerTitleBase + " [" + playerScoreStr + "點]");
    }

    private void setPanelTitle(JPanel panel, String title) {
        ((TitledBorder) panel.getBorder()).setTitle(title);
        panel.repaint();
    }

    private String calcScore(String hand) {
        if (hand.contains("HIDDEN"))
            return "?";

        int sum = 0;
        int aces = 0;
        for (String c : hand.split(";")) {
            if (c.isEmpty())
                continue;
            String rank = c.split(",")[0];
            if ("JQK".contains(rank)) {
                sum += 10;
            } else if ("A".equals(rank)) {
                sum += 11;
                aces++;
            } else {
                sum += Integer.parseInt(rank);
            }
        }
        while (sum > 21 && aces > 0) {
            sum -= 10;
            aces--;
        }
        return String.valueOf(sum);
    }

    private void drawCards(JPanel panel, String handStr) {
        for (String cardData : handStr.split(";")) {
            panel.add(CardPanel.fromProtocol(cardData));
        }
        panel.revalidate();
    }

    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("微軟正黑體", Font.BOLD, 16),
                Color.WHITE);
    }

    // ==================== 功能牌管理 ====================

    public JPanel getFunctionCardArea() {
        return functionCardArea;
    }

    /**
     * 清空功能牌區域 (保留「不使用」按鈕)
     */
    public void clearFunctionCards() {
        // 移除功能牌面板，但保留 skip 按鈕
        for (int i = functionCardArea.getComponentCount() - 1; i >= 0; i--) {
            Component comp = functionCardArea.getComponent(i);
            if (comp instanceof FunctionCardPanel) {
                functionCardArea.remove(comp);
            }
        }
        functionCardArea.revalidate();
        functionCardArea.repaint();
    }

    /**
     * 設定所有功能牌按鈕是否可用
     */
    public void setFunctionCardsEnabled(boolean enabled) {
        for (Component comp : functionCardArea.getComponents()) {
            if (comp instanceof FunctionCardPanel) {
                ((FunctionCardPanel) comp).setEnabled(enabled);
            }
        }
    }
}
