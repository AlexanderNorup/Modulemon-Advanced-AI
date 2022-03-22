package dk.sdu.mmmi.modulemon.CommonBattle.BattleEvents;

import dk.sdu.mmmi.modulemon.CommonBattleParticipant.IBattleParticipant;

public class VictoryBattleEvent implements IBattleEvent {
    private String text;
    private IBattleParticipant winner;

    public VictoryBattleEvent(String text, IBattleParticipant winner) {
        this.text = text;
        this.winner = winner;
    }

    @Override
    public String getText() {
        return text;
    }

    public IBattleParticipant getWinner() {
        return winner;
    }
}
