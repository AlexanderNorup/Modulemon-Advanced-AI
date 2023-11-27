package dk.sdu.mmmi.modulemon.MCTSBattleAI;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAI;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleSimulation;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleState;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.KnowledgeState;
import dk.sdu.mmmi.modulemon.CommonMonster.EmptyMove;
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
    private final int MAX_SIMULATE_DEPTH = 20;

    private final float EXPLORATION_COEFFICIENT = (float) (1.0 / Math.sqrt(2));

    public MCTSBattleAI(IBattleSimulation battleSimulation, IBattleParticipant participantToControl, IGameSettings settings) {
        var enableKnowlegdeStates = (Boolean) settings.getSetting(SettingsRegistry.getInstance().getAIKnowlegdeStateEnabled());
        System.out.println(String.format("MCTS AI using knowledge states: %b", enableKnowlegdeStates));
        knowledgeState = new KnowledgeState(!enableKnowlegdeStates);
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
        System.out.printf("Starting action finding (time limit: %dms)%n", this.timeLimit);
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

        System.out.println(String.format("Expanded %d nodes, based on %d simulated actions in %dms",  rootNode.getTimesVisited(), this.numSimulatedActions, ((System.nanoTime() - startTime) / 1000000)));
        System.out.println(explainNodeOptions(rootNode));
//        System.out.println(explainBestChild(bestChild));

        if (bestChild.getParentMove() != null) {
            battleSimulation.doMove(participantToControl, bestChild.getParentMove());
        } else if (bestChild.getParentSwitch() != null) {
            battleSimulation.switchMonster(participantToControl, bestChild.getParentSwitch());
        } else {
            throw new IllegalStateException("AI found no moves to do");
        }
    }

    private String explainNodeOptions(Node rootNode) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("MCTS (").append(rootNode.getState().getPlayer().equals(this.participantToControl) ? "player" : "opponent").append(" side) has ").append(rootNode.getChildren().size()).append(" options. In decreasing order of reward they are:").append('\n');
        for (var node : rootNode.getChildren().stream().sorted((a, b) -> Float.compare(a.getReward(), b.getReward()) * -1).toArray()) {
            stringBuilder.append("- ").append(node.toString()).append('\n');
        }

        return stringBuilder.toString();
    }

    /**
     * Will iterate the best rewards under a given node.
     * A nice way to get an overview over what the best scenario (according to MCTS) below a certain Node is.
     * It's not suuper usefull, because it has to assume the player is playing in their favour, which they often arent.
     */
    private String explainBestChild(Node bestChild) {
        StringBuilder stringBuilder = new StringBuilder();
        String action = bestChild.getParentMove() != null ?
                "using " + bestChild.getParentMove().getName()
                : "switching to " + bestChild.getParentSwitch();
        stringBuilder.append("Looking forward from ").append(action).append(", it sees the following best scenario:").append('\n');

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

            if (isTerminal(currentlyExpanding.getState(), true)) {
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

        while (!isTerminal(state, false) && depth < MAX_SIMULATE_DEPTH) {
            var action1 = chooseRandomAction(getParticipantFromState(state, participant1));
            state = simulateAction(participant1, action1, state);
            if (!isTerminal(state, false)) {
                // Need to check for terminal state here as well, since participant2 might have lost.
                var action2 = chooseRandomAction(getParticipantFromState(state, participant2));
                state = simulateAction(participant2, action2, state);
            }
            depth++;
        }

        var reward = getReward(state);

        if (reward > 0) {
            reward *= (2f / (depth+1)); // Making sure that the deeper, the worse reward
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
            if(move instanceof EmptyMove){
                return battleSimulation.simulateDoMove(actor, null, currentState);
            }
            return battleSimulation.simulateDoMove(actor, move, currentState);
        } else if (action instanceof IMonster monster) {
            return battleSimulation.simulateSwitchMonster(actor, monster, currentState);
        }
        throw new IllegalStateException("Cannot simulate when action is not an instance of IMonsterMove or IMonster");
    }

    private Node treePolicy(Node node) {
        while (!isTerminal(node.getState(), true)) {
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
        var bestUCT = Float.MIN_NORMAL;

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
            if(move instanceof EmptyMove){
                child = new Node(
                        battleSimulation.simulateDoMove(node.getParticipant(), null, node.getState()),
                        node,
                        move);
            }else {
                child = new Node(
                        battleSimulation.simulateDoMove(node.getParticipant(), move, node.getState()),
                        node,
                        move);
            }
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
        var allActions = getAllActionsForNode(node);
        List<Object> possibleActions = new ArrayList<Object>();

        for (var action : allActions ) {
            if(action instanceof IMonsterMove move) {
                if (node.getChildren().stream().noneMatch(c -> c.getParentMove() == move)) {
                    possibleActions.add(move);
                }
            }else if(action instanceof IMonster switchMonster){
                if (node.getChildren().stream().noneMatch(c -> c.getParentSwitch() == switchMonster)) {
                    possibleActions.add(switchMonster);
                }
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

    public List<Object> getAllActionsForNode(Node node){
        var monster = node.getParticipant().getActiveMonster();
        var possibleMoves = monster.getMoves();
        var possibleSwitchActions = node.getParticipant().getMonsterTeam().stream()
                .filter(m -> m.getHitPoints() > 0 && m != node.getParticipant().getActiveMonster()).toList();

        boolean useKnowledgeState = !node.getParticipant().equals(this.participantToControl);

        // If useKnowledgeState is true, then filter away all moves that haven't been seen before.
        var actions =  Stream.concat(possibleMoves.stream().filter(x -> !useKnowledgeState || this.knowledgeState.hasSeenMove(monster, x)),
                possibleSwitchActions.stream().filter(x -> !useKnowledgeState || this.knowledgeState.hasSeenMonster(x))).toList();

        if(actions.isEmpty()){
            if(useKnowledgeState){
                actions = new ArrayList<>(1);
                actions.add(EmptyMove.getInstance());
            }else{
                throw new IllegalStateException(String.format("There were no actions for the node: %s", node));
            }
        }

        return actions;
    }

    private boolean fullyExpanded(Node node) {
        var numActions = getAllActionsForNode(node).size();
        return node.getChildren().size() >= numActions ;
    }

    private boolean isTerminal(IBattleState battleState, boolean useKnowlegdeState) {
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
                .filter(x -> !useKnowlegdeState || knowledgeState.getEnemyMonsters().contains(x))  //only consider monsters we've seen
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
