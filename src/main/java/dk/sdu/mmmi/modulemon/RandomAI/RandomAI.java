package dk.sdu.mmmi.modulemon.RandomAI;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAI;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleSimulation;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;

import java.util.Random;
import java.util.stream.Stream;

public class RandomAI implements IBattleAI {

    private IBattleParticipant participantToControl;
    private IBattleSimulation battleSimulation;
    private boolean switchingAllowed;
    private Random randomGenerator;

    public RandomAI(IBattleSimulation battleSimulation, IBattleParticipant participantToControl, boolean switchingAllowed) {
        this.participantToControl = participantToControl;
        this.battleSimulation = battleSimulation;
        this.randomGenerator = new Random();
        this.switchingAllowed = switchingAllowed;
    }

    @Override
    public void doAction() {
        var action = getRandomAction();

        if (action instanceof IMonsterMove move) {
            battleSimulation.doMove(participantToControl, move);
        } else if (action instanceof IMonster monster) {
            battleSimulation.switchMonster(participantToControl, monster);
        } else {
            // No action. Just do nothing
            battleSimulation.doMove(participantToControl, null);
        }
    }

    private Object getRandomAction() {
        var monster = participantToControl.getActiveMonster();
        var possibleMoves = monster.getMoves();
        var possibleSwitchActions = participantToControl.getMonsterTeam().stream()
                .filter(x -> switchingAllowed)
                .filter(m -> m.getHitPoints() > 0 && m != participantToControl.getActiveMonster()).toList();

        var actions = Stream.concat(possibleMoves.stream(), possibleSwitchActions.stream()).toList();

        if (actions.isEmpty()) {
            return null;
        }

        return actions.get(this.randomGenerator.nextInt(actions.size()));
    }

    @Override
    public void opposingMonsterUsedMove(IMonster monster, IMonsterMove move) {
    }
}
