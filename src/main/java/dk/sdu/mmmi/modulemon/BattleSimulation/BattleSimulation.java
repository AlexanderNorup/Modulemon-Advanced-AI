package dk.sdu.mmmi.modulemon.BattleSimulation;

import dk.sdu.mmmi.modulemon.CommonBattle.*;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.*;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.BattleEvents.*;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BattleSimulation implements IBattleSimulation {
    private BattleState battleState;
    private IBattleEvent nextEvent;
    private Runnable onNextEvent;
    private IBattleAI opponentAI;
    private IBattleAIFactory opponentAIFactory;
    private IBattleAI playerAI;
    private IBattleAIFactory playerAIFactory;
    private IBattleMonsterProcessor monsterProcessor;
    private ExecutorService AIExecutor = Executors.newFixedThreadPool(1);
    private boolean playerStarted;

    @Override
    public void StartBattle(IBattleParticipant player, IBattleParticipant enemy) {

        if (player.getActiveMonster().getHitPoints() <= 0) {
            for (IMonster monster : player.getMonsterTeam()) {
                if (monster.getHitPoints() > 0) {
                    player.setActiveMonster(monster);
                    break;
                }
            }
        }

        if (enemy.getActiveMonster().getHitPoints() <= 0) {
            for (IMonster monster : enemy.getMonsterTeam()) {
                if (monster.getHitPoints() > 0) {
                    enemy.setActiveMonster(monster);
                    break;
                }
            }
        }

        if (player.getActiveMonster().getHitPoints() <= 0 || enemy.getActiveMonster().getHitPoints() <= 0) {
            throw new RuntimeException("Active monsters should have at least 1 HP");
        }

        // Assign first turn
        IMonster firstMonster;
        if(this.playerAIFactory != null && player.getActiveMonster().getName().equals(enemy.getActiveMonster().getName())){
            Random rand = new Random();
            firstMonster = rand.nextFloat() > 0.5 ? player.getActiveMonster() : enemy.getActiveMonster();
        } else {
            firstMonster = monsterProcessor.whichMonsterStarts(player.getActiveMonster(), enemy.getActiveMonster());
        }
        IBattleParticipant firstToTakeTurn = firstMonster == player.getActiveMonster() ? player : enemy;
        this.playerStarted = firstMonster == player.getActiveMonster();

        this.battleState = new BattleState(player, enemy);
        this.battleState.setActiveParticipant(firstToTakeTurn);
        if (opponentAIFactory != null)
            this.opponentAI = opponentAIFactory.getBattleAI(this, enemy);

        if (!firstToTakeTurn.isPlayerControlled()) {
            nextEvent = new InfoBattleEvent("The opponent starts the battle", battleState.clone());
            onNextEvent = () -> {
                battleState.setActiveParticipant(firstToTakeTurn);
                if (getOpponentAI() != null) {
                    doOpponentMove();
                } else {
                    nextEvent = new InfoBattleEvent("Waiting for an AI module", battleState.clone());
                }
            };
        } else {
            nextEvent = new InfoBattleEvent("Player starts the battle", battleState.clone());
            onNextEvent = () -> {
                battleState.setActiveParticipant(firstToTakeTurn);
                doPlayerMove();
            };
        }
    }


    private void switchTurns() {
        if (battleState.isPlayersTurn()) {
            if (getOpponentAI() != null) {
                battleState.setActiveParticipant(battleState.getEnemy());
                doOpponentMove();
            } else {
                nextEvent = new InfoBattleEvent("Waiting for an AI module", battleState.clone());
            }
        } else {
            battleState.setActiveParticipant(battleState.getPlayer());
            doPlayerMove();
        }
    }

    private String getActiveParticipantTitle() {
        return battleState.isPlayersTurn() ? "Player" : "Enemy";
    }

    @Override
    public IBattleState getState() {
        return this.battleState;
    }

    @Override
    public synchronized void doMove(IBattleParticipant battleParticipant, IMonsterMove move) {
        if (monsterProcessor == null) {
            nextEvent = new VictoryBattleEvent("Monsters unloaded, it's a draw", battleParticipant, battleState.clone());
            onNextEvent = () -> {
            };
            return;
        }
        if (battleParticipant != battleState.getActiveParticipant()) {
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

        if (battleParticipant.equals(battleState.getPlayer())) {
            if (getOpponentAI() != null) {
                getOpponentAI().opposingMonsterUsedMove(source, move);
            }
        }

        if (battleParticipant.isPlayerControlled()) {
            target = battleState.getEnemy().getActiveMonster();
            participantTitle = "Player";
            opposingParticipantTitle = "Opponent";
            opposingParticipant = battleState.getEnemy();
        } else {
            target = battleState.getPlayer().getActiveMonster();
            participantTitle = "Opponent";
            opposingParticipantTitle = "Player";
            opposingParticipant = battleState.getPlayer();
        }

        int damage = monsterProcessor.calculateDamage(source, move, target);
        int newHitPoints = target.getHitPoints() - damage;
        target.setHitPoints(Math.max(newHitPoints, 0));

        nextEvent = new MoveBattleEvent(participantTitle + "'s " + source.getName() + " used " + move.getName() + " for " + damage + " damage!", battleParticipant, move, damage, battleState.clone());

        onNextEvent = () -> {
            if (newHitPoints > 0) {
                switchTurns();
            } else {
                Optional<IMonster> nextMonster = opposingParticipant.getMonsterTeam().stream().filter(x -> x.getHitPoints() > 0).findFirst();

                if (nextMonster.isPresent()) {
                    opposingParticipant.setActiveMonster(nextMonster.get());
                    nextEvent = new MonsterFaintChangeBattleEvent(opposingParticipantTitle + "s monster fainted... Changed to " + nextMonster.get().getName(), opposingParticipant, nextMonster.get(), battleState.clone());
                    onNextEvent = this::switchTurns;
                } else {
                    nextEvent = new VictoryBattleEvent(opposingParticipantTitle + "s monster fainted... " + participantTitle + " won the battle.", battleParticipant, battleState.clone());
                    onNextEvent = () -> {
                    };
                }
            }
        };
    }

    @Override
    public synchronized void switchMonster(IBattleParticipant battleParticipant, IMonster monster) {
        if (battleState.getActiveParticipant() != battleParticipant) {
            throw new IllegalArgumentException("It is not that battle participants turn!");
        }
        IBattleParticipant participant = battleState.getActiveParticipant();
        if (monster.getHitPoints() <= 0) throw new IllegalArgumentException("You can't change to a dead monster");
        if (participant.getMonsterTeam().contains(monster)) {
            participant.setActiveMonster(monster);
            nextEvent = new ChangeMonsterBattleEvent(getActiveParticipantTitle() + " changed monster to " + monster.getName(), participant, monster, battleState.clone());
            onNextEvent = this::switchTurns;
        } else {
            throw new IllegalArgumentException("Can't change a players monster to a monster which is not in their team");
        }
    }

    @Override
    public synchronized void runAway(IBattleParticipant battleParticipant) {
        if (battleParticipant != battleState.getActiveParticipant()) {
            throw new IllegalArgumentException("It is not that battle participants turn!");
        }

        //Here some randomness could be introduced, so the player can't always run away
        nextEvent = new RunAwayBattleEvent(getActiveParticipantTitle() + " ran away", battleParticipant, battleState.clone());

    }

    @Override
    public IBattleState simulateDoMove(IBattleParticipant battleParticipant, IMonsterMove move, IBattleState currentState) {

        BattleState newState = (BattleState) currentState.clone();
        if (!newState.getActiveParticipant().equals(battleParticipant)) {
            throw new IllegalStateException("Simulated move for battle participant, is not the active participant!");
        }

        IMonster source = newState.getActiveParticipant().getActiveMonster();
        IBattleParticipant opposingParticipant = newState.getPlayer().equals(newState.getActiveParticipant())
                ? newState.getEnemy() : newState.getPlayer();
        IMonster target = opposingParticipant.getActiveMonster();

        int damage = 0;
        if (move != null) {
            damage = monsterProcessor.calculateDamage(source, move, target);
        }

        int newHitPoints = target.getHitPoints() - damage;
        if (newHitPoints > 0) {
            target.setHitPoints(newHitPoints);
        } else {
            target.setHitPoints(0);
            Optional<IMonster> nextMonster = opposingParticipant.getMonsterTeam().stream().filter(x -> x.getHitPoints() > 0).findFirst();

            nextMonster.ifPresent(opposingParticipant::setActiveMonster);
        }

        switchTurns(newState);

        return newState;
    }

    @Override
    public IBattleState simulateSwitchMonster(IBattleParticipant participant, IMonster monster, IBattleState currentState) {
        BattleState newState = (BattleState) currentState.clone();
        if (!newState.getActiveParticipant().equals(participant)) {
            throw new IllegalStateException("Simulated switch for battle participant, is not the active participant!");
        }
        newState.getActiveParticipant().setActiveMonster(monster);

        switchTurns(newState);

        return newState;
    }

    private void switchTurns(BattleState state) {
        if (state.isPlayersTurn()) {
            state.setActiveParticipant(state.getEnemy());
        } else {
            state.setActiveParticipant(state.getPlayer());
        }
    }

    @Override
    public synchronized IBattleEvent getNextBattleEvent() {
        IBattleEvent event = nextEvent;
        if (event != null && onNextEvent != null) {
            nextEvent = null;
            onNextEvent.run();
        }
        return event;
    }

    @Override
    public boolean hasNextBattleEvent() {
        return nextEvent != null;
    }

    private IBattleAI getOpponentAI() {
        if (this.opponentAI == null) {
            if (this.opponentAIFactory == null) {
                return null;
            }
            this.opponentAI = this.opponentAIFactory.getBattleAI(this, battleState.getEnemy());
        }
        return this.opponentAI;
    }

    private void doOpponentMove() {
        AIExecutor.execute(() -> {
            try {
                getOpponentAI().doAction();
            } catch (Exception ex) {
                System.out.println(ex);
                ex.printStackTrace();
                nextEvent = new AICrashedEvent(String.format("The opponent %s controller has crashed!", this.opponentAIFactory), this.battleState.getActiveParticipant(), ex, battleState.clone());
                onNextEvent = this::switchTurns;
            }
        });
    }

    private IBattleAI getPlayerAI() {
        if (this.playerAI == null) {
            if (this.playerAIFactory == null) {
                return null;
            }
            this.playerAI = this.playerAIFactory.getBattleAI(this, battleState.getPlayer());
        }
        return this.playerAI;
    }

    private void doPlayerMove() {
        // If player controlled, do nothing. Otherwise, call the player AI
        if (getPlayerAI() != null) {
            AIExecutor.execute(() -> {
                try {
                    getPlayerAI().doAction();
                } catch (Exception ex) {
                    System.out.println(ex);
                    ex.printStackTrace();
                    nextEvent = new AICrashedEvent(String.format("The player %s controller has crashed!", this.playerAIFactory), this.battleState.getActiveParticipant(), ex, battleState.clone());
                    onNextEvent = this::switchTurns;
                }
            });
        }
    }

    public void setMonsterProcessor(IBattleMonsterProcessor monsterProcessor) {
        this.monsterProcessor = monsterProcessor;
    }

    public void removeMonsterProcessor(IBattleMonsterProcessor monsterProcessor) {
        this.monsterProcessor = null;
    }

    public void setOpponentAIFactory(IBattleAIFactory factory) {
        this.opponentAI = null;
        this.opponentAIFactory = factory;
    }

    public void setPlayerAIFactory(IBattleAIFactory factory) {
        this.playerAI = null;
        this.playerAIFactory = factory;
    }

    public IBattleAIFactory getOpponentAIFactory() {
        return opponentAIFactory;
    }

    public IBattleAIFactory getPlayerAIFactory() {
        return playerAIFactory;
    }

    @Override
    public boolean playerStarted() {
        return this.playerStarted;
    }

    @Override
    public boolean isPlayerControlledByAI() {
        return this.playerAIFactory != null;
    }
}
