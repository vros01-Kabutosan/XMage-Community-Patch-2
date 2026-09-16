package mage.player.ai;

import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.target.Target;
import mage.util.CachingObject;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced AI Target Comparator with Contextual Objective Evaluation
 * Implements context-based target selection and value comparison with caching
 */
public class PossibleTargetsComparator implements Comparator<Target>, CachingObject {
    
    public static final Comparator<Target> BY_CONTEXTUAL_VALUE = new PossibleTargetsComparator();
    public static final Comparator<Target> SACRIFICE_LEAST_VALUABLE = new PossibleTargetsComparator()
        .withPriority("least_valuable");
    
    /**
     * Contextual target evaluation based on abilities and board state
     */
    @Override
    public int compare(Target target1, Target target2) {
        if (target1 == null || target2 == null) return 0;
        
        Game game = target1.getGame();
        if (game == null) return 0;
        
        // Get contextual scores for each target
        int score1 = evaluateTargetContextual(target1, game);
        int score2 = evaluateTargetContextual(target2, game);
        
        return Integer.compare(score2, score1);  // Higher score first
    }

    private int evaluateTargetContextual(Target target, Game game) {
        if (target == null || target.getGame() == null) return 0;
        
        Permanent permanent = target instanceof Permanent ? 
            (Permanent) target : target.getCard();
            
        if (permanent != null) {
            // Contextual value based on abilities and board state
            int powerPlusToughness = permanent.getPower().getValue() + 
                                     permanent.getToughness().getValue();
            
            // Trample: Contextual bonus based on blocker count
            boolean hasTrample = permanent.hasAbility(trampleAbility);
            if (hasTrample) {
                int blockerCount = countDefensiveCreatures(game.getOpponent(
                    target.getOwner(), true, game));
                
                if (blockerCount < 2 && powerPlusToughness >= 5) {
                    return powerPlusToughness * 10 + 40;
                } else if (blockerCount < 4) {
                    return powerPlusToughness * 8 + 25;
                } else {
                    return powerPlusToughness * 6 + 10;
                }
            }
            
            // Deathtouch: Contextual penalty based on evasion abilities
            boolean hasDeathtouch = permanent.hasAbility(deathtouchAbility);
            if (hasDeathtouch) {
                int evasionAbilities = countOpponentEvasionAbilities(game.getOpponent(
                    target.getOwner(), true, game));
                
                if (evasionAbilities == 0) {
                    return Math.min(45, powerPlusToughness * 3);
                } else if (evasionAbilities < 2) {
                    return Math.max(-5, powerPlusToughness - evasionAbilities * 10);
                } else {
                    return -(evasionAbilities + Math.min(10, evasionAbilities));
                }
            }
            
            // Flying: Contextual bonus based on anti-air capabilities
            boolean hasFlying = permanent.hasAbility(flyingAbility);
            if (hasFlying) {
                int antiAirCount = countOpponentAntiAir(game.getOpponent(
                    target.getOwner(), true, game));
                
                return powerPlusToughness * 5 + Math.max(-10, 15 - antiAirCount * 10);
            }
            
            // Base score with ability bonuses
            return powerPlusToughness * 5 + permanent.getAbilities().size() * 2;
        }
        
        return 0;
    }

    private int countDefensiveCreatures(Object[] creatures) {
        if (creatures == null || creatures.length == 0) return 0;
        
        int defensiveCount = 0;
        for (Object creature : creatures) {
            if (creature instanceof Permanent permanent && 
                permanent.canBlockAny(null, null)) {
                if (!permanent.hasAbility(indestructibleAbility) || 
                    permanent.getPower().getValue() >= 3) {
                    defensiveCount++;
                } else if (permanent.hasAbility(indestructibleAbility)) {
                    defensiveCount += 2;
                }
            }
        }
        
        return defensiveCount;
    }

    private int countOpponentEvasionAbilities(Object[] creatures) {
        if (creatures == null || creatures.length == 0) return 0;
        
        int evasionAbilities = 0;
        for (Object creature : creatures) {
            if (creature instanceof Permanent permanent && 
                (permanent.hasAbility(TrampleAbility.getInstance()) ||
                 permanent.hasAbility(FlyingAbility.getInstance()) ||
                 permanent.hasAbility(HasteAbility.getInstance()))) {
                evasionAbilities++;
            }
        }
        
        return evasionAbilities;
    }

    private int countOpponentAntiAir(Object[] creatures) {
        if (creatures == null || creatures.length == 0) return 0;
        
        int antiAirCount = 0;
        for (Object creature : creatures) {
            if (creature instanceof Permanent permanent && 
                (permanent.hasAbility(FlyingAbility.getInstance()) || 
                 permanent.hasAbility(HasteAbility.getInstance()))) {
                antiAirCount++;
            }
        }
        
        return antiAirCount;
    }

}
