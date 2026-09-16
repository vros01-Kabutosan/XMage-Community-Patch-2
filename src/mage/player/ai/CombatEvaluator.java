package mage.player.ai;

import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.keyword.*;
import mage.cards.Card;
import mage.constants.Outcome;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.player.ai.score.ArtificialScoringSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced Combat Evaluator with Contextual Combat Assessment
 * Implements dynamic combat scoring based on ability interactions and board state
 */
public class CombatEvaluator {
    
    public static final int WIN_COMBAT_SCORE = 5000;
    public static final int LOSE_COMBAT_SCORE = -WIN_COMBAT_SCORE;

    /**
     * Main combat evaluation method with contextual awareness
     */
    public int evaluate(Permanent attacker, Game game) {
        if (attacker == null || game == null) return 0;
        
        mage.players.Player owner = game.getPlayer(attacker.getOwner(), true);
        if (owner == null) return 0;
        
        // Contextual combat scoring based on board state and opponent capabilities
        int turn = game.getState().getTurn();
        
        // Base score from artificial scoring system
        int baseScore = ArtificialScoringSystem.getCardDefinitionScore(game, attacker instanceof Card ? (Card)attacker : null);
        
        // Evaluate trample contextually based on blockers and evasion
        if (attacker.hasAbility(TrampleAbility.getInstance())) {
            baseScore += ArtificialScoringSystem.evaluateTrampleContextual(game, attacker);
        }
        
        // Evaluate Deathtouch contextually based on opponent evasion
        if (attacker.hasAbility(DeathtouchAbility.getInstance())) {
            baseScore += ArtificialScoringSystem.evaluateDeathtouchContextual(game, attacker);
        }
        
        // Contextual: first strike bonus when few blockers available
        if (attacker.hasAbility(FirstStrikeAbility.getInstance())) {
            baseScore += ArtificialScoringSystem.evaluateFirstStrikeContextual(game, attacker);
        }
        
        // Contextual: haste bonus in early turns
        if (turn <= 3) {
            if (attacker.hasAbility(HasteAbility.getInstance())) {
                baseScore += ArtificialScoringSystem.evaluateHasteContextual(game, attacker);
            }
        }
        
        // Contextual: flying value based on anti-air capabilities
        int airSuperiority = evaluateAirSuperiority(game, owner);
        baseScore += Math.max(-10, airSuperiority / 2);
        
        return baseScore;
    }

    /**
     * Contextual evaluation of air superiority for flying creatures
     */
    private int evaluateAirSuperiority(Game game, mage.players.Player owner) {
        if (game == null || owner == null) return 0;
        
        var opponents = game.getOpponent(owner.getId(), null, game).getBoardCreatures(game).toArray();
        int antiAirCount = 0;
        
        for (Object creature : opponents) {
            if (creature instanceof mage.game.permanent.Permanent permanent && 
                (permanent.hasAbility(FlyingAbility.getInstance()) || 
                 permanent.hasAbility(HasteAbility.getInstance()))) {
                antiAirCount++;
            }
        }
        
        // Flying creatures have advantage when opponent lacks anti-air
        return Math.max(-antiAirCount * 10, antiAirCount > 0 ? -5 : 15);
    }

}
