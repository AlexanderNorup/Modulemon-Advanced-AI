package dk.sdu.mmmi.modulemon.CommonMonster;

import java.util.List;

public interface IMonster {
    String getName();
    int getHitPoints();
    void setHitPoints(int hitPoint);
    int getAttack();
    int getDefence();
    int getSpeed();
    String getFrontSprite();
    String getBackSprite();
    MonsterType getMonsterType();
    List<IMonsterMove> getMoves();
    IMonster clone();
    int getID();
}