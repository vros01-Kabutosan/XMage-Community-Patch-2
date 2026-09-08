package mage.player.ai.score;

import mage.abilities.Ability;
import mage.abilities.keyword.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: outdated, replace by edh or commander brackets ability score
 * @author nantuko
 */
public final class MagicAbility {

    private static final Map<String, Integer> scores;

    static {
        Map<String, Integer> values = new HashMap<>();
        values.put(DeathtouchAbility.getInstance().getRule(), 60);
        values.put(DefenderAbility.getInstance().getRule(), -100);
        values.put(DoubleStrikeAbility.getInstance().getRule(), 100);
        values.put(new ExaltedAbility().getRule(), 10);
        values.put(FirstStrikeAbility.getInstance().getRule(), 50);
        values.put(FlashAbility.getInstance().getRule(), 20);
        values.put(FlyingAbility.getInstance().getRule(), 50);
        values.put(new ForestwalkAbility().getRule(), 10);
        values.put(HasteAbility.getInstance().getRule(), 20);
        values.put(HexproofAbility.getInstance().getRule(), 80);
        values.put(IndestructibleAbility.getInstance().getRule(), 150);
        values.put(InfectAbility.getInstance().getRule(), 60);
        values.put(IntimidateAbility.getInstance().getRule(), 50);
        values.put(new IslandwalkAbility().getRule(), 10);
        values.put(LifelinkAbility.getInstance().getRule(), 35);
        values.put(new MountainwalkAbility().getRule(), 10);
        values.put(new MenaceAbility().getRule(), 40);
        values.put(new PlainswalkAbility().getRule(), 10);
        values.put(ReachAbility.getInstance().getRule(), 20);
        values.put(ShroudAbility.getInstance().getRule(), 60);
        values.put(new SwampwalkAbility().getRule(), 10);
        values.put(TrampleAbility.getInstance().getRule(), 30);
        values.put(new CantBeBlockedSourceAbility().getRule(), 100);
        values.put(VigilanceAbility.getInstance().getRule(), 20);
        values.put(WitherAbility.getInstance().getRule(), 30);
        // gatecrash
        values.put(new EvolveAbility().getRule(), 50);
        values.put(new ExtortAbility().getRule(), 30);
        scores = Collections.unmodifiableMap(values);
    }

    public static int getAbilityScore(Ability ability) {
        if (ability == null) {
            return 0;
        }
        Integer score = scores.get(ability.getRule());
        if (score == null) {
            //System.err.println("Couldn't find ability score: " + ability.getClass().getSimpleName() + " - " + ability.toString());
            //TODO: add handling protection from ..., levelup, kicker, etc. abilities
            return 2; // more abilities - more score in any use cases
        }
        return score;
    }
}
