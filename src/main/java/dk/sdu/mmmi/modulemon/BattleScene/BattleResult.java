package dk.sdu.mmmi.modulemon.BattleScene;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleClient.IBattleResult;

public class BattleResult implements IBattleResult {
    private IBattleParticipant winner;
    private IBattleParticipant player;
    private IBattleParticipant enemy;
    private int turnCount;
    private IBattleParticipant starter;

    public BattleResult(IBattleParticipant winner, IBattleParticipant player, IBattleParticipant enemy, IBattleParticipant starter, int turnCount) {
        this.winner = winner;
        this.player = player;
        this.enemy = enemy;
        this.starter = starter;
        this.turnCount = turnCount;
    }

    @Override
    public IBattleParticipant getWinner() {
        return winner;
    }

    @Override
    public IBattleParticipant getPlayer() {
        return player;
    }

    @Override
    public IBattleParticipant getEnemy() {
        return enemy;
    }

    @Override
    public IBattleParticipant getStarter() {
        return starter;
    }

    @Override
    public int getTurns() {
        return turnCount;
    }

}
