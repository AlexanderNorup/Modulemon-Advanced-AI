package dk.sdu.mmmi.modulemon.BattleScene.animations;

import com.badlogic.gdx.audio.Sound;
import dk.sdu.mmmi.modulemon.BattleScene.scenes.BattleScene;
import dk.sdu.mmmi.modulemon.BattleScene.scenes.BattleSceneDefaults;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.animations.AnimationCurves;
import dk.sdu.mmmi.modulemon.common.animations.BaseAnimation;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.drawing.Position;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;

import java.util.ArrayList;

public class MonsterLoafAroundAnimation extends BaseAnimation {
    private BattleScene battleScene;
    private IMonsterSpecifierDelegate monsterController;
    private Sound _attackSound;
    private boolean _attackSoundPlayed = false;
    private IGameSettings settings;

    public MonsterLoafAroundAnimation(BattleScene battleScene, IMonsterSpecifierDelegate monsterController, Sound attackSound, IGameSettings settings) {
        super();
        this.battleScene = battleScene;
        this.monsterController = monsterController;
        this._attackSound = attackSound;
        this.settings = settings;
        super.animationCurve = AnimationCurves.Linear();

        Timeline = new int[]{0, 210, 420, 630, 840, 1050, 1260, 1470, 1680, 1890, 2100, 2310, 2520};
        States = new ArrayList<>(Timeline.length);

        States.add(new float[]{
                monsterController.getDefaultPosition().getX(), monsterController.getDefaultPosition().getY(), //Monster x,y
                0f //Rotation
        });

        States.add(new float[]{
                monsterController.getDefaultPosition().getX() + 10, monsterController.getDefaultPosition().getY() + 5, //Monster x,y
                10f //Rotation
        });

        States.add(new float[]{
                monsterController.getDefaultPosition().getX() + 20, monsterController.getDefaultPosition().getY() + 0, //Monster x,y
                -10f //Rotation
        });

        States.add(new float[]{
                monsterController.getDefaultPosition().getX() + 30, monsterController.getDefaultPosition().getY() + 5, //Monster x,y
                10f //Rotation
        });

        States.add(new float[]{
                monsterController.getDefaultPosition().getX() + 20, monsterController.getDefaultPosition().getY() + 0, //Monster x,y
                -10f //Rotation
        });

        States.add(new float[]{
                monsterController.getDefaultPosition().getX() + 10, monsterController.getDefaultPosition().getY() + 5, //Monster x,y
                10f //Rotation
        });

        //go left
        States.add(new float[]{
                monsterController.getDefaultPosition().getX(), monsterController.getDefaultPosition().getY(), //Monster x,y
                0f //Rotation
        });

        States.add(new float[]{
                monsterController.getDefaultPosition().getX() - 10, monsterController.getDefaultPosition().getY() + 5, //Monster x,y
                10f //Rotation
        });

        States.add(new float[]{
                monsterController.getDefaultPosition().getX() - 20, monsterController.getDefaultPosition().getY() + 0, //Monster x,y
                -10f //Rotation
        });

        States.add(new float[]{
                monsterController.getDefaultPosition().getX() - 30, monsterController.getDefaultPosition().getY() + 5, //Monster x,y
                10f //Rotation
        });

        States.add(new float[]{
                monsterController.getDefaultPosition().getX() - 20, monsterController.getDefaultPosition().getY() + 0, //Monster x,y
                -10f //Rotation
        });

        States.add(new float[]{
                monsterController.getDefaultPosition().getX() - 10, monsterController.getDefaultPosition().getY() + 5, //Monster x,y
                10f //Rotation
        });

        States.add(new float[]{
                monsterController.getDefaultPosition().getX(), monsterController.getDefaultPosition().getY(), //Monster x,y
                0f //Rotation
        });

        monsterController.setX(battleScene, States.get(0)[0]);
        monsterController.setY(battleScene, States.get(0)[1]);
    }

    @Override
    public void update(GameData gameData) {
        super.tick();
        float[] states = super.getCurrentStates();
        monsterController.setX(battleScene, states[0]);
        monsterController.setY(battleScene, states[1]);
        monsterController.setRotation(battleScene, states[2]);

        if (!_attackSoundPlayed && _attackSound != null) {
            if (settings != null) {
                _attackSound.play((int) settings.getSetting(SettingsRegistry.getInstance().getSoundVolumeSetting()) / 100f);
            } else _attackSound.play();
            _attackSoundPlayed = true;
        }
    }
}
