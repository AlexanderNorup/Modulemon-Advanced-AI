package dk.sdu.mmmi.modulemon.CustomBattleView;

import dk.sdu.mmmi.modulemon.common.animations.AnimationCurves;
import dk.sdu.mmmi.modulemon.common.animations.BaseAnimation;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.drawing.Position;

import java.util.ArrayList;

public class ErrorTextAnimation extends BaseAnimation {
    private CustomBattleScene battleScene;
    public ErrorTextAnimation(CustomBattleScene battleScene, String errorText){
        super();
        this.battleScene = battleScene;
        super.animationCurve = AnimationCurves.EaseIn();

        Timeline = new int[]{0, 400, 1900, 2300};
        States = new ArrayList<>(Timeline.length);

        States.add(new float[]{
                battleScene.getScreenWidth()/2f, battleScene.getScreenHeight()+100, //Position x,y
                0 // Opacity
        });

        States.add(new float[]{
                battleScene.getScreenWidth()/2f,  (battleScene.getScreenHeight()/2f)+5, //Position x,y
                1 // Opacity
        });

        States.add(new float[]{
                battleScene.getScreenWidth()/2f,  (battleScene.getScreenHeight()/2f)-5, //Position x,y
                1 // Opacity
        });

        States.add(new float[]{
                battleScene.getScreenWidth()/2f,  -100, //Position x,y
                0 // Opacity
        });

        battleScene.setErrorText(errorText);
        battleScene.setErrorPosition(new Position(States.get(0)[0], States.get(0)[1]));
        battleScene.setErrorOpacity(States.get(0)[2]);
    }

    @Override
    public void update(GameData gameData) {
        super.tick();
        float[] states = super.getCurrentStates();
        battleScene.setErrorPosition(new Position(states[0], states[1]));
        battleScene.setErrorOpacity(states[2]);
    }
}
