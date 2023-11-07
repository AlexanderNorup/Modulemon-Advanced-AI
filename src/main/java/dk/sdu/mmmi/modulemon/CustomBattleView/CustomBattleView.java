package dk.sdu.mmmi.modulemon.CustomBattleView;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.TimeUtils;
import dk.sdu.mmmi.modulemon.CommonBattleClient.IBattleView;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAIFactory;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleSimulation;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterRegistry;
import dk.sdu.mmmi.modulemon.common.AssetLoader;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.animations.BaseAnimation;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.data.GameKeys;
import dk.sdu.mmmi.modulemon.common.data.IGameViewManager;
import dk.sdu.mmmi.modulemon.common.drawing.MathUtils;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;
import dk.sdu.mmmi.modulemon.common.services.IGameViewService;

import java.util.*;

public class CustomBattleView implements IGameViewService {
    private IGameSettings settings;
    private SettingsRegistry settingsRegistry = SettingsRegistry.getInstance();
    private List<IBattleAIFactory> battleAIFactoryList = new ArrayList<>();
    private IBattleView battleView;
    private IMonsterRegistry monsterRegistry;
    private IBattleSimulation battleSimulation;

    private Queue<BaseAnimation> backgroundAnimations;
    private CustomBattleScene scene;

    // LibGDX Sound stuff
    private Sound selectSound;
    private Sound chooseSound;
    private Sound wrongSound;
    private Music customBattleMusic;

    @Override
    public void init(IGameViewManager gameViewManager) {
        selectSound = AssetLoader.getInstance().getSoundAsset("/sounds/select.ogg", this.getClass());
        chooseSound = AssetLoader.getInstance().getSoundAsset("/sounds/choose.ogg", this.getClass());
        wrongSound = AssetLoader.getInstance().getSoundAsset("/sounds/metal-pipe.ogg", this.getClass());
        customBattleMusic = AssetLoader.getInstance().getMusicAsset("/music/customBattleMenu.ogg", this.getClass());
        customBattleMusic.setVolume((int) settings.getSetting(settingsRegistry.getMusicVolumeSetting()) / 100f);
        customBattleMusic.setLooping(true);
        customBattleMusic.play();

        scene = new CustomBattleScene(settings);
        backgroundAnimations = new LinkedList<>();
        redrawRoulettes = true;
    }

    private Integer selectedTeamAAI = null;
    private Integer selectedTeamBAI = 1;
    private Integer[] selectedTeamAIndicies = new Integer[6];
    private Integer[] selectedTeamBIndicies = new Integer[6];
    private int cursorPosition = 0;
    private boolean editingMode = false;
    private boolean redrawRoulettes = true;
    private boolean showingResults = false;


    @Override
    public void update(GameData gameData, IGameViewManager gameViewManager) {
        cursorPosition = MathUtils.clamp(cursorPosition, 0, 14);
        scene.setShowResults(showingResults);
        var nextAnimation = backgroundAnimations.peek();
        if (nextAnimation != null) {
            nextAnimation.update(gameData);
            if (nextAnimation.isFinished()) {
                backgroundAnimations.poll();
            }
        }

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
                startBattle(gameViewManager);
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
            if (showingResults) {
                showingResults = false;
                return;
            }

            editingMode = !editingMode;
            chooseSound.play(getSoundVolume());
        }

        if (showingResults) {
            return; // Don't allow any movement when showing results
        }

        if (gameData.getKeys().isPressed(GameKeys.DELETE) && cursorPosition < 13) {
            addToSelectedIndicies(null);
            editingMode = false;
            chooseSound.play(getSoundVolume());
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

    private void startBattle(IGameViewManager gameViewManager) {
        var teamA = Arrays.stream(getMonsterArray(selectedTeamAIndicies)).filter(Objects::nonNull).toList();
        var teamB = Arrays.stream(getMonsterArray(selectedTeamBIndicies)).filter(Objects::nonNull).toList();

        var teamAAI = getSelectedAI(selectedTeamAAI);
        var teamBAI = getSelectedAI(selectedTeamBAI);

        if (teamA.isEmpty() || teamB.isEmpty() || teamBAI == null) {
            wrongSound.play(1);
            var anim = new ErrorTextAnimation(scene, "Add some monsters to both teams you dork!");
            anim.start();
            backgroundAnimations.add(anim);
            return;
        }

        System.out.println("Using Team A AI: " + teamAAI);
        System.out.println("Using Team B AI: " + teamBAI);
        battleSimulation.setOpponentAIFactory(teamBAI);

        gameViewManager.setView(battleView.getGameView(), false); // Do not dispose the map
        customBattleMusic.stop();
        long startTime = TimeUtils.millis();
        battleView.startBattle(teamA, teamB, result -> {
            customBattleMusic.play();
            gameViewManager.setView(this);
            boolean teamAWon = result.getWinner() == result.getPlayer();
            var winnerTeamName = teamAWon ?
                    (teamAAI == null ? "You" : teamAAI.toString())
                    : (teamBAI.toString());
            scene.setResultsHeader("The winner is: " + winnerTeamName + "!");

            var resultLines = new ArrayList<String>() {{
                add(String.format("Total turns: %d", result.getTurns()));
                add(String.format("Battle time: %.2f seconds", (TimeUtils.timeSinceMillis(startTime) / 1000f)));
                add(String.format("The winning team ended up as so:"));
            }};

            for (var monster : result.getWinner().getMonsterTeam()) {
                resultLines.add(String.format("  - %s", monster));
            }

            scene.setResultLines(resultLines.toArray(new String[resultLines.size()]));
            cursorPosition = 0;
            showingResults = true;
            editingMode = false;
        });

        // Set the battle AI after the startBattle method to override the configs.
        battleSimulation.setOpponentAIFactory(teamBAI);
        if (teamAAI != null) {
            battleSimulation.setPlayerAIFactory(teamAAI);
        }
    }

    private void addToSelectedIndicies(Integer a) {
        redrawRoulettes = true;
        if (cursorPosition < 12) {
            var numMonsters = this.monsterRegistry.getMonsterAmount() - 1;
            if (isTeamA(cursorPosition)) {
                selectedTeamAIndicies[getGridAdjustedCursor(cursorPosition)] = scrollIndexWithNull(selectedTeamAIndicies[getGridAdjustedCursor(cursorPosition)], a, numMonsters);
            } else {
                selectedTeamBIndicies[getGridAdjustedCursor(cursorPosition)] = scrollIndexWithNull(selectedTeamBIndicies[getGridAdjustedCursor(cursorPosition)], a, numMonsters);
            }
        } else if (cursorPosition == 12) {
            selectedTeamAAI = scrollIndexWithNull(selectedTeamAAI, a, this.battleAIFactoryList.size() - 1);
        } else if (cursorPosition == 13) {
            var newBValue = scrollIndexWithNull(selectedTeamBAI, a, this.battleAIFactoryList.size() - 1);
            if (newBValue == null) {
                newBValue = selectedTeamBAI == 0 ? this.battleAIFactoryList.size() - 1 : 0;
            }
            selectedTeamBAI = newBValue;
        }
    }

    private Integer scrollIndexWithNull(Integer input, Integer a, int maxValue) {
        if (a == null) {
            return a;
        }

        if (input == null) {
            if (a > 0) {
                return 0;
            } else {
                return maxValue;
            }
        }

        var out = input + a;

        if (out < 0
                || out > maxValue) {
            return null;
        }

        return out;
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
