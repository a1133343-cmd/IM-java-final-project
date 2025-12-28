/**
 * 機會牌類型列舉
 * 定義所有機會牌的種類與描述
 */
public enum FunctionCardType {
    MAKE_A_DEAL("做個交易", "與一位玩家互換手牌"),
    FORCE_DRAW("我叫你抽", "強迫所有玩家抽一張牌"),
    HEAL_ONE_HP("我喝一口", "回一滴血");

    private final String displayName;
    private final String description;

    FunctionCardType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 從協定字串解析機會牌類型
     */
    public static FunctionCardType fromString(String name) {
        for (FunctionCardType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
