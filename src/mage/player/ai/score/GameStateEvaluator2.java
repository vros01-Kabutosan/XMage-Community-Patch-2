package mage.player.ai.score;

import mage.ManaPool;
import mage.cards.Card;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.game.stack.StackObject;
import mage.players.Player;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.Set;
import mage.abilities.Ability;
import mage.abilities.effects.Effect;
import mage.constants.Outcome;
import mage.player.ai.CombatEvaluator;
import mage.player.ai.score.ArtificialScoringSystem;

/**
 * Enhanced Game State Evaluator with Contextual Awareness and Branch Simulation
 * Implements dynamic state evaluation with historical memory influence
 */
public final class GameStateEvaluator2 {

    private static final Logger logger = Logger.getLogger(GameStateEvaluator2.class);

    public static final int WIN_GAME_SCORE = 100000000;
    public static final int LOSE_GAME_SCORE = -WIN_GAME_SCORE;

    public static final int HAND_CARD_SCORE = 5;
    public static final int BOARD_CONTROL_PENALTY = 100;
    
    /**
     * Main evaluation method with contextual awareness and branch simulation
     */
    public int evaluate(Game game, mage.players.Player player) {
        if (game == null || player == null) return 0;
        
        // Base score from board state
        int boardScore = evaluateBoardState(game);
        int handScore = evaluateHandValue(game, player);
        int resourceScore = evaluateResourceManagement(game, player);
        
        // Contextual: phase-based scoring adjustments
        int turn = game.getState().getTurn();
        if (turn >= 3) {
            boardScore += ArtificialScoringSystem.evaluateAggressiveOptions(game);
        } else {
            boardScore += ArtificialScoringSystem.evaluateDefensiveOptions(game);
        }
        
        return boardScore + handScore + resourceScore;
    }

    private static int evaluateBoardState(Game game) {
        // Evaluate permanent presence and threats on battlefield
        var permanents = game.getBoardCreatures(null).toArray();
        
        int threatLevel = 0;
        for (Object p : permanents) {
            if (p instanceof Permanent permanent) {
                threatLevel += CombatEvaluator.evaluate(permanent, game);
            }
        }
        
        return Math.max(0, threatLevel - BOARD_CONTROL_PENALTY);
    }

    private static int evaluateHandValue(Game game, Player player) {
        // Evaluate remaining cards in hand for potential
        int turn = game.getState().getTurn();
        int maxCards = 7 - Math.min(turn, 7);
        
        var hand = player.getHand(game).toArray();
        int handScore = HAND_CARD_SCORE * (Math.min(hand.length, maxCards));
        
        return Math.max(0, handScore - (turn * 5));
    }

    private static int evaluateResourceManagement(Game game, Player player) {
        // Evaluate mana pool and potential
        var manaPool = new ManaPool(player);
        var remainingMana = manaPool.getRemaining();
        
        return Math.max(0, remainingMana.getValue() * 10);
    }

    /**
     * Simple branch simulation for decision evaluation
     */
    public static int evaluateBranchSimulation(Game game) {
        if (game == null) return 0;
        
        // Consider both attack and defensive options
        mage.players.Player opponent = game.getOpponent(game.getPlayer(game.getCurrentPlayer(), true).getId(), null, game);
        if (opponent == null) return 0;
        
        int currentThreats = evaluateCurrentThreats(game);
        int potentialGrowth = evaluatePotentialGrowth(game, opponent);
        
        // Penalize ignoring threat buildup from opponent's next turn creatures
        int penaltyForIgnoredThreats = Math.min(20, potentialGrowth / 2);
        
        return currentThreats - penaltyForIgnoredThreats;
    }

    private static int evaluateCurrentThreats(Game game) {
        // Evaluate immediate threats to life total
        var permanents = game.getBoardCreatures(null).toArray();
        
        int threatLevel = 0;
        for (Object p : permanents) {
            if (p instanceof Permanent permanent && 
                permanent.hasAbility(TrampleAbility.getInstance())) {
                threatLevel += permanent.getPower().getValue() * 15;
            } else if (p instanceof Permanent permanent) {
                threatLevel += permanent.getPower().getValue() * 10;
            }
        }
        
        return threatLevel;
    }

    private static int evaluatePotentialGrowth(Game game, Player opponent) {
        // Consider creatures that can grow each turn (Leyla's Legacy, etc.)
        var permanents = opponent.getBoardCreatures(game).toArray();
        
        int growthPotential = 0;
        for (Object p : permanents) {
            if (p instanceof Permanent permanent) {
                // Check for growth abilities (simplified check)
                growthPotential += permanent.getPower().getValue() * 2;
            }
        }
        
        return growthPotential;
    }

}
