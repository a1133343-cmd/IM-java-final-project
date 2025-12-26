import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 牌堆類別 - 管理 52 張撲克牌
 */
public class Deck {
    private final List<Card> cards = new ArrayList<>();
    private int currentIndex = 0;

    public Deck() {
        String[] suits = {"H", "D", "C", "S"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        
        for (String suit : suits) {
            for (String rank : ranks) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    /**
     * 洗牌並重置索引
     */
    public void shuffle() {
        Collections.shuffle(cards);
        currentIndex = 0;
    }

    /**
     * 抽一張牌，牌用完則自動重洗
     */
    public Card draw() {
        if (currentIndex >= cards.size()) {
            shuffle();
        }
        return cards.get(currentIndex++);
    }
}
