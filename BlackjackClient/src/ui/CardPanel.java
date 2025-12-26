import javax.swing.*;
import java.awt.*;

/**
 * 撲克牌 UI 元件
 */
public class CardPanel extends JPanel {
    private static final int CARD_WIDTH = 65;
    private static final int CARD_HEIGHT = 95;

    /**
     * 建立撲克牌視覺元件
     * @param rank 牌面 (2-10, J, Q, K, A)
     * @param suitSymbol 花色符號 (♥, ♦, ♣, ♠)
     * @param color 花色顏色 (紅或黑)
     */
    public CardPanel(String rank, String suitSymbol, Color color) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        if ("?".equals(rank)) {
            // 隱藏牌背面
            setBackground(new Color(139, 0, 0));
        } else {
            JLabel rankLabel = new JLabel(rank, SwingConstants.LEFT);
            rankLabel.setForeground(color);
            rankLabel.setFont(new Font("Arial", Font.BOLD, 14));
            rankLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 0));

            JLabel suitLabel = new JLabel(suitSymbol, SwingConstants.CENTER);
            suitLabel.setForeground(color);
            suitLabel.setFont(new Font("Dialog", Font.PLAIN, 36));

            add(rankLabel, BorderLayout.NORTH);
            add(suitLabel, BorderLayout.CENTER);
        }
    }

    /**
     * 建立隱藏牌元件
     */
    public static CardPanel createHiddenCard() {
        return new CardPanel("?", "?", new Color(139, 0, 0));
    }

    /**
     * 從協定格式建立牌元件
     * @param cardData 格式: "rank,suit" (例如 "A,H")
     */
    public static CardPanel fromProtocol(String cardData) {
        if ("HIDDEN".equals(cardData)) {
            return createHiddenCard();
        }
        
        String[] parts = cardData.split(",");
        if (parts.length >= 2) {
            String rank = parts[0];
            String suit = parts[1];
            Color color = ("H".equals(suit) || "D".equals(suit)) ? Color.RED : Color.BLACK;
            String symbol = getSuitSymbol(suit);
            return new CardPanel(rank, symbol, color);
        }
        
        return createHiddenCard();
    }

    private static String getSuitSymbol(String suit) {
        switch (suit) {
            case "H": return "♥";
            case "D": return "♦";
            case "C": return "♣";
            case "S": return "♠";
            default: return "";
        }
    }
}
