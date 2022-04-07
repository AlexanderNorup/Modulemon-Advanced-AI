package dk.sdu.mmmi.modulemon.BattleSimulation;

import dk.sdu.mmmi.modulemon.CommonBattle.*;
import dk.sdu.mmmi.modulemon.CommonBattle.BattleEvents.*;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;
import java.util.Optional;

public class BattleSimulation implements IBattleSimulation {

    private BattleState battleState;

    private IBattleEvent nextEvent;
    private Runnable onNextEvent;

    //"Mocked" enemyControlSystem. Should be moved somewhere else and hidden behind an interface
    private IBattleAI AI;
    private IBattleMonsterProcessor monsterProcessor;

    @Override
    public void StartBattle(IBattleParticipant player, IBattleParticipant enemy) {

        if (player.getActiveMonster().getHitPoints()<=0 || enemy.getActiveMonster().getHitPoints()<=0){
            throw new RuntimeException("Active monsters should have at least 1 HP");
        }

        // Assign first turn
        IMonster firstMonster = monsterProcessor.whichMonsterStarts(player.getActiveMonster(), enemy.getActiveMonster());
        IBattleParticipant firstToTakeTurn = firstMonster == player.getActiveMonster() ? player : enemy;

        this.battleState = new BattleState(player, enemy);

        if (!firstToTakeTurn.isPlayerControlled()) {
            nextEvent = new InfoBattleEvent("enemy starts the battle");
            onNextEvent = () -> {
                battleState.setActiveParticipant(firstToTakeTurn);
                AI.doAction(this);
            };
        } else {
            nextEvent = new InfoBattleEvent("player starts the battle");
            onNextEvent = () -> {
                battleState.setActiveParticipant(firstToTakeTurn);
            };
        }

    }

    private void switchTurns() {
        if (battleState.isPlayersTurn()) {
            battleState.setActiveParticipant(battleState.getEnemy());
            AI.doAction(this);
        } else {
            battleState.setActiveParticipant(battleState.getPlayer());
        }

    }

    private String getActiveParticipantTitle() {
        return battleState.isPlayersTurn() ? "player" : "enemy";
    }

    @Override
    public IBattleState getState() {
        return this.battleState;
    }

    @Override
    public void doMove(IBattleParticipant battleParticipant, IMonsterMove move) {
        if (battleParticipant!=battleState.getActiveParticipant()) {
            throw new IllegalArgumentException("It is not that battle participants turn!");
        }
        if (!battleParticipant.getActiveMonster().getMoves().contains(move)) {
            throw new IllegalArgumentException("The battle participants active monster can't use the specified move");
        }

        IMonster source = battleParticipant.getActiveMonster();
        IMonster target;
        String participantTitle;
        String opposingParticipantTitle;
        IBattleParticipant opposingParticipant;

        if (battleParticipant.isPlayerControlled()) {
            target = battleState.getEnemy().getActiveMonster();
            participantTitle = "player";
            opposingParticipantTitle = "enemy";
            opposingParticipant = battleState.getEnemy();
        } else {
            target = battleState.getPlayer().getActiveMonster();
            participantTitle = "enemy";
            opposingParticipantTitle = "player";
            opposingParticipant = battleState.getPlayer();
        }

        int damage = monsterProcessor.calculateDamage(source, move, target);

        nextEvent = new MoveBattleEvent(participantTitle + " monster used " + move.getName() + " for " + damage + " damage", battleParticipant, move, damage);

        onNextEvent = () -> {
            int newHitPoints = target.getHitPoints()-damage;
            if (newHitPoints>0) {
                target.setHitPoints(newHitPoints);

                switchTurns();

            } else {
                target.setHitPoints(0);
                Optional<IMonster> nextMonster = opposingParticipant.getMonsterTeam().stream().filter(x -> x.getHitPoints() > 0).findFirst();

                if (nextMonster.isPresent()) {
                    nextEvent = new ChangeMonsterBattleEvent(opposingParticipantTitle + "s monster fainted... Changed to " + nextMonster.get().getName(), opposingParticipant, nextMonster.get());
                    onNextEvent = () -> {
                        opposingParticipant.setActiveMonster(nextMonster.get());
                        switchTurns();
                    };
                } else {
                    nextEvent = new VictoryBattleEvent(opposingParticipantTitle + "s monster fainted... " + participantTitle + " won the battle.", battleParticipant);
                }
            }
        };



    }

    @Override
    public void switchMonster(IBattleParticipant battleParticipant, IMonster monster) {
        if (battleState.getActiveParticipant()!=battleParticipant) {
            throw new IllegalArgumentException("It is not that battle participants turn!");
        }
        if (battleParticipant.getMonsterTeam().contains(monster)) {
            nextEvent = new ChangeMonsterBattleEvent(getActiveParticipantTitle()+" changed monster to " + monster.getName(), battleParticipant, monster);
            onNextEvent = () -> {
                battleParticipant.setActiveMonster(monster);
                switchTurns();
            };
        } else {
            throw new IllegalArgumentException("Can't change a players monster to a monster which is not in their team");
        }
    }

    @Override
    public void runAway(IBattleParticipant battleParticipant) {
        if (battleParticipant!=battleState.getActiveParticipant()) {
            throw new IllegalArgumentException("It is not that battle participants turn!");
        }

        //Here some randomness could be introduced, so the player can't always run away
        nextEvent = new RunAwayBattleEvent(getActiveParticipantTitle() + " ran away", battleParticipant);

    }

    @Override
    public IBattleState simulateDoMove(IBattleParticipant battleParticipant, IMonsterMove move, IBattleState currentState) {

        //Maybe add a check, that the battleParticipant is actually the active participant

        BattleState newState = (BattleState) currentState.clone();

        IMonster source = newState.getActiveParticipant().getActiveMonster();
        IMonster target;
        if (newState.isPlayersTurn()) {
            target = newState.getEnemy().getActiveMonster();
        } else {
            target = newState.getPlayer().getActiveMonster();
        }

        int damage = monsterProcessor.calculateDamage(source, move, target);
        target.setHitPoints(target.getHitPoints()-damage);

        return newState;
    }

    @Override
    public IBattleState simulateSwitchMonster(IBattleParticipant participant, IMonster monster, IBattleState currentState) {

        BattleState newState = (BattleState) currentState.clone();

        newState.getActiveParticipant().setActiveMonster(monster.clone());

        return newState;
    }

    @Override
    public IBattleEvent getNextBattleEvent() {
        IBattleEvent event = nextEvent;
        if (event!=null && onNextEvent!=null) {
            nextEvent = null;
            onNextEvent.run();
        }
        return event;
    }

    public void setMonsterProcessor(IBattleMonsterProcessor monsterProcessor) {
        this.monsterProcessor = monsterProcessor;
    }

    public void setAI(IBattleAI AI) {
        this.AI = AI;
    }

}