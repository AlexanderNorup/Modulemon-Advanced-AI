package dk.sdu.mmmi.modulemon.MCTSBattleAI;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAI;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleSimulation;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleState;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.KnowledgeState;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

public class MCTSBattleAI implements IBattleAI {

    private KnowledgeState knowledgeState;
    private IBattleParticipant participantToControl;
    private IBattleParticipant opposingParticipant;
    private IBattleSimulation battleSimulation;
    private long startTime;
    private int defaultTimeLimitMs = 1000;
    private IGameSettings settings = null;
    private long timeLimit = defaultTimeLimitMs;
    private final int MAX_SIMULATE_DEPTH = 5;

    private final float EXPLORATION_COEFFICIENT = (float) (1.0 / Math.sqrt(2));

    public MCTSBattleAI(IBattleSimulation battleSimulation, IBattleParticipant participantToControl, IGameSettings settings) {
        knowledgeState = new KnowledgeState();
        this.participantToControl = participantToControl;
        this.opposingParticipant = participantToControl == battleSimulation.getState().getPlayer()
                ? battleSimulation.getState().getEnemy()
                : battleSimulation.getState().getPlayer();
        this.battleSimulation = battleSimulation;
        this.settings = settings;
        this.timeLimit = getTimeLimitms();
    }

    public boolean outOfTime() {
        return ((System.nanoTime() - startTime) / 1000000) >= this.timeLimit;
    }

    private long getTimeLimitms() {
        if (settings == null) {
            return defaultTimeLimitMs;
        }
        Object limitObj = settings.getSetting(SettingsRegistry.getInstance().getAIProcessingTimeSetting());
        if (!(limitObj instanceof Integer)) {
            return defaultTimeLimitMs;
        }
        System.out.println("Setting time limit to: " + (int) limitObj);
        return (int) limitObj;
    }

    private int numSimulatedActions = 0;

    @Override
    public void doAction() {
        System.out.println("Starting action finding (time limit: " + this.timeLimit + ")");
        numSimulatedActions = 0;

        // Update state, should the enemy have changed their monster
        if (!knowledgeState.getEnemyMonsters().contains(opposingParticipant.getActiveMonster())) {
            knowledgeState.getEnemyMonsters().add(opposingParticipant.getActiveMonster());
        }

        startTime = System.nanoTime();
        var rootNode = new Node(battleSimulation.getState().clone(), this.participantToControl);
        while (!outOfTime()) {
            var newNode = treePolicy(rootNode);
            var reward = defaultPolicy(newNode);
            backpropagation(newNode, reward);
        }

        var bestChild = bestChild(rootNode, 0);

        System.out.println(explainBestChild(bestChild));

        if (bestChild.getParentMove() != null) {
            battleSimulation.doMove(participantToControl, bestChild.getParentMove());
        } else if (bestChild.getParentSwitch() != null) {
            battleSimulation.switchMonster(participantToControl, bestChild.getParentSwitch());
        } else {
            throw new IllegalStateException("AI found no moves to do");
        }
    }

    private String explainBestChild(Node bestChild) {
        StringBuilder stringBuilder = new StringBuilder();
        String action = bestChild.getParentMove() != null ?
                "using " + bestChild.getParentMove().getName()
                : "switching to " + bestChild.getParentSwitch();
        stringBuilder.append("MCTS is ").append(action).append(" because it sees the following best scenario: (Simulated ").append(this.numSimulatedActions).append(" actions)").append('\n');

        var currentlyExpanding = bestChild;
        var isMCTS = false;
        int turnCount = 1;
        do {
            var children = currentlyExpanding.getChildren();
            if (children == null || children.isEmpty()) {
                break;
            }

            var bestChildOfCurrentlyExpanding = children.stream().max((x, y) -> (int) x.getReward()).get();
            var user = isMCTS ? "MCTS" : "The player";

            var bestChildAction = bestChildOfCurrentlyExpanding.getParentMove() != null ?
                    " uses " + bestChildOfCurrentlyExpanding.getParentMove().getName()
                    : " switches to " + bestChildOfCurrentlyExpanding.getParentSwitch();
            stringBuilder.append("- Turn ")
                    .append(turnCount)
                    .append(": ")
                    .append(user)
                    .append(bestChildAction)
                    .append(" (Reward: ")
                    .append(bestChildOfCurrentlyExpanding.getReward())
                    .append(")");

            if(isTerminal(currentlyExpanding.getState())){
                stringBuilder.append(" [TERMINAL STATE]");
            }

            stringBuilder.append('\n');

            isMCTS = !isMCTS;
            turnCount++;
            currentlyExpanding = bestChildOfCurrentlyExpanding;
        } while (currentlyExpanding != null);

        return stringBuilder.toString();
    }

