package dk.sdu.mmmi.modulemon.Monster;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleMonsterProcessor;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;

public class BattleMonsterProcessor implements IBattleMonsterProcessor {

    @Override
    public IMonster whichMonsterStarts(IMonster iMonster1, IMonster iMonster2) {
        Monster monster1 = (Monster) iMonster1;
        Monster monster2 = (Monster) iMonster2;
        return monster1.getSpeed() >= monster2.getSpeed() ? monster1 : monster2;
    }

    @Override
    public int calculateDamage(IMonster iSource, IMonsterMove move, IMonster iTarget) {
        Monster source = (Monster) iSource;
        Monster target = (Monster) iTarget;
        float moveDamage = (float) move.getDamage();
        float sourceAttack = (float) source.getAttack();
        float targetDefence = (float) target.getDefence();

        int damage = Math.round(moveDamage*(sourceAttack/targetDefence));

        return damage;
    }

}
