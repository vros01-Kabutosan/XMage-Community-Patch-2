package mage.player.ai.util;

import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.keyword.DoubleStrikeAbility;
import mage.abilities.keyword.InfectAbility;
import mage.counters.CounterType;
import mage.game.Game;
import mage.game.combat.Combat;
import mage.game.combat.CombatGroup;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.game.turn.CombatDamageStep;
import mage.game.turn.EndOfCombatStep;
import mage.game.turn.Step;
import mage.player.ai.score.GameStateEvaluator2;
import mage.player.ai.PermanentEvaluator;
import mage.players.Player;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base helper methods for combat.
 *
 * @author noxx
 */
public final class CombatUtil {

    private static final List<Permanent> emptyList = Collections.unmodifiableList(new ArrayList<>());

    private static final Logger log = Logger.getLogger(CombatUtil.class);

    private CombatUtil() {
    }

    public static List<Permanent> canKillOpponent(Game game, List<Permanent> attackersList, List<Permanent> blockersList,
                                                  Player defender) {
        if (game == null || attackersList == null || blockersList == null || defender == null) {
            return emptyList;
        }
        List<Permanent> validBlockers = new ArrayList<>(blockersList);
        validBlockers.removeIf(Objects::isNull);
        List<Permanent> blockableAttackers = new ArrayList<>(attackersList);
        blockableAttackers.removeIf(Objects::isNull);
        List<Permanent> unblockableAttackers = new ArrayList<>();
        for (Permanent attacker : attackersList) {
            if (!canBeBlocked(game, attacker, validBlockers)) {
                unblockableAttackers.add(attacker);
                blockableAttackers.remove(attacker);
            }
        }

        sortByPower(blockableAttackers, false); // most powerfull go to first

        // imagine that most powerful will be blocked as 1-vs-1
        List<Permanent> attackersThatWontBeBlocked = new ArrayList<>(blockableAttackers);
        for (int i = 0; (i < validBlockers.size() && i < blockableAttackers.size()); i++) {
            attackersThatWontBeBlocked.remove(blockableAttackers.get(i));
        }
        attackersThatWontBeBlocked.addAll(unblockableAttackers);

        // now count if it is possible to win the game by this attack using unblockable attackers and
        // those attackers that won't be blocked for sure (as player will block other creatures)
        if (sumDamage(attackersThatWontBeBlocked, defender) >= defender.getLife() && defender.isLifeTotalCanChange()
                && defender.canLose(game) && defender.getLife() > 0) {
            blockableAttackers.addAll(unblockableAttackers);
            return blockableAttackers;
        }

        if (sumPoisonDamage(attackersThatWontBeBlocked, defender) >= 10 - defender.getCountersCount(CounterType.POISON)) {
            blockableAttackers.addAll(unblockableAttackers);
            return blockableAttackers;
        }

        return emptyList;
    }

    /**
     * Checks that attacker can be blocked by any blocker from the list.
     *
     * @param game         Game
     * @param attacker     Attacker to check
     * @param blockersList Blockers to try to block the attacker with.
     * @return true if attacker can be blocked by any blocker
     */
    public static boolean canBeBlocked(Game game, Permanent attacker, List<Permanent> blockersList) {
        if (game == null || attacker == null || blockersList == null) {
            return false;
        }
        for (Permanent blocker : blockersList) {
            if (blocker == null) {
                continue;
            }
            try {
                if (blocker.canBlock(attacker.getId(), game)) {
                    return true;
                }
            } catch (RuntimeException ex) {
                // Ignore malformed candidates and continue checking legality.
            }
        }
        return false;
    }

    public static void sortByPower(List<Permanent> permanents, final boolean ascending) {
        Comparator<Permanent> order = Comparator.comparingInt(p -> p.getPower().getValue());
        if (!ascending) {
            order = order.reversed();
        }
        permanents.sort(order.thenComparing(p -> p.getId().toString()));
    }

