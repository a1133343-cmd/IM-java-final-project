import java.util.HashMap;
import java.util.Map;

/**
 * 機會牌效果註冊表
 * 管理所有機會牌類型與其效果的對應關係
 */
public class FunctionCardEffectRegistry {

    private static final Map<FunctionCardType, FunctionCardEffect> effects = new HashMap<>();

    static {
        registerEffects();
    }

    /**
     * 註冊所有機會牌效果
     */
    private static void registerEffects() {
        effects.put(FunctionCardType.MAKE_A_DEAL, new MakeADealEffect());
        effects.put(FunctionCardType.FORCE_DRAW, new ForceDrawEffect());
        effects.put(FunctionCardType.HEAL_ONE_HP, new HealOneHpEffect());
    }

    /**
     * 根據機會牌類型取得對應效果
     * 
     * @param type 機會牌類型
     * @return 對應的效果實作，若不存在則回傳 null
     */
    public static FunctionCardEffect getEffect(FunctionCardType type) {
        return effects.get(type);
    }

    /**
     * 檢查指定類型是否需要選擇目標
     * 
     * @param type 機會牌類型
     * @return true 表示需要目標，false 表示不需要
     */
    public static boolean requiresTarget(FunctionCardType type) {
        FunctionCardEffect effect = effects.get(type);
        return effect != null && effect.requiresTarget();
    }
}