    private void backpropagation(Node node, float reward) {
        if (Float.isNaN(reward)) {
            throw new IllegalArgumentException("Reward must be a number");
        }
        do {
            node.incrementTimesVisited();
            node.setReward(node.getReward() + reward);
            node = node.getParent();
        } while (node != null);
    }

    private float defaultPolicy(Node node) {
        var state = node.getState().clone();
        var depth = 0;

        IBattleParticipant participant1 = participantToControl;
        IBattleParticipant participant2 = opposingParticipant;
        if (node.getParticipant().equals(opposingParticipant)) {
            participant1 = opposingParticipant;
            participant2 = participantToControl;
        }

        while (!isTerminal(state) && depth < MAX_SIMULATE_DEPTH) {
            var action1  = chooseRandomAction(getParticipantFromState(state, participant1));
            state = simulateAction(participant1, action1, state);
            if (!isTerminal(state)) {
                // Need to check for terminal state here as well, since participant2 might have lost.
                var action2 = chooseRandomAction(getParticipantFromState(state, participant2));
                state = simulateAction(participant2, action2, state);
            }
            depth++;
        }

        var reward = getReward(state);

        if (reward > 0) {
            reward *= (1f / depth); // Making sure that the deeper, the worse reward
        }

        return reward;
    }

    private IBattleParticipant getParticipantFromState(IBattleState state, IBattleParticipant existingParticipant) {
        return state.getPlayer().equals(existingParticipant) ? state.getPlayer() : state.getEnemy();
    }

    private float getReward(IBattleState battleState) {
        IBattleParticipant participantToControl = battleState.getPlayer().equals(this.participantToControl)
                ? battleState.getPlayer()
                : battleState.getEnemy();

        IBattleParticipant opposingParticipant = battleState.getPlayer().equals(this.participantToControl)
                ? battleState.getEnemy()
                : battleState.getPlayer();

        int ownMonsterHPSum = 0;
        for (IMonster monster : participantToControl.getMonsterTeam()) {
            if (monster.getHitPoints() > 0) ownMonsterHPSum += monster.getHitPoints();
        }

        int enemyMonsterHPSum = 0;
        for (IMonster monster : opposingParticipant.getMonsterTeam()) {
            if (monster.getHitPoints() > 0) enemyMonsterHPSum += monster.getHitPoints();
        }

        var result = (float) ownMonsterHPSum / (ownMonsterHPSum + enemyMonsterHPSum);

        if (Float.isNaN(result)) {
            throw new IllegalStateException("Calculated reward is NaN");
        }

        // This will return 1 if all the enemy's monsters are dead, 0 if all the AI's monster
        // are dead, and a number in between otherwise, which will be higher if the AI's monsters
        // have a larger proportion of the hp of all the monsters in the battle
        return result;
    }

    private Object chooseRandomAction(IBattleParticipant participant) {
        var possibleMoves = participant.getActiveMonster().getMoves();
        var possibleSwitches = participant.getMonsterTeam().stream().filter(x -> x.getHitPoints() > 0 && !x.equals(participant.getActiveMonster())).toList();
        List<Object> possibleActions = Stream.concat(possibleMoves.stream(), possibleSwitches.stream()).toList();
        var rand = new Random();
        return possibleActions.get(rand.nextInt(possibleActions.size()));
    }

    private IBattleState simulateAction(IBattleParticipant actor, Object action, IBattleState currentState) {
        this.numSimulatedActions++;
        if (action instanceof IMonsterMove move) {
            return battleSimulation.simulateDoMove(actor, move, currentState);
        } else if (action instanceof IMonster monster) {
            return battleSimulation.simulateSwitchMonster(actor, monster, currentState);
        }
        throw new IllegalStateException("Cannot simulate when action is not an instance of IMonsterMove or IMonster");
    }

    private Node treePolicy(Node node) {
        while (!isTerminal(node.getState())) {
            if (!fullyExpanded(node)) {
                return expandNode(node);
            } else {
                node = bestChild(node, EXPLORATION_COEFFICIENT);
            }
        }
        return node;
    }

