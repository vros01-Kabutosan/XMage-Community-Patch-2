package mage.player.ai;

import mage.MageObject;
import mage.abilities.Ability;
import mage.cards.Card;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.player.ai.score.ArtificialScoringSystem;

import java.util.*;
import java.util.List;

/**
 * Computer Player AI with Contextual Scoring and Basic Historical Memory
 */
public class ComputerPlayer extends Player {
    
    private static final Logger logger = Logger.getLogger(ComputerPlayer.class);
    
    // Simple memory tracking for strategic adaptation (not full persistence)
    private Map<TurnKey, Boolean> recentDecisions = new HashMap<>();
    
    /**
     * Evaluate play option with contextual awareness and basic memory
     */
    public int evaluate(Game game, Option option) {
        if (game == null || option == null) return 0;
        
        // Contextual scoring based on board state
        int score = ArtificialScoringSystem.getCardDefinitionScore(game, null);
        
        // Add contextual bonuses from CombatEvaluator
        Permanent attacker = getAttacker(game, option);
        if (attacker != null) {
            score += CombatEvaluator.evaluate(attacker, game);
        }
        
        // Simple memory-based adaptation
        TurnKey turnKey = new TurnKey(game.getState().getTurn(), option.toString());
        int memoryScore = recentDecisions.containsKey(turnKey) ? 
            (recentDecisions.get(turnKey) ? 5 : -5) : 0;
        
        return score + memoryScore;
    }

    /**
     * Clear memory for fresh start between games/turns
     */
    public void clearMemory(Game game) {
        if (game == null) return;
        
        // Clear decisions from recent turns
        int turn = game.getState().getTurn();
        List<TurnKey> keysToRemove = new ArrayList<>(recentDecisions.keySet());
        
        for (TurnKey key : keysToRemove) {
            if (key.turn < turn - 5) {  // Keep last 5 turns of decisions
                recentDecisions.remove(key);
            }
        }
    }

    private Permanent getAttacker(Game game, Option option) {
        // Simplified implementation - actual logic would be more complex
        if (option instanceof Permanent permanent) return permanent;
        
        if (option.getCard() instanceof Card card && card.getSubType() == CardType.CREATURE) {
            return new PermanentImpl(game).setCard(card);
        }
        
        return null;
    }

}

class TurnKey {
    private int turn;
    private String option;
    
    public TurnKey(int turn, String option) {
        this.turn = turn;
        this.option = option;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TurnKey)) return false;
        TurnKey other = (TurnKey) o;
        return this.turn == other.turn && Objects.equals(this.option, other.option);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(turn, option);
    }
}
