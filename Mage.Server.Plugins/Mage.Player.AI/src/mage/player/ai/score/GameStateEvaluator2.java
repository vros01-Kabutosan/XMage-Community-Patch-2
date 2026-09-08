package mage.player.ai.score;

import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import org.apache.log4j.Logger;

import java.util.UUID;
import mage.abilities.Ability;
import mage.abilities.effects.Effect;
import mage.constants.Outcome;

/**
 * @author nantuko
 * <p>
 * This evaluator is only good for two player games
 */
public final class GameStateEvaluator2 {

    private static final Logger logger = Logger.getLogger(GameStateEvaluator2.class);

    public static final int WIN_GAME_SCORE = 100000000;
    public static final int LOSE_GAME_SCORE = -WIN_GAME_SCORE;

    public static final int HAND_CARD_SCORE = 5;

    private static UUID findMostThreateningOpponent(UUID playerId, Game game) {
        UUID selectedOpponent = null;
        int highestThreat = Integer.MIN_VALUE;
        for (UUID opponentId : game.getOpponents(playerId, false)) {
            Player opponent = game.getPlayer(opponentId);
            if (opponent == null) {
                continue;
            }
            int threat = ArtificialScoringSystem.getLifeScore(opponent.getLife())
                    + opponent.getHand().size() * HAND_CARD_SCORE;
            for (Permanent permanent : game.getBattlefield().getAllActivePermanents(opponentId)) {
                threat += evaluatePermanentSafely(permanent, game, true);
            }
            if (selectedOpponent == null
                    || threat > highestThreat
                    || (threat == highestThreat
                    && opponentId.toString().compareTo(selectedOpponent.toString()) < 0)) {
                selectedOpponent = opponentId;
                highestThreat = threat;
            }
        }
        return selectedOpponent;
    }

    public static PlayerEvaluateScore evaluate(UUID playerId, Game game) {
        return evaluate(playerId, game, true);
    }

    public static PlayerEvaluateScore evaluate(UUID playerId, Game game, boolean useCombatPermanentScore) {
        Player player = game.getPlayer(playerId);
        // must find all leaved opponents
        Player opponent = game.getPlayer(findMostThreateningOpponent(playerId, game));
        if (opponent == null) {
            return new PlayerEvaluateScore(playerId, WIN_GAME_SCORE);
        }

        if (game.checkIfGameIsOver()) {
            if (player.hasLost()
                    || opponent.hasWon()) {
                return new PlayerEvaluateScore(playerId, LOSE_GAME_SCORE);
            }
            if (opponent.hasLost()
                    || player.hasWon()) {
                return new PlayerEvaluateScore(playerId, WIN_GAME_SCORE);
            }
        }

        int playerLifeScore = 0;
        int opponentLifeScore = 0;
        if (player.getLife() <= 0) { // we don't want a tie
            playerLifeScore = ArtificialScoringSystem.LOSE_GAME_SCORE;
        } else if (opponent.getLife() <= 0) {
            playerLifeScore = ArtificialScoringSystem.WIN_GAME_SCORE;
        } else {
            playerLifeScore = ArtificialScoringSystem.getLifeScore(player.getLife());
            opponentLifeScore = ArtificialScoringSystem.getLifeScore(opponent.getLife()); // TODO: minus
        }

        int playerPermanentsScore = 0;
        int opponentPermanentsScore = 0;
        try {
            StringBuilder sbPlayer = new StringBuilder();
            StringBuilder sbOpponent = new StringBuilder();

            // add values of player
            for (Permanent permanent : game.getBattlefield().getAllActivePermanents(playerId)) {
                int onePermScore = evaluatePermanentSafely(permanent, game, useCombatPermanentScore);
                playerPermanentsScore += onePermScore;
                if (logger.isDebugEnabled()) {
                    sbPlayer.append(permanent.getName()).append('[').append(onePermScore).append("] ");
                }
            }
            if (logger.isDebugEnabled()) {
                sbPlayer.insert(0, playerPermanentsScore + " - ");
                sbPlayer.insert(0, "Player..: ");
                logger.debug(sbPlayer);
            }

            // add values of opponent
            // Add battlefield pressure from every active opponent in multiplayer games.
            for (UUID opponentId : game.getOpponents(playerId, true)) {
                for (Permanent permanent : game.getBattlefield().getAllActivePermanents(opponentId)) {
                int onePermScore = evaluatePermanentSafely(permanent, game, useCombatPermanentScore);
                opponentPermanentsScore += onePermScore;
                if (logger.isDebugEnabled()) {
                    sbOpponent.append(permanent.getName()).append('[').append(onePermScore).append("] ");
                }
            }
            }
            if (logger.isDebugEnabled()) {
                sbOpponent.insert(0, opponentPermanentsScore + " - ");
                sbOpponent.insert(0, "Opponent: ");
                logger.debug(sbOpponent);
            }
        } catch (Throwable t) {
            logger.warn("Unable to evaluate one or more battlefield permanents", t);
        }

        // TODO: add card evaluator like permanent evaluator
        // - same card on battlefield must score x2 compared to hand, so AI will want to play it;
        // - other zones must score cards same way, example: battlefield = x, hand = x * 0.1, graveyard = x * 0.5, exile = x * 0.3
        // - possible bug in wrong score: instant and sorcery on hand will be more valuable compared to other zones,
        //   so AI will keep it in hand. Possible fix: look at card type and apply zones multipliers due special
        //   table like:
        //   * battlefield needs in creatures and enchantments/auras;
        //   * hand needs in instants and sorceries
        //   * graveyard needs in anything after battlefield and hand;
        //   * exile needs in nothing;
        //   * commander zone needs in nothing;
        // - additional improve: use revealed data to score opponent's hand:
        //   * known card by card evaluator;
        //   * unknown card by max value (so AI will use reveal to make opponent's total score lower -- is it helps???)
        // Keep opponent hand information hidden: only its size is observable.
        // For our own hand, value cards by definition instead of treating all cards equally.
        int playerHandScore = evaluateOwnHand(player, game);
        int opponentHandScore = opponent.getHand().size() * HAND_CARD_SCORE;
        int playerGraveyardScore = evaluateGraveyard(player, game);
        int opponentGraveyardScore = 0;
        int playerRevealedScore = evaluateRevealedCards(playerId, game);
        int opponentRevealedScore = 0;
        // Public zones from every active opponent matter in multiplayer games.
        for (UUID opponentId : game.getOpponents(playerId, true)) {
            Player publicOpponent = game.getPlayer(opponentId);
            if (publicOpponent != null) {
                opponentGraveyardScore += evaluateGraveyard(publicOpponent, game);
                opponentRevealedScore += evaluateRevealedCards(opponentId, game);
            }
        }

        int score = (playerLifeScore - opponentLifeScore)
                + (playerPermanentsScore - opponentPermanentsScore)
                + (playerHandScore - opponentHandScore)
                + (playerGraveyardScore - opponentGraveyardScore)
                + (playerRevealedScore - opponentRevealedScore);
        logger.debug(score
                + " total Score (life:" + (playerLifeScore - opponentLifeScore)
                + " permanents:" + (playerPermanentsScore - opponentPermanentsScore)
                + " hand:" + (playerHandScore - opponentHandScore) + ')');
        return new PlayerEvaluateScore(
                playerId,
                playerLifeScore, playerHandScore, playerPermanentsScore,
                opponentLifeScore, opponentHandScore, opponentPermanentsScore,
                playerGraveyardScore, opponentGraveyardScore,
                playerRevealedScore, opponentRevealedScore);
    }

