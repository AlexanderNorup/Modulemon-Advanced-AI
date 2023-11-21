package dk.sdu.mmmi.modulemon.BattleScene.animations;

import com.badlogic.gdx.audio.Sound;
import dk.sdu.mmmi.modulemon.BattleScene.scenes.BattleScene;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;

public class EnemyChristmasPresentAnimation extends EnemyBattleAttackAnimation{
    private boolean gavePresent = false;
    public EnemyChristmasPresentAnimation(BattleScene battleScene, Sound attackSound, IGameSettings settings) {
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
            super._battleScene.setPlayerSprite("/images/present_back.png", getClass());
            super._battleScene.setPlayerMonsterName("Rasmus");
            this.gavePresent = true;
        }
    }
}
