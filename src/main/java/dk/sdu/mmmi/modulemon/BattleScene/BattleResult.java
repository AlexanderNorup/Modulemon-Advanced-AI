package dk.sdu.mmmi.modulemon.BattleScene;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleClient.IBattleResult;

public class BattleResult implements IBattleResult {
    private IBattleParticipant winner;
    private IBattleParticipant player;
    private IBattleParticipant enemy;
    private int turnCount;

    public BattleResult(IBattleParticipant winner, IBattleParticipant player, IBattleParticipant enemy, int turnCount) {
        this.winner = winner;
        this.player = player;
        this.enemy = enemy;
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
    public int getTurns() {
        return turnCount;
    }
}
