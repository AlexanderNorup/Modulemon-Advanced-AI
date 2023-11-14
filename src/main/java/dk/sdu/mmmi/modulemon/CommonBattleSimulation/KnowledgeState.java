package dk.sdu.mmmi.modulemon.CommonBattleSimulation;

import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KnowledgeState {
    // Those of the enemy's monsters, the AI has seen
    private List<IMonster> enemyMonsters;
    // A map, mapping each of the enemy's monsters to a list of the moves, the AI has seen it use
    private Map<IMonster, List<IMonsterMove>> monsterMoves;

    private boolean allKnowing = false;

    public KnowledgeState(boolean allKnowing) {
        this.allKnowing = allKnowing;
        enemyMonsters = new ArrayList<>();
        monsterMoves = new HashMap<>();
    }

    public List<IMonster> getEnemyMonsters() {
        return enemyMonsters;
    }

    public Map<IMonster, List<IMonsterMove>> getMonsterMoves() {
        return monsterMoves;
    }

    public boolean hasSeenMove(IMonster monster, IMonsterMove move){
        if(allKnowing){
            return true;
        }
        if(!this.monsterMoves.containsKey(monster)){
            return false;
        }

        return this.monsterMoves.get(monster).contains(move);
    }

    public boolean hasSeenMonster(IMonster monster){
        if(allKnowing){
            return true;
        }
        return this.enemyMonsters.contains(monster);
    }
}
