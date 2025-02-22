package dk.sdu.mmmi.modulemon.Monster;

import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterMove;

import java.util.Locale;

public class MonsterMove implements IMonsterMove {

    private String name;
    private int damage;
    private MonsterType type;
    private String soundPath;

    private float accuracy = 0.90f;
    public MonsterMove(String name, int damage, MonsterType type, String soundPath, float accuracy) {
        this.name = name;
        this.damage = damage;
        this.type = type;
        this.soundPath = soundPath;
        this.accuracy = accuracy;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getDamage() {
        return damage;
    }

    public MonsterType getType() {
        return type;
    }

    @Override
    public String getSoundPath() {
        return soundPath;
    }

    @Override
    public String getBattleDescription() {
        return "Move: [" + this.getType() + "] " + this.getName() + ". Base damage: " + this.getDamage();
    }

    @Override
    public String getSummaryScreenDescription() {
        String moveType = String.valueOf(getType()).toLowerCase(Locale.ROOT);
        String upperCaseMoveType = moveType.substring(0, 1).toUpperCase() + moveType.substring(1);
        return this.getName() + " - " + this.getDamage() + " - " + upperCaseMoveType;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public float getAccuracy() {
        return accuracy;
    }
}
