
package mage.player.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import mage.abilities.keyword.DeathtouchAbility;
import mage.abilities.keyword.DoubleStrikeAbility;
import mage.abilities.keyword.FirstStrikeAbility;
import mage.abilities.keyword.IndestructibleAbility;
import mage.abilities.keyword.LifelinkAbility;
import mage.abilities.keyword.ReachAbility;
import mage.abilities.keyword.VigilanceAbility;
import mage.abilities.keyword.TrampleAbility;
import mage.game.Game;
import mage.game.permanent.Permanent;

/**
 *
 * @author BetaSteward_at_googlemail.com
 */
public class CombatEvaluator {

    //preserve calculations for efficiency
    private Map<UUID, Integer> values = new HashMap<>();
    private String lastGameStateValue;

    public int evaluate(Permanent creature, Game game) {
        String gameStateValue = game.getState().getValue(true);
        if (!gameStateValue.equals(lastGameStateValue)) {
            values.clear();
            lastGameStateValue = gameStateValue;
        }
        if (!values.containsKey(creature.getId())) {
            int value = 0;
            if (creature.canAttack(null, game)) {
                value += 2;
            }
            if (!creature.canBlockAny(game)) {
                value -= 2;
            }
            value += creature.getPower().getValue();
            value += creature.getToughness().getValue();
            value += creature.getAbilities().getEvasionAbilities().size();
            value += creature.getAbilities().getProtectionAbilities().size();
            value += creature.getAbilities().containsKey(FirstStrikeAbility.getInstance().getId()) ? 1 : 0;
            value += creature.getAbilities().containsKey(DoubleStrikeAbility.getInstance().getId()) ? 2 : 0;
            value += creature.getAbilities().containsKey(TrampleAbility.getInstance().getId()) ? 1 : 0;
            value += creature.getAbilities().containsKey(DeathtouchAbility.getInstance().getId()) ? 2 : 0;
            value += creature.getAbilities().containsKey(IndestructibleAbility.getInstance().getId()) ? 3 : 0;
            value += creature.getAbilities().containsKey(LifelinkAbility.getInstance().getId()) ? 1 : 0;
            value += creature.getAbilities().containsKey(VigilanceAbility.getInstance().getId()) ? 1 : 0;
            value += creature.getAbilities().containsKey(ReachAbility.getInstance().getId()) ? 1 : 0;
            values.put(creature.getId(), value);
        }
        return values.get(creature.getId());
    }

}
