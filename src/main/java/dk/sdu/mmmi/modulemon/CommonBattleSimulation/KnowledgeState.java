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

    public KnowledgeState() {
        enemyMonsters = new ArrayList<>();
        monsterMoves = new HashMap<>();
    }

    public List<IMonster> getEnemyMonsters() {
        return enemyMonsters;
    }

    public Map<IMonster, List<IMonsterMove>> getMonsterMoves() {
        return monsterMoves;
    }
}
