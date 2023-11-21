package dk.sdu.mmmi.modulemon.Monster;

import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleMonsterProcessor;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;

import java.util.Random;

public class BattleMonsterProcessor implements IBattleMonsterProcessor {

    @Override
    public IMonster whichMonsterStarts(IMonster iMonster1, IMonster iMonster2) {
        Monster monster1 = (Monster) iMonster1;
        Monster monster2 = (Monster) iMonster2;
        return monster1.getSpeed() >= monster2.getSpeed() ? monster1 : monster2;
    }

    @Override
    public int calculateDamage(IMonster iSource, IMonsterMove iMove, IMonster iTarget) {
        Monster source = (Monster) iSource;
        Monster target = (Monster) iTarget;
        MonsterMove move = (MonsterMove) iMove;
        float moveDamage = (float) move.getDamage();
        float sourceAttack = (float) source.getAttack();
        float targetDefence = (float) target.getDefence();

        if (!doAccuracyHit(move.getAccuracy())) { return 0; }
        // Same type attack bonus. Effectively the same as STAB in that other game
        boolean same_attack_type = source.getMonsterType() == move.getType();
        float attack_bonus = 1;

        if(same_attack_type){
            attack_bonus = 1.5f;
        }

        float monsterAttackDefence = (0.2f * sourceAttack + 3 + 20 ) / (targetDefence + 50);

        int damage =  Math.round( monsterAttackDefence * moveDamage * attack_bonus * calculateTypeAdvantage(move.getType(), target.getMonsterType()) * calculateCriticalHit(source) );
        return damage;
    }

    public float calculateTypeAdvantage(MonsterType source, MonsterType target) {
        switch (source) {
            case NORMAL:
                switch (target) {
                    default: return 1;
                }
            case FIRE:
                switch (target) {
                    case FIRE: return 0.5f;
                    case WATER: return 0.5f;
                    case GRASS: return 2;
                    default: return 1;
                }
            case AIR:
                switch (target) {
                    case GRASS: return 2;
                    case LIGHTNING: return 0.5f;
                    default: return 1;
                }
            case EARTH:
                switch (target) {
                    case AIR: return 0;
                    case FIRE: return 2;
                    case GRASS: return 0.5f;
                    case LIGHTNING: return 2;
                    default: return 1;
                }
            case GRASS:
                switch (target) {
                    case AIR: return 0.5f;
                    case EARTH: return 2;
                    case FIRE: return 0.5f;
                    case GRASS: return 0.5f;
                    default: return 1;
                }
            case WATER:
                switch (target) {
                    case EARTH: return 2;
                    case FIRE: return 2;
                    case WATER: return 0.5f;
                    case GRASS: return 0.5f;
                    default: return 1;
                }
            case LIGHTNING:
                switch (target) {
                    case AIR: return 2;
                    case EARTH: return 0;
                    case WATER: return 2;
                    case GRASS: return 0.5f;
                    case LIGHTNING: return 0.5f;
                    default: return 1;
                }
            default: return 1;
        }
    }

    public float calculateCriticalHit (Monster monster) {
        float baseSpeed = (float) monster.getSpeed();

        float threshold = (float) (baseSpeed / 0.5);

        // To ensure threshold doesn't go above 255
        if (threshold > 255) {
            threshold = 255;
        }

        Random random = new Random();
        //Generate random number between 0-255
        float randomVal = random.nextInt(256);
        //true if the generated number is less than the threshold
        boolean criticalHit = randomVal >= threshold;

        if (criticalHit) {
            return 1.5f;
        } else {
            return 1f;
        }
    }

    public boolean doAccuracyHit(float factor) {
        //Generates a random value
        Random random = new Random();
        //Ensures that the next move will always be below our factor and between(0-1)
        return random.nextFloat() <= factor;

    }
}