    public static Permanent getWorstCreature(Game game, List<Permanent>... lists) {
        PermanentEvaluator evaluator = new PermanentEvaluator();
        Permanent worst = null;
        int worstScore = Integer.MAX_VALUE;
        int worstCombatBody = Integer.MAX_VALUE;
        for (List<Permanent> list : lists) {
            if (list == null) {
                continue;
            }
            for (Permanent permanent : list) {
                if (permanent == null) {
                    continue;
                }
                int score = evaluator.evaluate(permanent, game);
                int combatBody = permanent.getPower() == null || permanent.getToughness() == null
                        ? Integer.MIN_VALUE
                        : permanent.getPower().getValue() + permanent.getToughness().getValue();
                if (worst == null || score < worstScore
                        || (score == worstScore && combatBody < worstCombatBody)
                        || (score == worstScore && combatBody == worstCombatBody
                        && permanent.getId().compareTo(worst.getId()) < 0)) {
                    worst = permanent;
                    worstScore = score;
                    worstCombatBody = combatBody;
                }
            }
        }
        return worst;
    }

    private static int getAttackerPriorityScore(Permanent attacker) {
        if (attacker == null || attacker.getPower() == null) {
            return Integer.MIN_VALUE;
        }
        int power = Math.max(0, attacker.getPower().getValue());
        int multiplier = 1;
        if (attacker.getAbilities().contains(DoubleStrikeAbility.getInstance())) {
            multiplier++;
        }
        if (attacker.getAbilities().contains(InfectAbility.getInstance())) {
            multiplier++;
        }
        int evasion = attacker.getAbilities().getEvasionAbilities().size();
        int protection = attacker.getAbilities().getProtectionAbilities().size();
        return power * multiplier + evasion * 2 + protection * 2;
    }
    public static void removeWorstCreature(Permanent permanent, List<Permanent>... lists) {
        for (List<Permanent> list : lists) {
            if (!list.isEmpty()) {
                list.remove(permanent);
            }
        }
    }

    private static int sumDamage(List<Permanent> attackersThatWontBeBlocked, Player defender) {
        int damage = 0;
        for (Permanent attacker : attackersThatWontBeBlocked) {
            if (!attacker.getAbilities().contains(InfectAbility.getInstance())) {
                damage += attacker.getPower().getValue();
                if (attacker.getAbilities().contains(DoubleStrikeAbility.getInstance())) {
                    damage += attacker.getPower().getValue();
                }
            }
        }
        return damage;
    }

    private static int sumPoisonDamage(List<Permanent> attackersThatWontBeBlocked, Player defender) {
        int damage = 0;
        for (Permanent attacker : attackersThatWontBeBlocked) {
            if (attacker.getAbilities().contains(InfectAbility.getInstance())) {
                damage += attacker.getPower().getValue();
                if (attacker.getAbilities().contains(DoubleStrikeAbility.getInstance())) {
                    damage += attacker.getPower().getValue();
                }
            }
        }
        return damage;
    }

    public static void handleExalted() {
    }

    /**
     * Determines what blockers from the list can block the attacker
     *
     * @param game         Game
     * @param attacker     Attacker to check
     * @param blockersList Blockers to try to block the attacker with.
     * @return true if attacker can be blocked by any blocker
     */
    public static List<Permanent> getPossibleBlockers(Game game, Permanent attacker, List<Permanent> blockersList) {
        List<Permanent> canBlock = new ArrayList<>();
        if (game == null || attacker == null || blockersList == null) {
            return canBlock;
        }
        for (Permanent blocker : blockersList) {
            if (blocker == null) {
                continue;
            }
            try {
                if (blocker.canBlock(attacker.getId(), game)) {
                    canBlock.add(blocker);
                }
            } catch (RuntimeException ex) {
                // Ignore malformed candidates and keep evaluating the combat.
            }
        }
        return canBlock;
    }

