package mage.abilities.effects.common.continuous;

import java.util.UUID;

import mage.abilities.Ability;
import mage.abilities.effects.AsThoughEffectImpl;
import mage.constants.AsThoughEffectType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.filter.FilterPermanent;
import mage.game.Game;
import mage.game.permanent.Permanent;

/**
 * @author Styxo
 */
public class ActivateAbilitiesAnyTimeYouCouldCastInstantEffect extends AsThoughEffectImpl {

    private final Class activatedAbility;
    private final FilterPermanent filter;

    public ActivateAbilitiesAnyTimeYouCouldCastInstantEffect(Class activatedAbility, String activatedAbilityName) {
        this(activatedAbility, activatedAbilityName, Duration.EndOfGame, null);
    }

    /**
     * Same as {@link #ActivateAbilitiesAnyTimeYouCouldCastInstantEffect(Class, String)} but with a
     * configurable duration and an optional filter on the source permanent of the affected ability.
     * Used by "until end of turn, you may activate &lt;kind&gt; of &lt;filter&gt; any time you could
     * cast an instant" effects (e.g. Jace's Machinations).
     */
    public ActivateAbilitiesAnyTimeYouCouldCastInstantEffect(Class activatedAbility, String activatedAbilityName, Duration duration, FilterPermanent filter) {
        super(AsThoughEffectType.ACTIVATE_AS_INSTANT, duration, Outcome.Benefit);
        this.activatedAbility = activatedAbility;
        this.filter = filter;
        staticText = "You may activate " + activatedAbilityName + " any time you could cast an instant";
    }

    protected ActivateAbilitiesAnyTimeYouCouldCastInstantEffect(final ActivateAbilitiesAnyTimeYouCouldCastInstantEffect effect) {
        super(effect);
        this.activatedAbility = effect.activatedAbility;
        this.filter = effect.filter == null ? null : effect.filter.copy();
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return true;
    }

    @Override
    public ActivateAbilitiesAnyTimeYouCouldCastInstantEffect copy() {
        return new ActivateAbilitiesAnyTimeYouCouldCastInstantEffect(this);
    }

    @Override
    public boolean applies(UUID objectId, Ability affectedAbility, Ability source, Game game, UUID playerId) {
        if (!affectedAbility.isControlledBy(source.getControllerId())
                || !activatedAbility.isAssignableFrom(affectedAbility.getClass())) {
            return false;
        }
        if (filter != null) {
            Permanent permanent = game.getPermanent(affectedAbility.getSourceId());
            if (permanent == null || !filter.match(permanent, source.getControllerId(), source, game)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean applies(UUID objectId, Ability source, UUID affectedControllerId, Game game) {
        return false; // Not used
    }

}