    private Node bestChild(Node node, float exploration_coefficient) {
        Node bestChild = null;
        var bestUCT = Float.NEGATIVE_INFINITY;

        for (Node child : node.getChildren()) {
            var uct = calculateUCT(child, exploration_coefficient);
            if (uct > bestUCT) {
                bestChild = child;
                bestUCT = uct;
            }
        }

        return bestChild;
    }

    private float calculateUCT(Node child, float exploration_coefficient) {
        var exploitation = child.getReward() / child.getTimesVisited();
        var exploration = Math.sqrt((2 * Math.log(child.getParent().getTimesVisited())) / child.getTimesVisited());
        return (float) (exploitation + exploration_coefficient * exploration);
    }

    private Node expandNode(Node node) {
        Object action = untriedAction(node);
        if (action instanceof Integer a && a == -1) {
            throw new IllegalStateException("No possible action was found when expanding node");
        }

        Node child;
        if (action instanceof IMonsterMove move) {
            child = new Node(
                    battleSimulation.simulateDoMove(node.getParticipant(), move, node.getState()),
                    node,
                    move);
        } else if (action instanceof IMonster monster) {
            child = new Node(
                    battleSimulation.simulateSwitchMonster(node.getParticipant(), monster, node.getState()),
                    node,
                    monster);
        } else {
            throw new IllegalStateException("No action was chosen when expanding node");
        }

        return child;
    }

    private Object untriedAction(Node node) {
        var possibleMoves = node.getParticipant().getActiveMonster().getMoves();
        var possibleSwitchActions = node.getParticipant().getMonsterTeam().stream()
                .filter(m -> m.getHitPoints() > 0 && m != node.getParticipant().getActiveMonster()).toList();
        List<Object> possibleActions = new ArrayList<Object>();
        // TODO: If the node.getParticipant() is not us, then check the knowlegde state

        for (IMonsterMove move : possibleMoves) {
            if (node.getChildren().stream().noneMatch(c -> c.getParentMove() == move)) {
                possibleActions.add(move);
            }
        }
        for (IMonster monster : possibleSwitchActions) {
            if (node.getChildren().stream().noneMatch(c -> c.getParentSwitch() == monster)) {
                possibleActions.add(monster);
            }
        }
        var possibleActionCount = possibleActions.size();
        if (possibleActionCount == 0) {
            return -1;
        } else {
            var rand = new Random();
            return possibleActions.get(rand.nextInt((int) possibleActionCount));
        }
    }

    private boolean fullyExpanded(Node node) {
        var moveCount = node.getParticipant().getActiveMonster().getMoves().size();
        var switchCount = node.getParticipant().getMonsterTeam().stream()
                .filter(m -> m.getHitPoints() > 0 && m != node.getParticipant().getActiveMonster())
                .count();

        return node.getChildren().size() >= (moveCount + switchCount);
    }

    private boolean isTerminal(IBattleState battleState) {
        IBattleParticipant thisParticipant = battleState.getPlayer();
        IBattleParticipant opposingParticipant = battleState.getEnemy();

        if (!thisParticipant.equals(this.participantToControl)) {
            opposingParticipant = battleState.getPlayer();
            thisParticipant = battleState.getEnemy();
        }

        // Check if all the AIs monsters are dead
        boolean allOwnMonstersDead = thisParticipant.getMonsterTeam().stream()
                .allMatch(x -> x.getHitPoints() <= 0);

        // Check if all the opposing participant's (known) monster are dead
        boolean allEnemyMonstersDead = opposingParticipant.getMonsterTeam().stream()
//                .filter(x -> knowledgeState.getEnemyMonsters().contains(x))  //only consider monsters we've seen
                .allMatch(x -> x.getHitPoints() <= 0);

        return allEnemyMonstersDead || allOwnMonstersDead;
    }

    @Override
    public void opposingMonsterUsedMove(IMonster monster, IMonsterMove move) {
        // If we don't know anything about the monsters moves yet, add an empty list
        if (!knowledgeState.getMonsterMoves().containsKey(monster)) {
            knowledgeState.getMonsterMoves().put(monster, new ArrayList<>());
        }
        // If this is the first time we see this monster using this move, add the move to
        // the list of known moves for that monster
        if (!knowledgeState.getMonsterMoves().get(monster).contains(move)) {
            knowledgeState.getMonsterMoves().get(monster).add(move);
        }
    }
}
