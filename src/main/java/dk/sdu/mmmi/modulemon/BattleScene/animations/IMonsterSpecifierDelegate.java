package dk.sdu.mmmi.modulemon.BattleScene.animations;

import dk.sdu.mmmi.modulemon.BattleScene.scenes.BattleScene;
import dk.sdu.mmmi.modulemon.common.drawing.Position;

// This is basically a way that makes it so I only have to make a single animation that works for both monsters.
public interface IMonsterSpecifierDelegate {
    Position getDefaultPosition();
    void setX(BattleScene scene, float x);
    void setY(BattleScene scene,float y);
    void setRotation(BattleScene scene,float rotation);
}
