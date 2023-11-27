package dk.sdu.mmmi.modulemon.RandomAI;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAI;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAIFactory;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleSimulation;

public class RandomAIFactory implements IBattleAIFactory {

    private boolean switchingAllowed;
    public RandomAIFactory(boolean swicthingAllowed) {
        this.switchingAllowed = swicthingAllowed;
    }

    @Override
    public IBattleAI getBattleAI(IBattleSimulation battleSimulation, IBattleParticipant participantToControl) {
        return new RandomAI(battleSimulation, participantToControl, this.switchingAllowed);
    }

    @Override
    public String toString() {
        return "Rnd AI (switch: %b)".formatted(this.switchingAllowed);
    }
}
