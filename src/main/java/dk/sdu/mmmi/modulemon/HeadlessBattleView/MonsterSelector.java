package dk.sdu.mmmi.modulemon.HeadlessBattleView;


import com.badlogic.gdx.utils.Array;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MonsterSelector {
    private Random random;
    private final Array<IMonster> allowedMonsters;
    private final int seed;

    public MonsterSelector(int seed, IMonsterRegistry registry) {
        this.seed = seed;
        this.random = new Random(this.seed);
        allowedMonsters = new Array<>(registry.getMonsterAmount()-1);
        for (IMonster mon : registry.getAllMonsters()) {
            if(!mon.getName().equalsIgnoreCase("god")){
                allowedMonsters.add(mon);
            }
        }
    }

    public List<IMonster> createMonsterTeam(int amount){
        List<IMonster> monsters = new ArrayList<>();
        for(int i = 0; i < amount; i++){
            var chosenMonsterIndex = random.nextInt(allowedMonsters.size);
            var monster = allowedMonsters.get(chosenMonsterIndex).clone();
            monsters.add(monster);
        }

        return monsters;
    }

    public void refreshSelector(){
        random = new Random(this.seed);
    }
}
