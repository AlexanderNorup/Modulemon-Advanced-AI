package dk.sdu.mmmi.modulemon.EmptyAI;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAI;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAIFactory;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleSimulation;

public class EmptyAIFactory implements IBattleAIFactory {

    public EmptyAIFactory() {    }

    @Override
    public IBattleAI getBattleAI(IBattleSimulation battleSimulation, IBattleParticipant participantToControl) {
        return new EmptyAI(battleSimulation, participantToControl);
    }

    @Override
    public String toString() {
        return "Empty AI";
    }
}
