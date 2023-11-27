package dk.sdu.mmmi.modulemon.EmptyAI;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAI;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleSimulation;
import dk.sdu.mmmi.modulemon.CommonMonster.EmptyMove;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;

public class EmptyAI implements IBattleAI {
    private IBattleParticipant participantToControl;
    private IBattleSimulation battleSimulation;

    public EmptyAI(IBattleSimulation battleSimulation, IBattleParticipant participantToControl) {
        this.participantToControl = participantToControl;
        this.battleSimulation = battleSimulation;
    }

    @Override
    public void doAction() {
        // No action. Just do nothing
        battleSimulation.doMove(participantToControl, EmptyMove.getInstance());
    }

    @Override
    public void opposingMonsterUsedMove(IMonster monster, IMonsterMove move) {
    }
}
