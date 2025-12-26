/**
 * 撲克牌類別
 */
public class Card {
    private final String suit;   // H, D, C, S (花色)
    private final String rank;   // 2-10, J, Q, K, A

    public Card(String suit, String rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public String getSuit() {
        return suit;
    }

    public String getRank() {
        return rank;
    }

    /**
     * 取得牌面點數值
     * J, Q, K = 10; A = 11 (後續由 Hand 處理 Ace 彈性)
     */
    public int getValue() {
        if ("JQK".contains(rank)) {
            return 10;
        }
        if ("A".equals(rank)) {
            return 11;
        }
        return Integer.parseInt(rank);
    }

    @Override
    public String toString() {
        return rank + "," + suit;
    }

    /**
     * 取得花色符號
     */
    public String getSuitSymbol() {
        switch (suit) {
            case "H": return "♥";
            case "D": return "♦";
            case "C": return "♣";
            case "S": return "♠";
            default: return suit;
        }
    }
}
