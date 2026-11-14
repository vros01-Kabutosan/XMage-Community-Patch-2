package mage.cards.j;

import java.util.UUID;

import mage.abilities.LoyaltyAbility;
import mage.abilities.effects.common.continuous.ActivateAbilitiesAnyTimeYouCouldCastInstantEffect;
import mage.abilities.effects.keyword.EmpowerJaceEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.SubType;
import mage.filter.common.FilterControlledPermanent;

/**
 * @author Hermes Agent
 */
public final class JacesMachinations extends CardImpl {

    private static final FilterControlledPermanent JACE_PLANESWALKER =
            new FilterControlledPermanent(SubType.JACE, "Jace planeswalker you control");

    static {
        JACE_PLANESWALKER.add(CardType.PLANESWALKER.getPredicate());
    }

    public JacesMachinations(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{2}{U}");

        // Until end of turn, you may activate loyalty abilities of Jace planeswalkers you control
        // on any player's turn any time you could cast an instant.
        ActivateAbilitiesAnyTimeYouCouldCastInstantEffect activateEffect =
                new ActivateAbilitiesAnyTimeYouCouldCastInstantEffect(
                        LoyaltyAbility.class, "loyalty abilities of Jace planeswalkers you control",
                        Duration.EndOfTurn, JACE_PLANESWALKER);
        activateEffect.setText("Until end of turn, you may activate loyalty abilities of Jace planeswalkers "
                + "you control on any player's turn any time you could cast an instant");
        this.getSpellAbility().addEffect(activateEffect);

        // Empower Jace 8.
        this.getSpellAbility().addEffect(new EmpowerJaceEffect(8).concatBy("<br>"));
    }

    private JacesMachinations(final JacesMachinations card) {
        super(card);
    }

    @Override
    public JacesMachinations copy() {
        return new JacesMachinations(this);
    }
}
