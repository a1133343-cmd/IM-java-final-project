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
        lobbyPanel.getPveButton().addActionListener(e -> 
            networkClient.send(Protocol.PVE_START));
            
        lobbyPanel.getCreateRoomButton().addActionListener(e -> 
            networkClient.send(Protocol.CREATE_ROOM));
            
        lobbyPanel.getJoinRoomButton().addActionListener(e -> {
            String roomId = lobbyPanel.getRoomId();
            if (!roomId.isEmpty()) {
                networkClient.send(Protocol.JOIN_ROOM + Protocol.DELIMITER + roomId);
            }
        });
        
        // 遊戲按鈕
        gamePanel.getStartGameButton().addActionListener(e -> 
            networkClient.send(Protocol.START));
            
        gamePanel.getHitButton().addActionListener(e -> 
            networkClient.send(Protocol.HIT));
            
        gamePanel.getStandButton().addActionListener(e -> 
            networkClient.send(Protocol.STAND));
            
        gamePanel.getLeaveButton().addActionListener(e -> 
            networkClient.send(Protocol.LEAVE));
            
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
                networkClient.connect(ip, SERVER_PORT, name);
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
}