    private static int evaluateOwnHand(Player player, Game game) {
        int score = 0;
        for (mage.cards.Card card : player.getHand().getCards(game)) {
            int definitionScore = ArtificialScoringSystem.getCardDefinitionScore(game, card);
            // Hand cards are future resources, so keep them below battlefield
            // permanents while distinguishing playable quality.
            score += HAND_CARD_SCORE + Math.max(0, Math.min(30, definitionScore / 25));
        }
        return score;
    }

    /** Publicly revealed cards are useful information, but only as a small temporary signal. */
    private static int evaluateRevealedCards(UUID ownerId, Game game) {
        int score = 0;
        for (mage.cards.Cards revealedCards : game.getState().getRevealed().values()) {
            for (mage.cards.Card card : revealedCards.getCards(game)) {
                if (ownerId.equals(card.getOwnerId())) {
                    int definitionScore = ArtificialScoringSystem.getCardDefinitionScore(game, card);
                    score += Math.max(0, Math.min(6, definitionScore / 100));
                }
            }
        }
        return Math.min(24, score);
    }

    private static int evaluateGraveyard(Player player, Game game) {
        int score = 0;
        for (mage.cards.Card card : player.getGraveyard().getCards(game)) {
            int definitionScore = ArtificialScoringSystem.getCardDefinitionScore(game, card);
            score += Math.max(0, Math.min(12, definitionScore / 60));
        }
        return Math.min(60, score);
    }

    private static int evaluatePermanentSafely(Permanent permanent, Game game, boolean useCombatPermanentScore) {
        try {
            return evaluatePermanent(permanent, game, useCombatPermanentScore);
        } catch (Throwable t) {
            logger.warn("Unable to evaluate permanent " + (permanent == null ? "null" : permanent.getName()), t);
            return 0;
        }
    }

