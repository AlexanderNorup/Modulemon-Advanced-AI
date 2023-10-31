package dk.sdu.mmmi.modulemon.CommonBattleSimulation.BattleEvents;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleState;

public class AICrashedEvent implements IBattleEvent {
    private String text;
    private IBattleParticipant participant;
    private Exception exception;
    private IBattleState state;
    public AICrashedEvent(String text, IBattleParticipant participant, Exception ex, IBattleState state) {
        this.text = text;
        this.participant = participant;
        this.exception = ex;
        this.state = state;
    }

    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public IBattleState getState() {
        return this.state;
    }

    public IBattleParticipant getParticipant() {
        return participant;
    }

    public Exception getException() {
        return exception;
    }
}
