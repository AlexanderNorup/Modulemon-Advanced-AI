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

        selectedTeamAIndicies = new Integer[6];
        selectedTeamBIndicies = new Integer[6];
        selectedTeamAIndicies[0] = 0;
        selectedTeamAIndicies[1] = 3;
        selectedTeamAIndicies[2] = 6;

        selectedTeamBIndicies[0] = 2;

        scene.setTeamAMonsters(getMonsterArray(selectedTeamAIndicies));
        scene.setTeamBMonsters(getMonsterArray(selectedTeamBIndicies));
    }

    private Integer selectedTeamAAI = null;
    private Integer selectedTeamBAI = 1;
    private Integer[] selectedTeamAIndicies;
    private Integer[] selectedTeamBIndicies;
    private int cursorPosition = 0;
    private boolean editingMode = false;
    private boolean redrawRoulettes = true;


    @Override
    public void update(GameData gameData, IGameViewManager gameViewManager) {
        cursorPosition = MathUtils.clamp(cursorPosition, 0, 14);

        if (redrawRoulettes) {
            var teamAAI = getSelectedAI(selectedTeamAAI);
            var teamBAI = getSelectedAI(selectedTeamBAI);
            if (teamAAI != null) {
                scene.setTeamAAIText(teamAAI.toString());
            } else {
                scene.setTeamAAIText("Player controlled");
            }
            if (teamBAI != null) {
                scene.setTeamBAIText(getSelectedAI(selectedTeamBAI).toString());
            } else {
                scene.setTeamBAIText("<choose an AI>");
            }
            scene.setTeamAMonsters(getMonsterArray(selectedTeamAIndicies));
            scene.setTeamBMonsters(getMonsterArray(selectedTeamBIndicies));
            redrawRoulettes = false;
        }

        // Reset all selection
        scene.setSelectedMonsterIndex(-1);
        scene.setChangingMonsterIndex(-1);
        scene.setTeamATextColor(Color.WHITE);
        scene.setTeamBTextColor(Color.WHITE);
        scene.setStartBattleColor(Color.WHITE);
        scene.setShowTeamAAIArrow(false);
        scene.setShowTeamBAIArrow(false);

        if (cursorPosition >= 0 && cursorPosition <= 11) {
            var adjustedCursor = getGridAdjustedCursor(cursorPosition);
            scene.setSelectedMonsterIndex(adjustedCursor % 6);
            if (editingMode) {
                scene.setChangingMonsterIndex(adjustedCursor % 6);
            }
        } else if (cursorPosition == 12) {
            scene.setTeamATextColor(CustomBattleScene.SelectColor);
            scene.setShowTeamAAIArrow(editingMode);
        } else if (cursorPosition == 13) {
            scene.setTeamBTextColor(CustomBattleScene.SelectColor);
            scene.setShowTeamBAIArrow(editingMode);
        } else if (cursorPosition == 14) {
            scene.setStartBattleColor(CustomBattleScene.SelectColor);
            if (editingMode) {
                System.out.println("SHOULD START A BATTLE!!");
                editingMode = false;
            }
        }
    }


    @Override
    public void draw(GameData gameData) {
        scene.draw(gameData);
    }

    @Override
    public void handleInput(GameData gameData, IGameViewManager gameViewManager) {
        if (gameData.getKeys().isPressed(GameKeys.BACK)) {
            chooseSound.play(getSoundVolume());
            if (editingMode) {
                editingMode = false;
            } else {
                gameViewManager.setDefaultView();
            }
        }

        if (gameData.getKeys().isPressed(GameKeys.ACTION)) {
            editingMode = !editingMode;
        }

        if (gameData.getKeys().isPressed(GameKeys.DELETE) && cursorPosition < 12) {
            addToSelectedIndicies(null);
            editingMode = false;
        }

        // This is a hardcoded mess. Don't think too hard about it, it just works with the current setup.
        if (gameData.getKeys().isPressed(GameKeys.UP) && !editingMode) {
            selectSound.play(getSoundVolume());
            if (cursorPosition <= 12) {
                cursorPosition -= 4;
            } else {
                cursorPosition -= 2;
            }
        } else if (gameData.getKeys().isPressed(GameKeys.DOWN) && !editingMode) {
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

            if (!editingMode) {
                if (cursorPosition == 14) {
                    cursorPosition = 13;
                } else {
                    cursorPosition += 1;
                }
            } else {
                addToSelectedIndicies(1);
            }
        } else if (gameData.getKeys().isPressed(GameKeys.LEFT)) {
            selectSound.play(getSoundVolume());

            if (!editingMode) {
                if (cursorPosition == 14) {
                    cursorPosition = 12;
                } else if (cursorPosition == 12) {
                    cursorPosition -= 4;
                } else {
                    cursorPosition -= 1;
                }
            } else {
                addToSelectedIndicies(-1);
            }
        }
    }

    private void addToSelectedIndicies(Integer a) {
        redrawRoulettes = true;
        if (cursorPosition < 12) {
            if (isTeamA(cursorPosition)) {
                setTeamIndicies(a, selectedTeamAIndicies);
            } else {
                setTeamIndicies(a, selectedTeamBIndicies);
            }
        } else if (a == null) {
            return;
        } else if (cursorPosition == 12) {
            selectedTeamAAI += a;
        } else if (cursorPosition == 13) {
            selectedTeamBAI += a;
        }
    }

    private void setTeamIndicies(Integer a, Integer[] teamIndicies) {
        if (a == null) {
            teamIndicies[getGridAdjustedCursor(cursorPosition)] = a;
            return;
        }

        if (teamIndicies[getGridAdjustedCursor(cursorPosition)] == null) {
            if (a > 0){
                teamIndicies[getGridAdjustedCursor(cursorPosition)] = 0;
                return;
            }else{
                teamIndicies[getGridAdjustedCursor(cursorPosition)] = monsterRegistry.getMonsterAmount() - 1;
                return;
            }
        }

        teamIndicies[getGridAdjustedCursor(cursorPosition)] += a;

        if (teamIndicies[getGridAdjustedCursor(cursorPosition)] < 0
                || teamIndicies[getGridAdjustedCursor(cursorPosition)] >= monsterRegistry.getMonsterAmount()) {
            teamIndicies[getGridAdjustedCursor(cursorPosition)] = null;
            return;
        }
    }

    private boolean isTeamA(int cursorPosition) {
        return cursorPosition % 4 < 2;
    }

    private int getGridAdjustedCursor(int cursorPosition) {
        boolean isTeamA = isTeamA(cursorPosition);
        int offsetPerSide = cursorPosition / (isTeamA ? 4 : 2);
        scene.setTeamA(isTeamA);
        var adjustedCursor = cursorPosition - 2 * offsetPerSide;
        if (!isTeamA) {
            adjustedCursor += 2 * (cursorPosition / 4);
        }
        return adjustedCursor;
    }

    private IBattleAIFactory getSelectedAI(Integer index) {
        if (index == null) {
            return null;
        }
        return this.battleAIFactoryList.get(index % battleAIFactoryList.size());
    }

    private IMonster[] getMonsterArray(Integer[] indicies) {
        var monsters = new IMonster[indicies.length];
        for (int i = 0; i < indicies.length; i++) {
            if (indicies[i] != null) {
                monsters[i] = monsterRegistry.getMonster(indicies[i] % monsterRegistry.getMonsterAmount());
            }
        }
        return monsters;
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