    public static int evaluatePermanent(Permanent permanent, Game game, boolean useCombatPermanentScore) {
        // prevent AI from attaching bad auras to its own permanents ex: Brainwash and Demonic Torment (no immediate penalty on the battlefield)
        int value = 0;
        if (!permanent.getAttachments().isEmpty()) {
            for (UUID attachmentId : permanent.getAttachments()) {
                Permanent attachment = game.getPermanent(attachmentId);
                for (Ability a : attachment.getAbilities(game)) {
                    for (Effect e : a.getEffects()) {
                        if (e.getOutcome().equals(Outcome.Detriment)
                                && attachment.getControllerId().equals(permanent.getControllerId())) {
                            value -= 1000;  // seems to work well ; -300 is not effective enough
                        }
                    }
                }
            }
        }
        value += ArtificialScoringSystem.getFixedPermanentScore(game, permanent);
        value += ArtificialScoringSystem.getDynamicPermanentScore(game, permanent);
        if (useCombatPermanentScore) {
            value += ArtificialScoringSystem.getCombatPermanentScore(game, permanent);
        }
        return value;
    }

    public static class PlayerEvaluateScore {

        private UUID playerId;
        private int playerLifeScore = 0;
        private int playerHandScore = 0;
        private int playerPermanentsScore = 0;
        private int playerGraveyardScore = 0;
        private int playerRevealedScore = 0;

        private int opponentLifeScore = 0;
        private int opponentHandScore = 0;
        private int opponentPermanentsScore = 0;
        private int opponentGraveyardScore = 0;
        private int opponentRevealedScore = 0;

        private int specialScore = 0; // special score (ignore all others, e.g. for win/lose game states)

        public PlayerEvaluateScore(UUID playerId, int specialScore) {
            this.playerId = playerId;
            this.specialScore = specialScore;
        }

        public PlayerEvaluateScore(UUID playerId,
                       int playerLifeScore, int playerHandScore, int playerPermanentsScore,
                       int opponentLifeScore, int opponentHandScore, int opponentPermanentsScore) {
            this(playerId, playerLifeScore, playerHandScore, playerPermanentsScore,
                    opponentLifeScore, opponentHandScore, opponentPermanentsScore, 0, 0, 0, 0);
        }

        public PlayerEvaluateScore(UUID playerId,
                       int playerLifeScore, int playerHandScore, int playerPermanentsScore,
                       int opponentLifeScore, int opponentHandScore, int opponentPermanentsScore,
                       int playerGraveyardScore, int opponentGraveyardScore) {
            this(playerId, playerLifeScore, playerHandScore, playerPermanentsScore,
                    opponentLifeScore, opponentHandScore, opponentPermanentsScore,
                    playerGraveyardScore, opponentGraveyardScore, 0, 0);
        }

        public PlayerEvaluateScore(UUID playerId,
                       int playerLifeScore, int playerHandScore, int playerPermanentsScore,
                       int opponentLifeScore, int opponentHandScore, int opponentPermanentsScore,
                       int playerGraveyardScore, int opponentGraveyardScore,
                       int playerRevealedScore, int opponentRevealedScore) {
            this.playerId = playerId;
            this.playerLifeScore = playerLifeScore;
            this.playerHandScore = playerHandScore;
            this.playerPermanentsScore = playerPermanentsScore;
            this.opponentLifeScore = opponentLifeScore;
            this.opponentHandScore = opponentHandScore;
            this.opponentPermanentsScore = opponentPermanentsScore;
            this.playerGraveyardScore = playerGraveyardScore;
            this.opponentGraveyardScore = opponentGraveyardScore;
            this.playerRevealedScore = playerRevealedScore;
            this.opponentRevealedScore = opponentRevealedScore;
        }

        public UUID getPlayerId() {
            return this.playerId;
        }

        public int getPlayerScore() {
            return playerLifeScore + playerHandScore + playerPermanentsScore + playerGraveyardScore + playerRevealedScore;
        }

        public int getOpponentScore() {
            return opponentLifeScore + opponentHandScore + opponentPermanentsScore + opponentGraveyardScore + opponentRevealedScore;
        }

        public int getTotalScore() {
            if (specialScore != 0) {
                return specialScore;
            } else {
                return getPlayerScore() - getOpponentScore();
            }
        }

        public int getPlayerLifeScore() {
            return playerLifeScore;
        }

        public int getPlayerHandScore() {
            return playerHandScore;
        }

        public int getPlayerPermanentsScore() {
            return playerPermanentsScore;
        }

        public String getPlayerInfoFull() {
            return "Life:" + playerLifeScore
                    + ", Hand:" + playerHandScore
                    + ", Perm:" + playerPermanentsScore;
        }

        public String getPlayerInfoShort() {
            return "L:" + playerLifeScore
                    + ",H:" + playerHandScore
                    + ",P:" + playerPermanentsScore;
        }

        public String getOpponentInfoFull() {
            return "Life:" + opponentLifeScore
                    + ", Hand:" + opponentHandScore
                    + ", Perm:" + opponentPermanentsScore;
        }

        public String getOpponentInfoShort() {
            return "L:" + opponentLifeScore
                    + ",H:" + opponentHandScore
                    + ",P:" + opponentPermanentsScore;
        }
    }
}
