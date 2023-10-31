package dk.sdu.mmmi.modulemon.BattleScene.animations;

import dk.sdu.mmmi.modulemon.BattleScene.scenes.BattleScene;
import dk.sdu.mmmi.modulemon.BattleScene.scenes.BattleSceneDefaults;
import dk.sdu.mmmi.modulemon.common.animations.BaseAnimation;
import dk.sdu.mmmi.modulemon.common.data.GameData;

import java.util.ArrayList;

public class PlayerCrashAnimation extends BaseAnimation {

    private BattleScene _battleScene;

    public PlayerCrashAnimation(BattleScene battleScene) {
        super();
        Timeline = new int[]{0, 2000};
        States = new ArrayList<>(Timeline.length);

        //Initial state
        States.add(new float[]{
                BattleSceneDefaults.playerMonsterRotation() //rotation
        });

        States.add(new float[]{
                3600f //rotation
        });

        this._battleScene = battleScene;
    }

    @Override
    public void update(GameData gameData) {
        super.tick();

        float[] states = super.getCurrentStates();
        this._battleScene.setPlayerMonsterRotation(states[0]);
    }
}
