package dk.sdu.mmmi.modulemon.CustomBattleView;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import dk.sdu.mmmi.modulemon.CommonBattleClient.IBattleView;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAIFactory;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleSimulation;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterRegistry;
import dk.sdu.mmmi.modulemon.common.AssetLoader;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.data.GameKeys;
import dk.sdu.mmmi.modulemon.common.data.IGameViewManager;
import dk.sdu.mmmi.modulemon.common.drawing.MathUtils;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;
import dk.sdu.mmmi.modulemon.common.services.IGameViewService;

import java.util.ArrayList;
import java.util.List;

public class CustomBattleView implements IGameViewService {
    private IGameSettings settings;
    private SettingsRegistry settingsRegistry = SettingsRegistry.getInstance();
    private List<IBattleAIFactory> battleAIFactoryList = new ArrayList<>();
    private IBattleView battleView;
    private IMonsterRegistry monsterRegistry;
    private IBattleSimulation battleSimulation;

    private CustomBattleScene scene;

    // LibGDX Sound stuff
    private Sound selectSound;
    private Sound chooseSound;
    private Music customBattleMusic;


    @Override
    public void init(IGameViewManager gameViewManager) {
        selectSound = AssetLoader.getInstance().getSoundAsset("/sounds/select.ogg", this.getClass());
        chooseSound = AssetLoader.getInstance().getSoundAsset("/sounds/choose.ogg", this.getClass());
        customBattleMusic = AssetLoader.getInstance().getMusicAsset("/music/customBattleMenu.ogg", this.getClass());
        customBattleMusic.setVolume((int) settings.getSetting(settingsRegistry.getMusicVolumeSetting()) / 100f);
        customBattleMusic.setLooping(true);
        customBattleMusic.play();

        scene = new CustomBattleScene(settings);

        var monsters = monsterRegistry.getAllMonsters();

        var teamA = new IMonster[6];
        var teamB = new IMonster[6];

        teamA[0] = monsters[0];
        teamA[1] = monsters[3];
        teamA[2] = monsters[6];

        teamB[0] = monsters[2];

        scene.setTeamAMonsters(teamA);
        scene.setTeamBMonsters(teamB);
    }

    private int cursorPosition = 0;

    @Override
    public void update(GameData gameData, IGameViewManager gameViewManager) {
        cursorPosition = MathUtils.clamp(cursorPosition, 0, 14);
        scene.setTeamAAIText("Player Controlled");
        scene.setTeamBAIText(battleAIFactoryList.get(0).toString());

        // Reset all selection
        scene.setSelectedMonsterIndex(-1);
        scene.setTeamATextColor(Color.WHITE);
        scene.setTeamBTextColor(Color.WHITE);
        scene.setStartBattleColor(Color.WHITE);


        if (cursorPosition >= 0 && cursorPosition <= 11) {
            boolean isTeamA = cursorPosition % 4 < 2;
            int overOnTheOtherSide = cursorPosition / (isTeamA ? 4 : 2);
            scene.setTeamA(isTeamA);
            var adjustedCursor = cursorPosition - 2 * overOnTheOtherSide;
            if (!isTeamA) {
                adjustedCursor += 2 * (cursorPosition / 4);
            }
            scene.setSelectedMonsterIndex(adjustedCursor % 6);
        } else if (cursorPosition == 12) {
            scene.setTeamATextColor(CustomBattleScene.SelectColor);
        } else if (cursorPosition == 13) {
            scene.setTeamBTextColor(CustomBattleScene.SelectColor);
        } else if (cursorPosition == 14) {
            scene.setStartBattleColor(CustomBattleScene.SelectColor);
        }

    }

    @Override
    public void draw(GameData gameData) {
        scene.draw(gameData);
    }

    @Override
    public void handleInput(GameData gameData, IGameViewManager gameViewManager) {
        if (gameData.getKeys().isDown(GameKeys.BACK)) {
            chooseSound.play(getSoundVolume());
            gameViewManager.setDefaultView();
        }

        if (gameData.getKeys().isPressed(GameKeys.UP)) {
            selectSound.play(getSoundVolume());
            if (cursorPosition <= 12) {
                cursorPosition -= 4;
            } else {
                cursorPosition -= 2;
            }
        } else if (gameData.getKeys().isPressed(GameKeys.DOWN)) {
            selectSound.play(getSoundVolume());
            if (cursorPosition < 12) {
                if (cursorPosition >= 8) {
                    cursorPosition = cursorPosition % 4 < 2 ? 12 : 13;
                } else {
                    cursorPosition += 4;
                }
            } else {
                cursorPosition += 2;
            }
        } else if (gameData.getKeys().isPressed(GameKeys.RIGHT)) {
            selectSound.play(getSoundVolume());
            if (cursorPosition == 14) {
                cursorPosition = 13;
            } else {
                cursorPosition += 1;
            }
        } else if (gameData.getKeys().isPressed(GameKeys.LEFT)) {
            selectSound.play(getSoundVolume());
            if (cursorPosition == 14) {
                cursorPosition = 12;
            } else if (cursorPosition == 12) {
                cursorPosition -= 4;
            } else {
                cursorPosition -= 1;
            }
        }
    }

    @Override
    public void dispose() {
        customBattleMusic.stop();
        customBattleMusic.dispose();
        customBattleMusic = null;
    }

    public void addBattleAI(IBattleAIFactory battleAIFactory) {
        battleAIFactoryList.add(battleAIFactory);
    }

    public void setBattleView(IBattleView battleView) {
        this.battleView = battleView;
    }

    public void setBattleSimulation(IBattleSimulation battleSimulation) {
        this.battleSimulation = battleSimulation;
    }

    public void setMonsterRegistry(IMonsterRegistry monsterRegistry) {
        this.monsterRegistry = monsterRegistry;
    }

    public void setSettings(IGameSettings settings) {
        this.settings = settings;
    }

    private float getMusicVolume() {
        return ((int) settings.getSetting(settingsRegistry.getMusicVolumeSetting()) / 100f);
    }

    private float getSoundVolume() {
        return ((int) settings.getSetting(settingsRegistry.getSoundVolumeSetting()) / 100f);
    }

    @Override
    public String toString() {
        return "Custom Battle";
    }
}