    /**
     * AI related code, find better block combination for attackers
     */
    public static CombatInfo blockWithGoodTrade2(Game game, List<Permanent> attackers, List<Permanent> blockers) {
        if (game == null || game.getCombat() == null || attackers == null || blockers == null) {
            return new CombatInfo();
        }
        // Work on private, deterministic candidate lists. The caller may reuse
        // these collections while the combat decision is being evaluated.
        attackers = new ArrayList<>(attackers);
        blockers = new ArrayList<>(blockers);
        attackers.removeIf(Objects::isNull);
        blockers.removeIf(Objects::isNull);
        Comparator<Permanent> stableOrder = Comparator.comparing(
                permanent -> permanent == null || permanent.getId() == null
                        ? "" : permanent.getId().toString());
        Comparator<Permanent> attackerOrder = Comparator
                .comparingInt(CombatUtil::getAttackerPriorityScore)
                .reversed()
                .thenComparing(stableOrder);
        attackers.sort(attackerOrder);
        blockers.sort(stableOrder);

        UUID attackerId = game.getCombat().getAttackingPlayerId();
        if (attackerId == null || game.getCombat().getDefenders() == null
                || game.getCombat().getDefenders().isEmpty()) {
            return new CombatInfo();
        }
        UUID defenderId = game.getCombat().getDefenders().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(UUID::toString))
                .findFirst()
                .orElse(null);
        if (defenderId == null) {
            return new CombatInfo();
        }

        // TODO: implement full game simulations of all possible combinations (e.g. multiblockers support)

        CombatInfo combatInfo = new CombatInfo();
        for (Permanent attacker : attackers) {
            // simple combat simulation (1 vs 1)
            List<Permanent> allBlockers = getPossibleBlockers(game, attacker, blockers);
            List<SurviveInfo> blockerStats = getBlockersThatWillSurvive2(game, attackerId, defenderId, attacker, allBlockers);
            Map<Permanent, Integer> blockingDiffScore = new HashMap<>();
            Map<Permanent, Integer> nonBlockingDiffScore = new HashMap<>();
            blockerStats.forEach(s -> {
                blockingDiffScore.put(s.getBlocker(), s.getDiffBlockingScore());
                nonBlockingDiffScore.put(s.getBlocker(), s.getDiffNonblockingScore());
            });

            // split blockers by usage priority
            List<Permanent> survivedAndKillBlocker = new ArrayList<>();
            List<Permanent> survivedBlockers = new ArrayList<>();
            List<Permanent> diedBlockers = new ArrayList<>();
            blockerStats.forEach(stats -> {
                if (stats.isAttackerDied() && !stats.isBlockerDied()) {
                    survivedAndKillBlocker.add(stats.getBlocker());
                } else if (!stats.isBlockerDied()) {
                    survivedBlockers.add(stats.getBlocker());
                } else {
                    diedBlockers.add(stats.getBlocker());
                }
            });

            int blockedCount = 0;

            // find good blocker
            Permanent blocker = getWorstCreature(game, survivedAndKillBlocker, survivedBlockers);
            if (blocker != null) {
                combatInfo.addPair(attacker, blocker);
                removeWorstCreature(blocker, blockers, survivedAndKillBlocker, survivedBlockers);
                blockedCount++;

                // Add a second survivor when the pair can finish the attacker.
                if (!survivedAndKillBlocker.contains(blocker)) {
                    final Permanent chosenBlocker = blocker;
                    int combinedPower = chosenBlocker.getPower().getValue();
                    List<Permanent> supportCandidates = survivedBlockers.stream()
                            .filter(candidate -> candidate != chosenBlocker)
                            .filter(candidate -> combinedPower + candidate.getPower().getValue() >= attacker.getToughness().getValue())
                            .collect(Collectors.toList());
                    Permanent support = getWorstCreature(game, supportCandidates);
                    if (support != null) {
                        Boolean pairKillsAttacker = null;
                        try {
                            SurviveInfo pairInfo = simulateBlockerCombination(game, attackerId, defenderId, attacker, Arrays.asList(chosenBlocker, support));
                            pairKillsAttacker = pairInfo == null ? null : pairInfo.isAttackerDied();
                        } catch (RuntimeException ex) {
                            pairKillsAttacker = null;
                        }
                        if (pairKillsAttacker == null || pairKillsAttacker) {
                            combatInfo.addPair(attacker, support);
                            removeWorstCreature(support, blockers, survivedBlockers);
                            blockedCount++;
                        }
                    }
                }
            }

            // find good sacrifices (chump blocks also supported due bad game score on loose)
            // TODO: add chump blocking support here?
            // TODO: there are many triggers on damage, attack, etc - it can't be processed without real game simulations
            if (blocker == null) {
                blocker = getWorstCreature(game, diedBlockers);
                if (blocker != null) {
                    int diffBlockingScore = blockingDiffScore.getOrDefault(blocker, 0);
                    int diffNonBlockingScore = nonBlockingDiffScore.getOrDefault(blocker, 0);
                    if (diffBlockingScore >= 0 || diffBlockingScore > diffNonBlockingScore) {
                        // it's good - can sacrifice and get better game state, also protect from game loose
                        combatInfo.addPair(attacker, blocker);
                        removeWorstCreature(blocker, blockers, diedBlockers);
                        blockedCount++;
                    }
                }
            }

            // find blockers for restrictions
            while (true) {
                if (blockers.isEmpty()) {
                    break;
                }

                // TODO: add multiple use case support with min/max blockedBy conditional and other
                //   see all possible use cases in checkBlockRestrictions, checkBlockRequirementsAfter and checkBlockRestrictionsAfter

                // Respect a hard upper bound before adding another blocker.
                if (attacker.getMaxBlockedBy() > 0 && blockedCount >= attacker.getMaxBlockedBy()) {
                    break;
                }

                // effects support: can't be blocked except by xxx or more creatures
                if (blockedCount > 0 && attacker.getMinBlockedBy() > blockedCount) {
                    // it already has 1 blocker (killer in best use case), so no needs in second killer
                    blocker = getWorstCreature(game, survivedBlockers, survivedAndKillBlocker, diedBlockers);
                    if (blocker != null) {
                        combatInfo.addPair(attacker, blocker);
                        removeWorstCreature(blocker, blockers, survivedBlockers, survivedAndKillBlocker, diedBlockers);
                        blockedCount++;
                        continue; // try to find next required blocker
                    } else {
                        // invalid configuration, must stop
                        break;
                    }
                }

                // no more active restrictions
                break;
            }

            // no more blockers to use
            if (blockers.isEmpty()) {
                break;
            }
        }

