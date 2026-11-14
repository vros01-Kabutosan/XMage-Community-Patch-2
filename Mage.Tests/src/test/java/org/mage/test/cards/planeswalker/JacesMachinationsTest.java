package org.mage.test.cards.planeswalker;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Jace's Machinations (FRA 32): Instant {2}{U}
 * "Until end of turn, you may activate loyalty abilities of Jace planeswalkers you control
 * on any player's turn any time you could cast an instant. Empower Jace 8."
 */
public class JacesMachinationsTest extends CardTestPlayerBase {

    @Test
    public void canActivateJaceLoyaltyAbilityOnOpponentsTurn() {
        addCard(Zone.BATTLEFIELD, playerA, "Jace Beleren");
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, "Jace's Machinations");

        // Turn 2 belongs to playerB: playerA casts the instant and then activates the loyalty
        // ability of their Jace during the opponent's turn.
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerA, "Jace's Machinations");
        activateAbility(2, PhaseStep.POSTCOMBAT_MAIN, playerA, "+2: Each player draws a card");

        setStopAt(2, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        // +2 adds two loyalty counters (Jace Beleren starts with 3)
        assertCounterCount(playerA, "Jace Beleren", CounterType.LOYALTY, 5);
    }

    @Test
    public void empowerJace8CreatesTokenWithEightCounters() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, "Jace's Machinations");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Jace's Machinations");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Jace", 1);
        assertCounterCount(playerA, "Jace", CounterType.LOYALTY, 8);
    }
}
