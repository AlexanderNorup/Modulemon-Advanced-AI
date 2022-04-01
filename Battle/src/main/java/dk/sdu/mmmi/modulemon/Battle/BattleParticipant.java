package dk.sdu.mmmi.modulemon.Battle;

import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;

import java.util.ArrayList;
import java.util.List;

public class BattleParticipant implements IBattleParticipant {

    private List<IMonster> monsterTeam;
    private IMonster activeMonster;

    private boolean playerControlled;

    public BattleParticipant(List<IMonster> monsterTeam, boolean playerControlled) {
        this.monsterTeam = monsterTeam;
        if (monsterTeam.size()>0) {
            this.activeMonster = monsterTeam.get(0);
        }
        this.playerControlled = playerControlled;
    }

    public BattleParticipant(List<IMonster> monsterTeam, IMonster activeMonster, boolean playerControlled) {
        this.monsterTeam = monsterTeam;
        this.activeMonster = activeMonster;
        this.playerControlled = playerControlled;
    }

    @Override
    public boolean isPlayerControlled() {
        return playerControlled;
    }

    @Override
    public IMonster getActiveMonster() {
        return activeMonster;
    }

    @Override
    public void setActiveMonster(IMonster monster) {
        this.activeMonster = monster;
    }

    @Override
    public List<IMonster> getMonsterTeam() {
        return this.monsterTeam;
    }

    @Override
    public IBattleParticipant clone() {
        List<IMonster> cloneTeam = new ArrayList<>();
        IMonster cloneActiveMonster = null;
        for (IMonster monster : this.monsterTeam) {
            IMonster cloneMonster = monster.clone();
            cloneTeam.add(cloneMonster);
            if (monster == this.activeMonster) {
                cloneActiveMonster = cloneMonster;
            }
        }
        return new BattleParticipant(cloneTeam, cloneActiveMonster, this.playerControlled);

    }
}