        return combatInfo;
    }

    /**
     * Game simulations to find all survived/killer blocker
     */
    private static List<SurviveInfo> getBlockersThatWillSurvive2(Game game, UUID attackerId, UUID defenderId, Permanent attacker, List<Permanent> possibleBlockers) {
        List<SurviveInfo> res = new ArrayList<>();
        if (possibleBlockers == null) {
            return res;
        }
        for (Permanent blocker : possibleBlockers) {
            if (blocker == null) {
                continue;
            }
            // A broken simulation candidate must not abort the whole combat decision.
            SurviveInfo info;
            try {
                info = willItSurviveSimple(game, attackerId, defenderId, attacker, blocker);
            } catch (RuntimeException ex) {
                continue;
            }
            if (info == null) {
                continue;
            }
            info.setBlocker(blocker);
            res.add(info);
        }
        return res;
    }

    private static SurviveInfo simulateBlockerCombination(Game originalGame, UUID attackingPlayerId, UUID defendingPlayerId, Permanent attacker, List<Permanent> blockers) {
        if (originalGame == null || attacker == null || blockers == null || blockers.isEmpty()) { return null; }
        Game sim = originalGame.createSimulationForAI();
        if (sim.getPlayer(defendingPlayerId) == null) { return null; }
        List<Permanent> validBlockers = blockers.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (validBlockers.isEmpty()) { return null; }
        Combat combat = sim.getCombat();
        combat.setAttacker(attackingPlayerId);
        combat.setDefenders(sim);
        int startScore = GameStateEvaluator2.evaluate(defendingPlayerId, sim).getTotalScore();
        int simulationSteps = 0;
        for (Permanent blocker : validBlockers) {
            sim.getPlayer(defendingPlayerId).declareBlocker(defendingPlayerId, blocker.getId(), attacker.getId(), sim);
        }
        sim.fireEvent(GameEvent.getEvent(GameEvent.EventType.DECLARED_BLOCKERS, defendingPlayerId, defendingPlayerId));
        sim.checkStateAndTriggered();
        while (!sim.getStack().isEmpty()) {
            if (++simulationSteps > 1000) { return null; }
            sim.getStack().resolve(sim);
            sim.applyEffects();
        }
        sim.fireEvent(GameEvent.getEvent(GameEvent.EventType.DECLARE_BLOCKERS_STEP_POST, sim.getActivePlayerId(), sim.getActivePlayerId()));
        simulateStep(sim, new CombatDamageStep(true));
        simulateStep(sim, new CombatDamageStep(false));
        simulateStep(sim, new EndOfCombatStep());
        sim.checkStateAndTriggered();
        while (!sim.getStack().isEmpty()) {
            if (++simulationSteps > 1000) { return null; }
            sim.getStack().resolve(sim);
            sim.applyEffects();
        }
        int endBlockingScore = GameStateEvaluator2.evaluate(defendingPlayerId, sim).getTotalScore();
        Permanent firstBlocker = validBlockers.get(0);
        return new SurviveInfo(!sim.getBattlefield().containsPermanent(attacker.getId()),
                !sim.getBattlefield().containsPermanent(firstBlocker.getId()),
                endBlockingScore - startScore, 0);
    }
    public static SurviveInfo willItSurviveSimulation(Game originalGame, UUID attackingPlayerId, UUID defendingPlayerId, Permanent attacker, Permanent blocker) {
        Game sim = originalGame.createSimulationForAI();
        if (blocker == null || attacker == null || sim.getPlayer(defendingPlayerId) == null) {
            return null;
        }

        // TODO: need code research, possible bugs in miss prepare code due real combat logic
        // TODO: bugged, miss combat.clear code (possible bugs - wrong blocker declare by AI on multiple options?)
        Combat combat = sim.getCombat();
        combat.setAttacker(attackingPlayerId);
        combat.setDefenders(sim);
        int startScore = GameStateEvaluator2.evaluate(defendingPlayerId, sim).getTotalScore();
        int simulationSteps = 0;

        // real game simulation
        // TODO: need debug and testing, old code from 2012
        //   must have infinite/freeze protection (e.g. limit stack resolves)

        // declare
        sim.getPlayer(defendingPlayerId).declareBlocker(defendingPlayerId, blocker.getId(), attacker.getId(), sim);
        sim.fireEvent(GameEvent.getEvent(GameEvent.EventType.DECLARED_BLOCKERS, defendingPlayerId, defendingPlayerId));
        sim.checkStateAndTriggered();
        while (!sim.getStack().isEmpty()) {
            if (++simulationSteps > 1000) {
                return null;
            }
            sim.getStack().resolve(sim);
            sim.applyEffects();
        }
        sim.fireEvent(GameEvent.getEvent(GameEvent.EventType.DECLARE_BLOCKERS_STEP_POST, sim.getActivePlayerId(), sim.getActivePlayerId()));

        // combat
        simulateStep(sim, new CombatDamageStep(true));
        simulateStep(sim, new CombatDamageStep(false));
        simulateStep(sim, new EndOfCombatStep());

        // after
        sim.checkStateAndTriggered();
        while (!sim.getStack().isEmpty()) {
            if (++simulationSteps > 1000) {
                return null;
            }
            sim.getStack().resolve(sim);
            sim.applyEffects();
        }

        int endBlockingScore = GameStateEvaluator2.evaluate(defendingPlayerId, sim).getTotalScore();
        int endNonBlockingScore = startScore; // TODO: implement
        return new SurviveInfo(
                !sim.getBattlefield().containsPermanent(attacker.getId()),
                !sim.getBattlefield().containsPermanent(blocker.getId()),
                endBlockingScore - startScore,
                endNonBlockingScore - startScore
        );
    }

    public static SurviveInfo willItSurviveSimple(Game originalGame, UUID attackingPlayerId, UUID defendingPlayerId, Permanent attacker, Permanent blocker) {
        Game sim = originalGame.createSimulationForAI();
        if (blocker == null || attacker == null || sim.getPlayer(defendingPlayerId) == null) {
            return null;
        }

        Combat combat = sim.getCombat();
        combat.setAttacker(attackingPlayerId);
        combat.setDefenders(sim);

        Game simNonBlocking = sim.copy();

        // attacker tapped before attack, it will add additional score to blocker, but it must be ignored
        // so blocker will block same creature with same score without penalty
        int startScore = GameStateEvaluator2.evaluate(defendingPlayerId, sim, false).getTotalScore();

        // fake combat simulation (simple damage simulation)
        Permanent simAttacker = sim.getPermanent(attacker.getId());
        Permanent simBlocker = sim.getPermanent(blocker.getId());
        if (simAttacker == null || simBlocker == null) {
            throw new IllegalArgumentException("Broken sim game, can't find attacker or blocker");
        }
        // don't ask about that hacks - just replace to real combat simulation someday (another hack but with full stack resolve)
        // first damage step
        simulateCombatDamage(sim, simBlocker, simAttacker, true);
        simulateCombatDamage(sim, simAttacker, simBlocker, true);
        simAttacker.applyDamage(sim);
        simBlocker.applyDamage(sim);
        sim.checkStateAndTriggered();
        sim.processAction();
        // second damage step
        if (sim.getPermanent(simBlocker.getId()) != null && sim.getPermanent(simAttacker.getId()) != null) {
            simulateCombatDamage(sim, simBlocker, simAttacker, false);
            simulateCombatDamage(sim, simAttacker, simBlocker, false);
            simAttacker.applyDamage(sim);
            simBlocker.applyDamage(sim);
            sim.checkStateAndTriggered();
            sim.processAction();
        }

        /* old manual PT compare
        if (attacker.getPower().getValue() >= blocker.getToughness().getValue()) {
            sim.getBattlefield().removePermanent(blocker.getId());
        }
        if (attacker.getToughness().getValue() <= blocker.getPower().getValue()) {
            sim.getBattlefield().removePermanent(attacker.getId());
        }
         */

        // fake non-block simulation
        simNonBlocking.getPlayer(defendingPlayerId).damage(
                attacker.getPower().getValue(),
                attacker.getId(),
                null,
                simNonBlocking,
                true,
                true
        );
        simNonBlocking.checkStateAndTriggered();
        simNonBlocking.processAction();

        int endBlockingScore = GameStateEvaluator2.evaluate(defendingPlayerId, sim, false).getTotalScore();
        int endNonBlockingScore = GameStateEvaluator2.evaluate(defendingPlayerId, simNonBlocking, false).getTotalScore();
        return new SurviveInfo(
                !sim.getBattlefield().containsPermanent(attacker.getId()),
                !sim.getBattlefield().containsPermanent(blocker.getId()),
                endBlockingScore - startScore,
                endNonBlockingScore - startScore
        );
    }

    private static void simulateCombatDamage(Game sim, Permanent fromCreature, Permanent toCreature, boolean isFirstDamageStep) {
        Ability fakeAbility = new SimpleStaticAbility(null);
        if (CombatGroup.dealsDamageThisStep(fromCreature, isFirstDamageStep, sim)) {
            fakeAbility.setSourceId(fromCreature.getId());
            fakeAbility.setControllerId(fromCreature.getControllerId());
            toCreature.damage(fromCreature.getPower().getValue(), fromCreature.getId(), fakeAbility, sim, true, true);
        }
    }

    private static void simulateStep(Game sim, Step step) {
        sim.getPhase().setStep(step);
        if (!step.skipStep(sim, sim.getActivePlayerId())) {
            step.beginStep(sim, sim.getActivePlayerId());
            // The following commented out call produces random freezes.
            //game.checkStateAndTriggered();
            while (!sim.getStack().isEmpty()) {
                sim.getStack().resolve(sim);
                sim.applyEffects();
            }
            step.endStep(sim, sim.getActivePlayerId());
        }
    }
}
