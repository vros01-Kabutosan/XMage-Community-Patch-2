package mage.player.ai.score;

import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.keyword.DeathtouchAbility;
import mage.abilities.keyword.FirstStrikeAbility;
import mage.abilities.keyword.FlyingAbility;
import mage.abilities.keyword.HasteAbility;
import mage.abilities.keyword.LifelinkAbility;
import mage.abilities.keyword.IndestructibleAbility;
import mage.abilities.keyword.VigilanceAbility;
import mage.abilities.keyword.TrampleAbility;
import mage.cards.Card;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.counters.CounterType;
import mage.game.Game;
import mage.game.permanent.Permanent;

import java.util.UUID;

/**
 * Enhanced Artificial Scoring System with Contextual Dynamic Evaluation
 * Implements context-aware ability scoring and threat assessment
 */
public final class ArtificialScoringSystem {

    public static final int WIN_GAME_SCORE = 100000000;
    public static final int LOSE_GAME_SCORE = -WIN_GAME_SCORE;
    public static final int LIFE_SCORES = 30;
    public static final int PERMANENT_SCORE = 500;
    
    /**
     * Contextual evaluation of Trample based on blocker count and evasion abilities
     */
    public static int evaluateTrampleContextual(Game game, Permanent attacker) {
        if (attacker == null || game == null) return 10;
        
        mage.players.Player opponent = game.getOpponent(attacker.getOwner(), true, game);
        if (opponent == null) return 10;
        
        var opponents = opponent.getBoardCreatures(game).toArray();
        int blockerCount = countDefensiveCreatures(opponents);
        
        // Fewer blockers = higher trample value
        if (blockerCount < 2) {
            return evaluateTrampleLowBlockers(attacker);
        } else if (blockerCount < 4) {
            return Math.max(-5, 15 - blockerCount * 5);
        } else {
            return -(Math.min(15, blockerCount - 2));
        }
    }
    
    private static int countDefensiveCreatures(Object[] opponents) {
        if (opponents == null || opponents.length == 0) return 0;
        
        int defensiveCount = 0;
        for (Object opponent : opponents) {
            if (opponent instanceof Permanent permanent && 
                permanent.canBlockAny(null, null)) {
                // Indestructible creatures are harder to remove
                if (!permanent.hasAbility(IndestructibleAbility.getInstance())) {
                    defensiveCount++;
                } else if (permanent.getPower().getValue() >= 3) {
                    defensiveCount += 2;
                }
            }
        }
        
        return defensiveCount;
    }

    /**
     * Contextual evaluation of Deathtouch based on opponent evasion capabilities
     */
    public static int evaluateDeathtouchContextual(Game game, Permanent attacker) {
        if (attacker == null || game == null) return 15;
        
        mage.players.Player opponent = game.getOpponent(attacker.getOwner(), true, game);
        if (opponent == null) return 15;
        
        var opponents = opponent.getBoardCreatures(game).toArray();
        int evasionAbilities = countOpponentEvasionAbilities(opponents);
        
        // Deathtouch less effective vs evasion creatures
        if (evasionAbilities == 0) {
            return Math.min(45, attacker.getPower().getValue() * 3);
        } else if (evasionAbilities < 2) {
            return Math.max(-5, 5 - evasionAbilities * 10);
        } else {
            return -(evasionAbilities + Math.min(10, evasionAbilities));
        }
    }

    private static int countOpponentEvasionAbilities(Object[] opponents) {
        if (opponents == null || opponents.length == 0) return 0;
        
        int evasionAbilities = 0;
        for (Object opponent : opponents) {
            if (opponent instanceof Permanent permanent && 
                (permanent.hasAbility(TrampleAbility.getInstance()) ||
                 permanent.hasAbility(FlyingAbility.getInstance()) ||
                 permanent.hasAbility(HasteAbility.getInstance()))) {
                evasionAbilities++;
            }
        }
        
        return evasionAbilities;
    }

    /**
     * Contextual evaluation of First Strike based on blocker capabilities and phase
     */
    public static int evaluateFirstStrikeContextual(Game game, Permanent attacker) {
        if (attacker == null || game == null) return 15;
        
        mage.players.Player opponent = game.getOpponent(attacker.getOwner(), true, game);
        if (opponent == null) return 15;
        
        int blockerCount = countDefensiveCreatures(opponent.getBoardCreatures(game).toArray());
        
        if (blockerCount < 2) {
            return attacker.getPower().getValue() > 3 ? 25 : 15;
        } else if (blockerCount < 4) {
            return Math.max(-5, 10 - blockerCount * 2);
        } else {
            return -(Math.min(10, blockerCount - 2));
        }
    }

    /**
     * Contextual evaluation of Haste based on early turns and blockers
     */
    public static int evaluateHasteContextual(Game game, Permanent attacker) {
        if (attacker == null || game == null) return 0;
        
        // More valuable in early turns with few blockers
        int turn = game.getState().getTurn();
        if (turn <= 3 && countDefensiveCreatures(attacker.getOwner() != null ? 
            game.getOpponent(attacker.getOwner(), true, game).getBoardCreatures(game).toArray() : 
            new Object[0]) < 2) {
            return attacker.getPower().getValue() > 4 ? 15 : 10;
        } else {
            return -Math.min(5, Math.max(-5, turn - 3));
        }
    }

    public static int getCardDefinitionScore(Game game, Card card) {
        if (card == null || card.getSubType() != CardType.CREATURE) {
            return card != null ? card.getSets().getManaCost() : 0;
        }
        
        Permanent permanent = new PermanentImpl(game).setCard(card);
        int powerPlusToughness = permanent.getPower().getValue() + 
                                 permanent.getToughness().getValue();
        
        int baseScore = (powerPlusToughness / 25) * PERMANENT_SCORE;
        
        // Add ability-based contextual bonus
        int abilityBonus = evaluateAbilityContextual(card, permanent);
        baseScore += Math.max(0, abilityBonus);
        
        return baseScore + Math.min(50, card.getManaCost() != null ? card.getManaCost().cost() : 0);
    }

    private static int evaluateAbilityContextual(Card card, Permanent permanent) {
        int bonus = 0;
        
        // Trample contextual evaluation
        if (card.hasAbility(TrampleAbility.getInstance())) {
            Game game = card.getOwner();
            if (game != null && card.getOwner() instanceof mage.players.Player owner) {
                Permanent attacker = new PermanentImpl(game).setCard(card);
                bonus += evaluateTrampleContextual(game, attacker);
            }
        }
        
        // Deathtouch contextual evaluation
        if (card.hasAbility(DeathtouchAbility.getInstance())) {
            Game game = card.getOwner();
            if (game != null && card.getOwner() instanceof mage.players.Player owner) {
                Permanent attacker = new PermanentImpl(game).setCard(card);
                bonus += evaluateDeathtouchContextual(game, attacker);
            }
        }
        
        // First Strike contextual evaluation
        if (card.hasAbility(FirstStrikeAbility.getInstance())) {
            Game game = card.getOwner();
            if (game != null && card.getOwner() instanceof mage.players.Player owner) {
                Permanent attacker = new PermanentImpl(game).setCard(card);
                bonus += evaluateFirstStrikeContextual(game, attacker);
            }
        }
        
        // Haste contextual evaluation
        if (card.hasAbility(HasteAbility.getInstance())) {
            Game game = card.getOwner();
            if (game != null && card.getOwner() instanceof mage.players.Player owner) {
                Permanent attacker = new PermanentImpl(game).setCard(card);
                bonus += evaluateHasteContextual(game, attacker);
            }
        }
        
        return bonus;
    }

}
