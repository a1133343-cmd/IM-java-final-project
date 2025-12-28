import javax.swing.*;
import java.awt.*;

/**
 * 機會牌 UI 面板
 * 顯示單張機會牌的資訊和使用按鈕
 */
public class FunctionCardPanel extends JPanel {
    private final int cardId;
    private final String cardType;
    private final String displayName;
    private final String description;
    private final JButton useButton;

    // 機會牌類型對應顯示名稱和描述
    private static String getDisplayName(String type) {
        switch (type) {
            case "MAKE_A_DEAL":
                return "做個交易";
            case "FORCE_DRAW":
                return "我叫你抽";
            case "HEAL_ONE_HP":
                return "我喝一口";
            default:
                return type;
        }
    }

    private static String getDescription(String type) {
        switch (type) {
            case "MAKE_A_DEAL":
                return "與一位玩家互換手牌";
            case "FORCE_DRAW":
                return "強迫所有玩家抽一張牌";
            case "HEAL_ONE_HP":
                return "回一滴血";
            default:
                return "";
        }
    }

    public FunctionCardPanel(int cardId, String cardType) {
        this.cardId = cardId;
        this.cardType = cardType;
        this.displayName = getDisplayName(cardType);
        this.description = getDescription(cardType);

        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(60, 60, 100));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 215, 0), 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        setPreferredSize(new Dimension(120, 100));

        // 卡牌名稱
        JLabel nameLabel = new JLabel(displayName, SwingConstants.CENTER);
        nameLabel.setFont(new Font("微軟正黑體", Font.BOLD, 14));
        nameLabel.setForeground(new Color(255, 215, 0));

        // 效果描述
        JLabel descLabel = new JLabel("<html><center>" + description + "</center></html>", SwingConstants.CENTER);
        descLabel.setFont(new Font("微軟正黑體", Font.PLAIN, 10));
        descLabel.setForeground(Color.WHITE);

        // 使用按鈕
        useButton = new JButton("使用");
        useButton.setBackground(new Color(70, 160, 70));
        useButton.setForeground(new Color(30, 30, 30)); // 深色文字確保清晰
        useButton.setFocusPainted(false);
        useButton.setOpaque(true);
        useButton.setBorderPainted(true);
        useButton.setFont(new Font("微軟正黑體", Font.BOLD, 12));

        add(nameLabel, BorderLayout.NORTH);
        add(descLabel, BorderLayout.CENTER);
        add(useButton, BorderLayout.SOUTH);
    }

    public int getCardId() {
        return cardId;
    }

    public String getCardType() {
        return cardType;
    }

    public JButton getUseButton() {
        return useButton;
    }

    /**
     * 從協定字串創建機會牌面板
     * 格式: id,TYPE_NAME
     */
    public static FunctionCardPanel fromProtocol(String data) {
        String[] parts = data.split(",");
        if (parts.length >= 2) {
            try {
                int id = Integer.parseInt(parts[0]);
                String type = parts[1];
                return new FunctionCardPanel(id, type);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 設定按鈕是否可用
     */
    public void setEnabled(boolean enabled) {
        useButton.setEnabled(enabled);
    }
}
