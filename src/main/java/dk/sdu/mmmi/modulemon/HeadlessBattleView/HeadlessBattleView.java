package dk.sdu.mmmi.modulemon.HeadlessBattleView;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import dk.sdu.mmmi.modulemon.Battle.BattleParticipant;
import dk.sdu.mmmi.modulemon.BattleScene.BattleResult;
import dk.sdu.mmmi.modulemon.BattleSimulation.BattleSimulation;
import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattleClient.IBattleResult;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.BattleEvents.AICrashedEvent;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.BattleEvents.ChangeMonsterBattleEvent;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.BattleEvents.VictoryBattleEvent;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAIFactory;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonsterRegistry;
import dk.sdu.mmmi.modulemon.Monster.BattleMonsterProcessor;
import dk.sdu.mmmi.modulemon.common.AssetLoader;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.data.GameKeys;
import dk.sdu.mmmi.modulemon.common.data.IGameViewManager;
import dk.sdu.mmmi.modulemon.common.drawing.MathUtils;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;
import dk.sdu.mmmi.modulemon.common.services.IGameViewService;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.BattleEvents.MoveBattleEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Array;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class HeadlessBattleView implements IGameViewService {
    private IGameSettings settings;
    private SettingsRegistry settingsRegistry = SettingsRegistry.getInstance();
    private Sound selectSound;
    private Sound chooseSound;
    private Sound yaySound;
    private List<IBattleAIFactory> battleAIFactoryList = new ArrayList<>();
    private Music battleWaitMusic;
    private Music menuMusic;
    private IMonsterRegistry monsterRegistry;
    private List<Future<IBattleResult>> battleResults;
    private List<Future<IBattleResult>> battleResultsToRemove;
    private int concurrentBattles = 5;
    private int currentBattles = 0;
    private ExecutorService battleExecutor = Executors.newFixedThreadPool(concurrentBattles);
    private HeadlessBattleScene scene;
    private HeadlessBattlingScene battlingScene;
    private int teamAWins = 0;
    private int teamAStartWins = 0;
    private int winTurnsA = 0;
    private int teamBWins = 0;
    private int teamBStartWins = 0;
    private int winTurnsB = 0;
    private int completedBattles = 0;
    private boolean battling = false;
    private boolean doneBattling = false;
    private int cursorPosition = 0;
    private Integer selectedTeamAAI = 1;
    private Integer selectedTeamBAI = 1;
    private boolean editingMode = false;
    private int[] battleAmounts = {1, 10, 100, 250, 500, 750, 1000};
    private int battleAmountIndex = 1;
    private MonsterSelector monsterSelector;
    private final int TEAM_MONSTER_AMOUNT = 3;

    private CSVWriter csvWriter;

    @Override
    public void init(IGameViewManager gameViewManager) {
        selectSound = AssetLoader.getInstance().getSoundAsset("/sounds/select.ogg", this.getClass());
        chooseSound = AssetLoader.getInstance().getSoundAsset("/sounds/choose.ogg", this.getClass());
        yaySound = AssetLoader.getInstance().getSoundAsset("/sounds/YAY.ogg", this.getClass());
        battleWaitMusic = AssetLoader.getInstance().getMusicAsset("/music/headless.ogg", this.getClass());
        battleWaitMusic.setLooping(true);
        battleWaitMusic.setVolume(getMusicVolume());
        menuMusic = AssetLoader.getInstance().getMusicAsset("/music/headlessMenu.ogg", this.getClass());
        menuMusic.setLooping(true);
        menuMusic.setVolume(getMusicVolume());
        menuMusic.play();

        csvWriter = new CSVWriter();
        csvWriter.setWriteExcelHeader(true); // Makes it so the file is easy to open in Excel.

        var coloumnTitles = new ArrayList<String>() {{
            add("Battle");
            add("WinnerTeam");
            add("TurnsToWin");
        }};
        int monstersPerTeam = 3;
        for(String team : new String[]{"WinnerTeam", "LoserTeam"}) {
            for (int i = 0; i < monstersPerTeam; i++) {
                coloumnTitles.add(String.format("%s_Monster%d_Name", team, i+1));
                coloumnTitles.add(String.format("%s_Monster%d_HP", team, i+1));
            }
        }

        csvWriter.setColumnTitles(coloumnTitles.toArray(new String[0]));

        scene = new HeadlessBattleScene(settings);
        battlingScene = new HeadlessBattlingScene();
        monsterSelector = new MonsterSelector(1337, monsterRegistry);
    }

    @Override
    public void update(GameData gameData, IGameViewManager gameViewManager) {
        if (battling) {
            int battleAmount = battleAmounts[battleAmountIndex];
            battlingScene.setTeamAWins(teamAWins);
            battlingScene.setTeamBWins(teamBWins);
            battlingScene.setTeamAStartWins(teamAStartWins);
            battlingScene.setTeamBStartWins(teamBStartWins);
            battlingScene.setAvgTurnsToWinA((float)winTurnsA / teamAWins);
            battlingScene.setAvgTurnsToWinB((float)winTurnsB / teamBWins);
            battlingScene.setBattleProgress(((float) completedBattles / battleAmount));
            battlingScene.setCurrentBattles(currentBattles);
            battlingScene.setDoneBattling(doneBattling);
            for (var battleResultFuture : battleResults) {
                if (battleResultFuture.isDone()) {
                    try {
                        var battleResult = battleResultFuture.get();
                        boolean teamAWon = true;
                        if (battleResult.getWinner() == battleResult.getPlayer()) {
                            teamAWins++;
                            if(battleResult.getStarter() == battleResult.getWinner()){
                                teamAStartWins++;
                            }
                            winTurnsA += battleResult.getTurns();
                        } else {
                            teamAWon = false;
                            teamBWins++;
                            if(battleResult.getStarter() == battleResult.getWinner()){
                                teamBStartWins++;
                            }
                            winTurnsB += battleResult.getTurns();
                        }
                        completedBattles++;
                        currentBattles--;
                        battleResultsToRemove.add(battleResultFuture);

                        var row = new ArrayList<Object>();
                        row.add(completedBattles);
                        row.add(teamAWon ? "A" : "B");
                        row.add(battleResult.getTurns());

                        var participantWinner = battleResult.getWinner();
                        var participantLoser = battleResult.getWinner().equals(battleResult.getPlayer()) ? battleResult.getEnemy() : battleResult.getPlayer();
                        for(var team : new IBattleParticipant[]{participantWinner, participantLoser}) {
                            for (var monster : team.getMonsterTeam()) {
                                row.add(monster.getName());
                                row.add(monster.getHitPoints());
                            }
                        }

                        csvWriter.addRow(row.toArray());
                    } catch (InterruptedException | ExecutionException e) {
                        System.out.println("Battle was cancelled!");
                    }
                }
            }

            if (battleResultsToRemove.size() > 0) {
                for (var future : battleResultsToRemove) {
                    battleResults.remove(future);
                }
                battleResultsToRemove.clear();
            }

            if (completedBattles >= battleAmount) {
                yaySound.play(getSoundVolume());
                battling = false;
                EmptyOutHandler.setDefaultHandler();
                doneBattling = true;
                currentBattles = 0;
                // Stop any remaining battles
                stopAllBattles();

                // Save CSV file
                try {
                    final String outputDir = "BattleResults";
                    var dir = new File(outputDir);
                    if(!dir.exists()) {
                        dir.mkdirs();
                    }
                    var filename = Path.of(outputDir, String.format("battle_simulation_results_%d.csv", System.currentTimeMillis() / 1000L));
                    csvWriter.save(filename.toAbsolutePath().toString());
                } catch (IOException e) {
                    System.out.println("Tried to save CSV file, but failed! Oh no! Anyway..");
                    e.printStackTrace();
                }

            } else if (currentBattles < concurrentBattles && currentBattles + completedBattles < battleAmount) {
                var amountBattlesToStart = concurrentBattles - currentBattles;
                for (int i = 0; i < amountBattlesToStart; i++) {
                    battleResults.add(runBattle());
                }
            }
        } else if (doneBattling) {
            battlingScene.setDoneBattling(true);
            battlingScene.setTeamAWins(teamAWins);
            battlingScene.setTeamBWins(teamBWins);
            battlingScene.setTeamAStartWins(teamAStartWins);
            battlingScene.setTeamBStartWins(teamBStartWins);
            battlingScene.setAvgTurnsToWinA((float)winTurnsA / teamAWins);
            battlingScene.setAvgTurnsToWinB((float)winTurnsB / teamBWins);
            battlingScene.setBattleProgress((float) completedBattles / battleAmounts[battleAmountIndex]);
            battlingScene.setCurrentBattles(currentBattles);
            if (battleWaitMusic.getVolume() <= 0) {
                battleWaitMusic.stop();
                battleWaitMusic.setVolume(getSoundVolume());
            } else {
                battleWaitMusic.setVolume(MathUtils.clamp(battleWaitMusic.getVolume() - 0.0025f, 0, 1));
            }
        } else {
            cursorPosition = MathUtils.clamp(cursorPosition, 0, 3);

            var teamAAI = getSelectedAI(selectedTeamAAI);
            var teamBAI = getSelectedAI(selectedTeamBAI);
            if (teamAAI != null) {
                scene.setTeamAAIText(teamAAI.toString());
            } else {
                scene.setTeamAAIText("No AI's loaded");
            }
            if (teamBAI != null) {
                scene.setTeamBAIText(getSelectedAI(selectedTeamBAI).toString());
            } else {
                scene.setTeamBAIText("No AI's loaded");
            }

            scene.setTeamIndex(-1);
            scene.setAIIndex(-1);
            scene.setBattleAmount(battleAmounts[battleAmountIndex]);
            scene.setStartColor(Color.WHITE);
            scene.setAmountColor(Color.WHITE);
            scene.setChoosing(editingMode);

            if (cursorPosition >= 0 && cursorPosition <= 1) {
                if (editingMode) {
                    scene.setAIIndex(cursorPosition);
                } else {
                    scene.setTeamIndex(cursorPosition);
                }
            } else if (cursorPosition == 2) {
                scene.setAmountColor(HeadlessBattleScene.SelectColor);
            } else if (cursorPosition == 3) {
                scene.setStartColor(HeadlessBattleScene.SelectColor);
            }
        }
    }

    private void stopAllBattles() {
        doneBattling = true;
        currentBattles = 0;
        for (var battleFuture : battleResults) {
            battleFuture.cancel(true);
        }
    }

    @Override
    public void draw(GameData gameData) {
        if (battling || doneBattling) {
            battlingScene.draw(gameData);
        } else {
            scene.draw(gameData);
        }
    }

    @Override
    public void handleInput(GameData gameData, IGameViewManager gameViewManager) {
        if (doneBattling) {
            if (gameData.getKeys().isPressed(GameKeys.ACTION) || gameData.getKeys().isPressed(GameKeys.BACK)) {
                doneBattling = false;
                teamAWins = 0;
                teamAStartWins = 0;
                winTurnsA = 0;
                teamBWins = 0;
                teamBStartWins = 0;
                winTurnsB = 0;
                completedBattles = 0;
                battlingScene.setAvgTurnsToWinA(0.0f);
                battlingScene.setAvgTurnsToWinB(0.0f);
                battleWaitMusic.stop();
                menuMusic.play();
            }
            return;
        }

        if (gameData.getKeys().isPressed(GameKeys.BACK)) {
            chooseSound.play(getSoundVolume());
            if (editingMode) {
                editingMode = false;
            } else if (battling || doneBattling) {
                battling = false;
                stopAllBattles();
            } else {
                gameViewManager.setDefaultView();
            }
        }

        if (battling) {
            return;
        }

        if (editingMode) {
            if (gameData.getKeys().isPressed(GameKeys.RIGHT)) {
                selectSound.play(getSoundVolume());
                if (cursorPosition == 0) {
                    selectedTeamAAI = scrollIndex(selectedTeamAAI, 1, battleAIFactoryList.size());
                } else if (cursorPosition == 1) {
                    selectedTeamBAI = scrollIndex(selectedTeamBAI, 1, battleAIFactoryList.size());
                } else if (cursorPosition == 2) {
                    battleAmountIndex = scrollIndex(battleAmountIndex, 1, battleAmounts.length);
                }
            }
            if (gameData.getKeys().isPressed(GameKeys.LEFT)) {
                selectSound.play(getSoundVolume());
                if (cursorPosition == 0) {
                    selectedTeamAAI = scrollIndex(selectedTeamAAI, -1, battleAIFactoryList.size());
                } else if (cursorPosition == 1) {
                    selectedTeamBAI = scrollIndex(selectedTeamBAI, -1, battleAIFactoryList.size());
                } else if (cursorPosition == 2) {
                    battleAmountIndex = scrollIndex(battleAmountIndex, -1, battleAmounts.length);
                }
            }
            if (gameData.getKeys().isPressed(GameKeys.ACTION)) {
                chooseSound.play(getSoundVolume());
                editingMode = false;
            }
        } else {
            if (gameData.getKeys().isPressed(GameKeys.DOWN)) {
                selectSound.play(getSoundVolume());
                cursorPosition = cursorPosition >= 3 ? 0 : cursorPosition+1;
            }
            if (gameData.getKeys().isPressed(GameKeys.UP)) {
                selectSound.play(getSoundVolume());
                cursorPosition = cursorPosition <= 0 ? 3 : cursorPosition-1;;
            }
            if (gameData.getKeys().isPressed(GameKeys.ACTION) && cursorPosition == 3) {
                chooseSound.play(getSoundVolume());
                battleResults = new ArrayList<Future<IBattleResult>>();
                battleResultsToRemove = new ArrayList<Future<IBattleResult>>();
                monsterSelector.refreshSelector();
                for (var i = 0; i < concurrentBattles; i++) {
                    battleResults.add(runBattle());
                }
                menuMusic.stop();
                battleWaitMusic.setVolume(getSoundVolume());
                battleWaitMusic.play();
                battling = true;
                EmptyOutHandler.setEmptyHandler();
            } else if (gameData.getKeys().isPressed(GameKeys.ACTION)) {
                chooseSound.play(getSoundVolume());
                editingMode = true;
            }
        }
    }

    private Future<IBattleResult> runBattle() {
        var teamAAI = getSelectedAI(selectedTeamAAI);
        var teamBAI = getSelectedAI(selectedTeamBAI);

        if (teamAAI == null || teamBAI == null) {
            System.out.println("Missing AI for one or more teams");
            return null;
        }

        // Currently just static list of monsters. Maybe create random teams, load from file or let user select teams
        List<IMonster> teamA = monsterSelector.createMonsterTeam(TEAM_MONSTER_AMOUNT);
        List<IMonster> teamB = monsterSelector.createMonsterTeam(TEAM_MONSTER_AMOUNT);

        var teamAPlayer = new BattleParticipant(teamA, true);
        var teamBPlayer = new BattleParticipant(teamB, false);

        var battleSim = new BattleSimulation();
        var processor = new BattleMonsterProcessor();
        battleSim.setMonsterProcessor(processor);
        battleSim.setPlayerAIFactory(teamAAI);
        battleSim.setOpponentAIFactory(teamBAI);
        battlingScene.setTeamAAIName(teamAAI.toString());
        battlingScene.setTeamBAIName(teamBAI.toString());

        battleSim.StartBattle(teamAPlayer, teamBPlayer);
        currentBattles++;
        return battleExecutor.submit(() -> {
            int turns = 0;
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                var event = battleSim.getNextBattleEvent();
                if (event instanceof VictoryBattleEvent victoryBattleEvent) {
                    var winner = victoryBattleEvent.getWinner() == battleSim.getState().getPlayer()
                            ? battleSim.getState().getPlayer()
                            : battleSim.getState().getEnemy();
                    var starter = battleSim.playerStarted() ? battleSim.getState().getPlayer() : battleSim.getState().getEnemy();
                    return new BattleResult(winner, battleSim.getState().getPlayer(), battleSim.getState().getEnemy(), starter, turns);
                } else if (event instanceof ChangeMonsterBattleEvent || event instanceof MoveBattleEvent) {
                    turns++;
                } else if (event instanceof AICrashedEvent) {
                    System.out.println("AI CRASHED!! Oh no! Anyway...");
                }
            }
        });

    }

    private Integer scrollIndex(int index, int scrollAmount, int max) {
        var out = (index + scrollAmount) % max;
        if (out <= -1) {
            out = index + scrollAmount + max;
        }
        return out;
    }

    @Override
    public void dispose() {
        menuMusic.stop();
        menuMusic.dispose();
        menuMusic = null;
        battleWaitMusic.stop();
        battleWaitMusic.dispose();
        battleWaitMusic = null;
    }

    public String toString() {
        return "Simulate Battles";
    }


    private float getSoundVolume() {
        return ((int) settings.getSetting(settingsRegistry.getSoundVolumeSetting()) / 100f);
    }

    private float getMusicVolume() {
        return ((int) settings.getSetting(settingsRegistry.getMusicVolumeSetting()) / 100f);
    }

    public void setSettings(IGameSettings settings) {
        this.settings = settings;
    }

    private IBattleAIFactory getSelectedAI(Integer index) {
        if (index == null || battleAIFactoryList.size() == 0) {
            return null;
        }

        return this.battleAIFactoryList.get(index % battleAIFactoryList.size());
    }

    public void addBattleAI(IBattleAIFactory battleAIFactory) {
        this.battleAIFactoryList.add(battleAIFactory);
    }

    public void setMonsterRegistry(IMonsterRegistry monsterRegistry) {
        this.monsterRegistry = monsterRegistry;
    }
}
