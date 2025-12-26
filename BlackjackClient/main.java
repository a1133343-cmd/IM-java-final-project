import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.net.*;

public class main extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    // 連線相關
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String myName;
    private final int SERVER_PORT = 12345; // Port 保持固定

    // [修正] 新增變數記錄是否為 PVE
    private boolean isPveMode = false;

    // GUI 元件
    private JTextField nameField, ipField, roomIdField, chatInput; // 新增 ipField
    private JTextArea chatArea, playerListArea;
    private JPanel dealerPanel, playerPanel;
    private JLabel statusLabel, lblRoomId;
    private JButton btnHit, btnStand, btnLeave, btnStartGame;

    // 狀態變數
    private String dealerTitleBase = "莊家區";
    private String playerTitleBase = "你的手牌";
    private String dealerScoreStr = "?";
    private String playerScoreStr = "0";

    public BlackjackClient() {
        setTitle("Blackjack 21點: 絕地求生版");
        setSize(1000, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initLoginPanel();
        initLobbyPanel();
        initGamePanel();

        add(mainPanel);
        // 注意：這裡不再自動連線，改由登入按鈕觸發
    }

    // [修改] 連線邏輯改為帶入 IP 與 Name
    private void connectAndLogin(String ip, String name) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, SERVER_PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // 連線成功後，發送登入指令
                out.println("LOGIN|" + name);

                // 開始監聽 Server 訊息
                listen();
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "無法連線至 " + ip + "\n請確認 IP 正確且防火牆已開啟 Port 12345");
                    // 連線失敗不關閉程式，讓使用者可以重試
                });
            }
        }).start();
    }

    private void listen() {
        try {
            String msg;
            while ((msg = in.readLine()) != null)
                processMessage(msg);
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "與伺服器斷線");
                System.exit(0);
            });
        }
    }

    private void processMessage(String msg) {
        String[] parts = msg.split("\\|");
        String cmd = parts[0];

        SwingUtilities.invokeLater(() -> {
            switch (cmd) {
                case "LOGIN_OK":
                    cardLayout.show(mainPanel, "LOBBY");
                    break;

                case "PVE_STARTED":
                    isPveMode = true;
                    statusLabel.setText("單人練習模式");
                    lblRoomId.setText("PVE Mode");
                    cardLayout.show(mainPanel, "GAME");
                    resetUI();
                    btnStartGame.setVisible(false);
                    break;

                case "ROOM_CREATED":
                    statusLabel.setText("等待玩家加入...");
                    lblRoomId.setText("房號: " + parts[1]);
                    cardLayout.show(mainPanel, "GAME");
                    resetUI();
                    btnStartGame.setVisible(true);
                    break;

                case "ROOM_JOINED":
                    statusLabel.setText("等待房主開始...");
                    lblRoomId.setText("房號: " + parts[1]);
                    cardLayout.show(mainPanel, "GAME");
                    resetUI();
                    btnStartGame.setVisible(false);
                    break;

                case "STATE":
                    if (parts.length >= 5) {
                        updateGameTable(parts[2], parts[3]);
                        updatePlayerList(parts[4]);
                    }
                    btnStartGame.setVisible(false);
                    break;

                case "TURN":
                    if (parts[1].equals("YOUR")) {
                        statusLabel.setText("輪到你了！ (目前點數: " + playerScoreStr + ")");
                        statusLabel.setForeground(Color.GREEN);
                        unlockButtons();
                    } else {
                        statusLabel.setText("等待其他玩家...");
                        statusLabel.setForeground(Color.YELLOW);
                        lockButtons();
                    }
                    break;

                case "GAME_OVER":
                    updateGameTable(parts[1], parts[2]);
                    String res = parts[3].replace("\\n", "\n");
                    JOptionPane.showMessageDialog(this, res, "回合結果", JOptionPane.INFORMATION_MESSAGE);
                    out.println("READY");
                    statusLabel.setText("等待其他玩家確認...");
                    statusLabel.setForeground(Color.WHITE);
                    lockButtons();
                    checkStartButtonVisibility();
                    break;

                case "HP_UPDATE":
                    updatePlayerList(parts[1]);
                    checkStartButtonVisibility();
                    break;

                case "CHAT":
                    if (parts.length > 1) {
                        chatArea.append(parts[1] + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                    break;

                case "MSG":
                    statusLabel.setText(parts[1]);
                    break;

                case "LOBBY":
                    isPveMode = false;
                    cardLayout.show(mainPanel, "LOBBY");
                    break;

                case "ERROR":
                    JOptionPane.showMessageDialog(this, parts[1]);
                    break;
            }
        });
    }

    private void checkStartButtonVisibility() {
        String txt = playerListArea.getText();
        if (txt.contains(myName + "(莊)") || isPveMode) {
            btnStartGame.setVisible(true);
            if (isPveMode) {
                btnStartGame.setText("開始下一局");
            } else {
                btnStartGame.setText("開始下一局 (你是莊家)");
            }
        } else {
            btnStartGame.setVisible(false);
        }
    }

    private void lockButtons() {
        btnHit.setEnabled(false);
        btnStand.setEnabled(false);
    }

    private void unlockButtons() {
        btnHit.setEnabled(true);
        btnStand.setEnabled(true);
    }

    private void resetUI() {
        dealerPanel.removeAll();
        playerPanel.removeAll();
        dealerScoreStr = "?";
        playerScoreStr = "0";
        refreshTitles();
        dealerPanel.repaint();
        playerPanel.repaint();
        lockButtons();
    }

    // --- UI Setup ---
    private void initLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(40, 40, 40));

        JLabel title = new JLabel("Blackjack Survival");
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.ORANGE);

        nameField = new JTextField(15);
        ipField = new JTextField("127.0.0.1", 15); // 預設 IP

        JButton btnLogin = new JButton("連線並進入遊戲");

        // [修改] 登入按鈕事件
        btnLogin.addActionListener(e -> {
            String ip = ipField.getText().trim();
            myName = nameField.getText().trim();
            if (!ip.isEmpty() && !myName.isEmpty()) {
                connectAndLogin(ip, myName);
            } else {
                JOptionPane.showMessageDialog(this, "請輸入 IP 與 暱稱");
            }
        });

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.gridx = 0;
        g.gridy = 0;
        panel.add(title, g);

        g.gridy++;
        panel.add(new JLabel("<html><font color='white'>伺服器 IP:</font></html>"), g);
        g.gridy++;
        panel.add(ipField, g);

        g.gridy++;
        panel.add(new JLabel("<html><font color='white'>暱稱:</font></html>"), g);
        g.gridy++;
        panel.add(nameField, g);

        g.gridy++;
        panel.add(btnLogin, g);

        mainPanel.add(panel, "LOGIN");
    }

    private void initLobbyPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(80, 200, 80, 200));

        JButton btnPVE = new JButton("單人練習 (PVE)");
        JButton btnCreate = new JButton("創建房間 (多人亂鬥)");

        JPanel joinPanel = new JPanel();
        roomIdField = new JTextField(6);
        JButton btnJoin = new JButton("加入房間");
        joinPanel.add(new JLabel("房號:"));
        joinPanel.add(roomIdField);
        joinPanel.add(btnJoin);

        btnPVE.addActionListener(e -> out.println("PVE_START"));
        btnCreate.addActionListener(e -> out.println("CREATE_ROOM"));
        btnJoin.addActionListener(e -> {
            if (!roomIdField.getText().isEmpty())
                out.println("JOIN_ROOM|" + roomIdField.getText());
        });

        panel.add(btnPVE);
        panel.add(btnCreate);
        panel.add(joinPanel);
        mainPanel.add(panel, "LOBBY");
    }

    private void initGamePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        dealerPanel = new JPanel();
        dealerPanel.setBackground(new Color(30, 100, 30));
        dealerPanel.setPreferredSize(new Dimension(800, 180));
        dealerPanel.setBorder(createTitledBorder("莊家區"));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(30, 100, 30));

        JPanel topInfoBar = new JPanel(new BorderLayout());
        topInfoBar.setBackground(new Color(30, 80, 30));

        statusLabel = new JLabel("準備中...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("微軟正黑體", Font.BOLD, 24));
        statusLabel.setForeground(Color.YELLOW);

        lblRoomId = new JLabel("房號: --   ", SwingConstants.RIGHT);
        lblRoomId.setFont(new Font("微軟正黑體", Font.BOLD, 18));
        lblRoomId.setForeground(Color.WHITE);

        topInfoBar.add(statusLabel, BorderLayout.CENTER);
        topInfoBar.add(lblRoomId, BorderLayout.EAST);

        JPanel infoSplit = new JPanel(new GridLayout(1, 2));

        playerListArea = new JTextArea();
        playerListArea.setEditable(false);
        playerListArea.setFont(new Font("Monospaced", Font.BOLD, 14));
        playerListArea.setBackground(Color.BLACK);
        playerListArea.setForeground(Color.GREEN);
        JScrollPane listScroll = new JScrollPane(playerListArea);
        listScroll.setBorder(createTitledBorder("存活玩家"));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(Color.BLACK);
        chatArea.setForeground(Color.WHITE);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(createTitledBorder("聊天室"));

        infoSplit.add(listScroll);
        infoSplit.add(chatScroll);

        JPanel chatBox = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        JButton btnSend = new JButton("送出");
        btnSend.addActionListener(e -> {
            if (!chatInput.getText().isEmpty()) {
                out.println("CHAT|" + chatInput.getText());
                chatInput.setText("");
            }
        });
        chatBox.add(chatInput, BorderLayout.CENTER);
        chatBox.add(btnSend, BorderLayout.EAST);

        JPanel midWrap = new JPanel(new BorderLayout());
        midWrap.add(infoSplit, BorderLayout.CENTER);
        midWrap.add(chatBox, BorderLayout.SOUTH);

        centerPanel.add(topInfoBar, BorderLayout.NORTH);
        centerPanel.add(midWrap, BorderLayout.CENTER);

        playerPanel = new JPanel();
        playerPanel.setBackground(new Color(30, 100, 30));
        playerPanel.setPreferredSize(new Dimension(800, 180));
        playerPanel.setBorder(createTitledBorder("你的手牌"));

        JPanel btnPanel = new JPanel();
        btnStartGame = new JButton("開始遊戲");
        btnStartGame.setBackground(Color.ORANGE);
        btnStartGame.setVisible(false);
        btnStartGame.addActionListener(e -> out.println("START"));

        btnHit = new JButton("Hit (要牌)");
        btnStand = new JButton("Stand (停牌)");
        btnHit.addActionListener(e -> out.println("HIT"));
        btnStand.addActionListener(e -> out.println("STAND"));
        btnLeave = new JButton("離開房間");
        btnLeave.addActionListener(e -> out.println("LEAVE"));

        btnPanel.add(btnStartGame);
        btnPanel.add(btnHit);
        btnPanel.add(btnStand);
        btnPanel.add(btnLeave);

        JPanel bottomWrap = new JPanel(new BorderLayout());
        bottomWrap.add(playerPanel, BorderLayout.CENTER);
        bottomWrap.add(btnPanel, BorderLayout.SOUTH);

        panel.add(dealerPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(bottomWrap, BorderLayout.SOUTH);

        mainPanel.add(panel, "GAME");
    }

    private void updatePlayerList(String data) {
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

    private void updateGameTable(String dHand, String mHand) {
        dealerPanel.removeAll();
        playerPanel.removeAll();

        drawCards(dealerPanel, dHand);
        drawCards(playerPanel, mHand);

        dealerScoreStr = calcScore(dHand);
        playerScoreStr = calcScore(mHand);

        refreshTitles();

        dealerPanel.repaint();
        playerPanel.repaint();
    }

    private void refreshTitles() {
        setPanelTitle(dealerPanel, dealerTitleBase + " [" + dealerScoreStr + "點]");
        setPanelTitle(playerPanel, playerTitleBase + " [" + playerScoreStr + "點]");
    }

    private void setPanelTitle(JPanel p, String title) {
        ((TitledBorder) p.getBorder()).setTitle(title);
        p.repaint();
    }

    private String calcScore(String hand) {
        if (hand.contains("HIDDEN"))
            return "?";
        int s = 0, a = 0;
        for (String c : hand.split(";")) {
            if (c.isEmpty())
                continue;
            String r = c.split(",")[0];
            if ("JQK".contains(r))
                s += 10;
            else if ("A".equals(r)) {
                s += 11;
                a++;
            } else
                s += Integer.parseInt(r);
        }
        while (s > 21 && a > 0) {
            s -= 10;
            a--;
        }
        return String.valueOf(s);
    }

    private void drawCards(JPanel p, String h) {
        for (String c : h.split(";")) {
            if (c.equals("HIDDEN"))
                p.add(createCard("?", "?", new Color(139, 0, 0)));
            else {
                String[] d = c.split(",");
                if (d.length >= 2) {
                    Color col = (d[1].equals("H") || d[1].equals("D")) ? Color.RED : Color.BLACK;
                    p.add(createCard(d[0], getSuit(d[1]), col));
                }
            }
        }
        p.revalidate();
    }

    private JPanel createCard(String r, String s, Color c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setPreferredSize(new Dimension(65, 95));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        if (r.equals("?")) {
            p.setBackground(new Color(139, 0, 0));
            return p;
        }
        JLabel lr = new JLabel(r, SwingConstants.LEFT);
        lr.setForeground(c);
        lr.setFont(new Font("Arial", Font.BOLD, 14));
        lr.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 0));
        JLabel ls = new JLabel(s, SwingConstants.CENTER);
        ls.setForeground(c);
        ls.setFont(new Font("Dialog", Font.PLAIN, 36));
        p.add(lr, BorderLayout.NORTH);
        p.add(ls, BorderLayout.CENTER);
        return p;
    }

    private String getSuit(String s) {
        switch (s) {
            case "H":
                return "♥";
            case "D":
                return "♦";
            case "C":
                return "♣";
            case "S":
                return "♠";
            default:
                return "";
        }
    }

    private TitledBorder createTitledBorder(String t) {
        return BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), t,
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("微軟正黑體", Font.BOLD, 16),
                Color.WHITE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BlackjackClient().setVisible(true));
    }
}