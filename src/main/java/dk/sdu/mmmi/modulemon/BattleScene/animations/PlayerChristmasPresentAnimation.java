package dk.sdu.mmmi.modulemon.BattleScene.animations;

import com.badlogic.gdx.audio.Sound;
import dk.sdu.mmmi.modulemon.BattleScene.scenes.BattleScene;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class PlayerChristmasPresentAnimation extends PlayerBattleAttackAnimation{
    private boolean gavePresent = false;
    public PlayerChristmasPresentAnimation(BattleScene battleScene, Sound attackSound, IGameSettings settings) {
        super(battleScene, attackSound, settings);

        //Extend this animation to last for 5 seconds!
        var newTimeline = new int[Timeline.length + 1];
        for(int i = 0; i < Timeline.length; i++){
            newTimeline[i] = Timeline[i];
        }
        newTimeline[newTimeline.length-1] = 5000;
        Timeline = newTimeline;
        States.add(States.get(States.size()-1));
    }

    @Override
    public void update(GameData gameData) {
        super.update(gameData);

        var states = super.getCurrentStates();
        if(states[7] > 0.9f && !this.gavePresent) {
            super._battleScene.setEnemySprite("/images/present_front.png", getClass());
            super._battleScene.setEnemyMonsterName("Rasmus");
            this.gavePresent = true;
        }
    }
}
