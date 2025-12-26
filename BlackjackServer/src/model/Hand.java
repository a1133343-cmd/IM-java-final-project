import java.util.ArrayList;
import java.util.List;

/**
 * 手牌類別 - 管理玩家持有的牌
 */
public class Hand {
    private final List<Card> cards = new ArrayList<>();

    public void add(Card card) {
        cards.add(card);
    }

    public List<Card> getCards() {
        return cards;
    }

    public int size() {
        return cards.size();
    }

    /**
     * 計算最佳點數 (自動處理 Ace 彈性: 11 或 1)
     */
    public int bestValue() {
        int sum = 0;
        int aces = 0;
        
        for (Card card : cards) {
            sum += card.getValue();
            if ("A".equals(card.getRank())) {
                aces++;
            }
        }
        
        // Ace 從 11 降為 1 以避免爆牌
        while (sum > 21 && aces > 0) {
            sum -= 10;
            aces--;
        }
        
        return sum;
    }

    /**
     * 是否爆牌
     */
    public boolean isBust() {
        return bestValue() > 21;
    }

    /**
     * 轉為協定字串
     * @param showAll 是否顯示所有牌
     * @param hideSecond 是否隱藏第二張牌
     */
    public String toString(boolean showAll, boolean hideSecond) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (!showAll && hideSecond && i == 1) {
                sb.append("HIDDEN");
            } else {
                sb.append(cards.get(i).toString());
            }
            if (i < cards.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    /**
     * 格式化手牌顯示 (含花色符號)
     */
    public String formatForDisplay() {
        StringBuilder sb = new StringBuilder(" [");
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            sb.append(card.getSuitSymbol()).append(card.getRank());
            if (i < cards.size() - 1) {
                sb.append(" ");
            }
        }
        sb.append("] ").append(bestValue()).append("點");
        return sb.toString();
    }
}
