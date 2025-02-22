package dk.sdu.mmmi.modulemon.gameviews;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import dk.sdu.mmmi.modulemon.CommonBattleClient.IBattleView;
import dk.sdu.mmmi.modulemon.CommonBattleSimulation.IBattleAIFactory;
import dk.sdu.mmmi.modulemon.Game;
import dk.sdu.mmmi.modulemon.MCTSBattleAI.MCTSBattleAIFactory;
import dk.sdu.mmmi.modulemon.SimpleAI.BattleAIFactory;
import dk.sdu.mmmi.modulemon.common.AssetLoader;
import dk.sdu.mmmi.modulemon.common.OSGiFileHandle;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.data.GameKeys;
import dk.sdu.mmmi.modulemon.common.data.IGameViewManager;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;
import dk.sdu.mmmi.modulemon.common.services.IGameViewService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MenuView implements IGameViewService {

    /**
     * Creates the necessary variables used for custom fonts.
     */
    private GlyphLayout glyphLayout;
    private SpriteBatch spriteBatch;
    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private BitmapFont menuOptionsFont;
    private BitmapFont smallMenuOptionsFont;
    private BitmapFont settingsValueFont;
    private BitmapFont smallMenuFont;
    private Music menuMusic;

    private Texture logo;

    private String musicVolume = "";
    private String soundVolume = "";
    private String aiTime = "";
    private String battleTheme = "";
    private String AI = "";
    private int battleThemeIndex = 0;

    private String title = "";
    private String subtitle = "";

    private int currentOption;
    private MenuStates currentMenuState = MenuStates.DEFAULT;
    private String[] menuOptions;
    private String[] defaultMenuOptions = new String[]{
            "Play",
            "Settings",
            "Quit"
    };
    // The settings that will be shown under the "Settings" menu.
    private String[] settingOptions = new String[]{
            "Change Music Volume",
            "Change Sound Volume",
            "Use Persona Rectangles",
            "Use AI Alpha-beta pruning",
            "AI Processing Time",
            "Battle Music Theme",
            "AI",
            "Use AI knowledge states",
            "Nondeterminism (random battles)",
            "Concurrent battle amount"
    };

    private int AIIndex = 0;

    private String[] AIOptions = new String[]{
            "MCTS",
            "Simple",
            "Minimax"
    };

    private Sound selectSound;
    private Sound chooseSound;

    // The 4 different themes available to choose from.
    private String[] musicThemes = new String[]{
            "Original",
            "Pop",
            "Victory",
            "Orchestral",
    };

    private List<String> settingsValueList = new ArrayList<>();
    private boolean showSettings = false;

    private GameData gameData = new GameData();

    private IGameSettings settings;
    private SettingsRegistry settingsRegistry = SettingsRegistry.getInstance();

    public MenuView(IGameSettings settings) {
        this.settings = settings;
    }

    @Override
    public void init(IGameViewManager gameViewManager) {
        menuMusic = AssetLoader.getInstance().getMusicAsset("/music/menu.ogg", MenuView.class);
        // Instantiates the variables
        spriteBatch = new SpriteBatch();
        glyphLayout = new GlyphLayout();
        menuMusic.play();
        menuMusic.setVolume(0);
        menuMusic.setLooping(true);

        /*
          Sets up FontGenerator to enable us to use our own fonts.
         */
        FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(
                new OSGiFileHandle("/fonts/Modulemon-Solid.ttf", this.getClass())
        );
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        // Font size
        parameter.size = 80;

        // Sets the @titleFont to use our custom font file with the chosen font size
        titleFont = fontGenerator.generateFont(parameter);
        parameter.size = 24;
        subtitleFont = fontGenerator.generateFont(parameter);


        fontGenerator = new FreeTypeFontGenerator(
                new OSGiFileHandle("/fonts/Roboto-Medium.ttf", this.getClass()));
        // Font size
        parameter.size = 34;

        // Sets the @menuOptionsFont to use our custom font file with the chosen font size
        menuOptionsFont = fontGenerator.generateFont(parameter);

        parameter.size = 20;
        smallMenuFont = fontGenerator.generateFont(parameter);
        smallMenuOptionsFont = fontGenerator.generateFont(parameter);
        settingsValueFont = fontGenerator.generateFont(parameter);

        fontGenerator.dispose();

        logo = AssetLoader.getInstance().getTextureAsset("/icons/cat-logo.png", this.getClass());
        selectSound = AssetLoader.getInstance().getSoundAsset("/sounds/select.ogg", this.getClass());
        chooseSound = AssetLoader.getInstance().getSoundAsset("/sounds/choose.ogg", this.getClass());

        // Sets the options for the menu
        menuOptions = defaultMenuOptions;

        settingsInitializer();
    }

    @Override
    public void update(GameData gameData, IGameViewManager gameViewManager) {
        if (currentMenuState == MenuStates.SELECTING_GAMEVIEW) {
            title = "Select GamévieW";
            subtitle = "Choose where to start!";
            List<IGameViewService> gameViews = Game.getGameViewServiceList();
            menuOptions = new String[gameViews.size() + 1];

            for (int i = 0; i < gameViews.size(); i++) {
                menuOptions[i] = gameViews.get(i).toString();
            }
            menuOptions[menuOptions.length - 1] = "GO BACK";
        } else if (currentMenuState == MenuStates.SETTINGS) {
            title = "Settings";
            subtitle = "";
            menuOptions = new String[settingOptions.length + 1];
            for (int i = 0; i < settingOptions.length; i++) {
                menuOptions[i] = settingOptions[i];
            }
            menuOptions[settingOptions.length] = "GO BACK";
        } else {
            //Default
            menuOptions = defaultMenuOptions;
            title = "ModulémoN 2.0";
            subtitle = "Advanced AI for Gamez edition";
        }
        // Compares the value of the volume option to the correct value in the settings.json file
        if (menuMusic.getVolume() != getMusicVolumeAsFloat()) {
            // Sets the volume to the one found in the json file
            menuMusic.setVolume(getMusicVolumeAsFloat());
        }
    }

    @Override
    public void draw(GameData data) {
        // clear screen to black
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        spriteBatch.setProjectionMatrix(Game.cam.combined);

        spriteBatch.begin();

        glyphLayout.setText(titleFont, title);

        spriteBatch.draw(logo, (Game.WIDTH) / 2.2f, (Game.HEIGHT - glyphLayout.height) / 1.2f);

        titleFont.setColor(Color.valueOf("ffcb05"));
        titleFont.draw(
                spriteBatch,
                title,
                (Game.WIDTH - glyphLayout.width) / 2,
                (Game.HEIGHT - glyphLayout.height) / 1.25f
        );

        if (!subtitle.isBlank()) {
            subtitleFont.setColor(Color.valueOf("ffcb05"));
            glyphLayout.setText(subtitleFont, subtitle);
            subtitleFont.draw(
                    spriteBatch,
                    subtitle,
                    (Game.WIDTH - glyphLayout.width) / 2,
                    ((Game.HEIGHT - glyphLayout.height) / 1.25f) - titleFont.getXHeight() - titleFont.getCapHeight() - 10f);
        }

        // If showSettings is true, it should draw with a smaller font, i.e. smallMenuOptionsFont.
        // And also draw the values of the appertaining settings (On / Off, volume level, etc.)
        if (showSettings) {
            for (int i = 0; i < settingsValueList.size(); i++) {
                glyphLayout.setText(settingsValueFont, settingsValueList.get(i));
                settingsValueFont.draw(
                        spriteBatch,
                        settingsValueList.get(i),
                        (Game.WIDTH - glyphLayout.width) / 1.55f,
                        (Game.HEIGHT - 60 * (i)) / 2f
                );
            }
            for (int i = 0; i < menuOptions.length; i++) {
                glyphLayout.setText(smallMenuOptionsFont, menuOptions[i]);
                if (currentOption == i) smallMenuOptionsFont.setColor(Color.valueOf("2a75bb"));
                else smallMenuOptionsFont.setColor(Color.WHITE);
                smallMenuOptionsFont.draw(
                        spriteBatch,
                        menuOptions[i],
                        (Game.WIDTH - glyphLayout.width) / 2f,
                        (Game.HEIGHT - 60 * i) / 2f
                );
            }
        }
        /*
         * Runs through the entire menuOptions array and draws them all.
         * Also sets the currently selected option to a custom color
         */
        else {
            for (int i = 0; i < menuOptions.length; i++) {
                glyphLayout.setText(menuOptionsFont, menuOptions[i]);
                if (currentOption == i) menuOptionsFont.setColor(Color.valueOf("2a75bb"));
                else menuOptionsFont.setColor(Color.WHITE);
                menuOptionsFont.draw(
                        spriteBatch,
                        menuOptions[i],
                        (Game.WIDTH - glyphLayout.width) / 2f,
                        (Game.HEIGHT - 100 * i) / 2f
                );
            }
        }

//        glyphLayout.setText(smallMenuFont, "Press CTRL+K to open the Bundle Controller (if loaded)");
//        smallMenuFont.draw(
//                spriteBatch,
//                "Press CTRL+K to open the Bundle Controller (if loaded)",
//                (Game.WIDTH - glyphLayout.width) / 2f,
//                40
//        );

        spriteBatch.end();
    }

    @Override
    public void handleInput(GameData gameData, IGameViewManager gameViewManager) {
        if (gameData.getKeys().isPressed(GameKeys.BACK)) {
            goBackToMainMenu();
        }
        // Moves up in the menu
        if (gameData.getKeys().isPressed(GameKeys.UP)) {
            if (currentOption > 0) {
                currentOption--;
            } else {
                currentOption = menuOptions.length - 1;
            }
            selectSound.play(getSoundVolumeAsFloat());
        }
        // Moves down in the menu
        if (gameData.getKeys().isPressed(GameKeys.DOWN)) {
            if (currentOption < menuOptions.length - 1) {
                currentOption++;
            } else {
                currentOption = 0;
            }
            selectSound.play(getSoundVolumeAsFloat());
        }

        if (gameData.getKeys().isPressed(GameKeys.LEFT)) {
            handleSettings("LEFT");
        }
        if (gameData.getKeys().isPressed(GameKeys.RIGHT)) {
            handleSettings("RIGHT");
        }
        // Selects the current option
        if (gameData.getKeys().isPressed(GameKeys.ACTION) || gameData.getKeys().isPressed(GameKeys.E)) {
            handleSettings("ACTION");
            selectOption(gameViewManager);
        }
    }

    /**
     * Handler for when an option is selected.
     * Based on what the currentOption is, it will execute the appertaining code.
     */
    private void selectOption(IGameViewManager gvm) {
        if (menuOptions[currentOption].equalsIgnoreCase("GO BACK")) {
            goBackToMainMenu();
            return;
        }


        if (currentMenuState == MenuStates.SELECTING_GAMEVIEW) {
            List<IGameViewService> views = Game.getGameViewServiceList();
            if (currentOption > views.size()) {
                System.out.println("ERROR: Tried to set invalid view");
                currentOption = 0;
            }
            IGameViewService selectedView = views.get(currentOption);
            gvm.setView(selectedView);
            if (selectedView instanceof IBattleView battleView) {
                chooseSound.play(getSoundVolumeAsFloat());

                battleView.startBattle(null, null, null);
            }
        } else {
            if (Objects.equals(menuOptions[currentOption], "Play")) {
                currentMenuState = MenuStates.SELECTING_GAMEVIEW;
                currentOption = 0;
                chooseSound.play(getSoundVolumeAsFloat());
            }
            if (Objects.equals(menuOptions[currentOption], "Settings")) {
                currentMenuState = MenuStates.SETTINGS;
                showSettings = true;
                chooseSound.play(getSoundVolumeAsFloat());
            }
            if (Objects.equals(menuOptions[currentOption], "Quit")) {
                chooseSound.play(getSoundVolumeAsFloat());
                Gdx.app.exit();
            }
        }
    }

    private void goBackToMainMenu() {
        currentMenuState = MenuStates.DEFAULT;
        currentOption = 0;
        showSettings = false;
        chooseSound.play(getSoundVolumeAsFloat());
        return;
    }

    /**
     * Method to handle what logic should happen in the Settings menu. It checks the currently selected/hovered setting
     * and executes the code relevant to that. It takes the parameter keyInput.
     *
     * @param keyInput declares what kind of key was pressed such that relevant logic can be applied. For instance, if you
     *                 want to increase something when pressing right and decrease when pressing left.
     */
    private void handleSettings(String keyInput) {
        if (settings != null) {
            switch (keyInput) {
                case "RIGHT": {
                    if (menuOptions[currentOption].equalsIgnoreCase("Change Music Volume")) {
                        // Max volume is 100, which is 1f, since that's the max volume libgdx music and sound can use.
                        if (!((int) settings.getSetting(settingsRegistry.getMusicVolumeSetting()) >= 100)) {
                            // Increases volume by 10 (0.1f)
                            int new_volume = (int) settings.getSetting(settingsRegistry.getMusicVolumeSetting()) + 10;
                            settings.setSetting(settingsRegistry.getMusicVolumeSetting(), new_volume);

                            musicVolume = (int) settings.getSetting(settingsRegistry.getMusicVolumeSetting()) + "%";
                            settingsValueList.set(0, musicVolume);
                            chooseSound.play(getSoundVolumeAsFloat());
                        }
                        break;
                    }

                    if (menuOptions[currentOption].equalsIgnoreCase("Change Sound Volume")) {
                        // Max volume is 100, which is 1f, since that's the max volume libgdx music and sound can use.
                        if (!((int) settings.getSetting(settingsRegistry.getSoundVolumeSetting()) >= 100)) {
                            // Increases volume by 10 (0.1f)
                            int new_volume = (int) settings.getSetting(settingsRegistry.getSoundVolumeSetting()) + 10;
                            settings.setSetting(settingsRegistry.getSoundVolumeSetting(), new_volume);

                            soundVolume = (int) settings.getSetting(settingsRegistry.getSoundVolumeSetting()) + "%";
                            settingsValueList.set(1, soundVolume);
                            chooseSound.play(getSoundVolumeAsFloat());
                        }
                        break;
                    }

                    if (menuOptions[currentOption].equalsIgnoreCase("AI Processing Time")) {
                        // Sets the max processing time to 10 seconds
                        if (!((int) settings.getSetting(settingsRegistry.getAIProcessingTimeSetting()) >= 10000)) {
                            // Increases the processing time by 100 ms
                            int ai_processing_time = (int) settings.getSetting(settingsRegistry.getAIProcessingTimeSetting()) + 100;
                            settings.setSetting(settingsRegistry.getAIProcessingTimeSetting(), ai_processing_time);

                            aiTime = (int) settings.getSetting(settingsRegistry.getAIProcessingTimeSetting()) + " ms";
                            settingsValueList.set(4, aiTime);
                            chooseSound.play(getSoundVolumeAsFloat());
                        }
                        break;
                    }

                    if (menuOptions[currentOption].equalsIgnoreCase("AI")) {
                        AIIndex++;

                        AIIndex = AIIndex % AIOptions.length;
                        settings.setSetting(settingsRegistry.getBattleAISetting(), AIOptions[AIIndex]);

                        AI = (String) settings.getSetting(settingsRegistry.getBattleAISetting());
                        settingsValueList.set(6, AI);

                        chooseSound.play(getSoundVolumeAsFloat());
                        break;
                    }

                    if (menuOptions[currentOption].equalsIgnoreCase("Battle Music Theme")) {
                        battleThemeIndex++;

                        //Circular Array to ensure it loops over the full list indefinitely
                        battleThemeIndex = battleThemeIndex % musicThemes.length;
                        // Sets the theme setting to be the one selected from the musicThemes array, based on the current index
                        settings.setSetting(settingsRegistry.getBattleMusicThemeSetting(), musicThemes[battleThemeIndex]);

                        // Updates the name of the theme to the newly selected one
                        battleTheme = (String) settings.getSetting(settingsRegistry.getBattleMusicThemeSetting());
                        settingsValueList.set(5, battleTheme);

                        chooseSound.play(getSoundVolumeAsFloat());
                        break;
                    }

                    if (menuOptions[currentOption].equalsIgnoreCase("Concurrent battle amount")) {
                        // Sets the maximum concurrent battles to 10
                        if (((int) settings.getSetting(settingsRegistry.getConcurrentBattleAmount()) < 16)) {
                            // increases the concurrent battles by 1
                            int concurrentBattles = (int) settings.getSetting(settingsRegistry.getConcurrentBattleAmount()) + 1;
                            settings.setSetting(settingsRegistry.getConcurrentBattleAmount(), concurrentBattles);

                            settingsValueList.set(9, String.valueOf(concurrentBattles));
                            chooseSound.play(getSoundVolumeAsFloat());
                        }
                        break;
                    }

                    boolean_settings_switch_on_off();
                    break;
                }
                case "LEFT": {
                    if (menuOptions[currentOption].equalsIgnoreCase("Change Music Volume")) {
                        // Min volume is 0, which essentially mutes all the music.
                        if (((int) settings.getSetting(settingsRegistry.getMusicVolumeSetting()) > 0)) {
                            // Decreases volume by 10 (0.1f)
                            int new_volume = (int) settings.getSetting(settingsRegistry.getMusicVolumeSetting()) - 10;
                            settings.setSetting(settingsRegistry.getMusicVolumeSetting(), new_volume);

                            musicVolume = (int) settings.getSetting(settingsRegistry.getMusicVolumeSetting()) + "%";
                            settingsValueList.set(0, musicVolume);
                            chooseSound.play(getSoundVolumeAsFloat());
                        }
                        break;
                    }

                    if (menuOptions[currentOption].equalsIgnoreCase("Change Sound Volume")) {
                        // Min volume is 0, which essentially mutes all the sound.
                        if (((int) settings.getSetting(settingsRegistry.getSoundVolumeSetting()) > 0)) {
                            // Decreases volume by 10 (0.1f)
                            int new_volume = (int) settings.getSetting(settingsRegistry.getSoundVolumeSetting()) - 10;
                            settings.setSetting(settingsRegistry.getSoundVolumeSetting(), new_volume);

                            soundVolume = (int) settings.getSetting(settingsRegistry.getSoundVolumeSetting()) + "%";
                            settingsValueList.set(1, soundVolume);
                            chooseSound.play(getSoundVolumeAsFloat());
                        }
                        break;
                    }

                    if (menuOptions[currentOption].equalsIgnoreCase("AI Processing Time")) {
                        // Sets the minimum processing time to 500 ms
                        if (((int) settings.getSetting(settingsRegistry.getAIProcessingTimeSetting()) > 100)) {
                            // Decreases the processing time by 100 ms
                            int ai_processing_time = (int) settings.getSetting(settingsRegistry.getAIProcessingTimeSetting()) - 100;
                            settings.setSetting(settingsRegistry.getAIProcessingTimeSetting(), ai_processing_time);

                            aiTime = (int) settings.getSetting(settingsRegistry.getAIProcessingTimeSetting()) + " ms";
                            settingsValueList.set(4, aiTime);
                            chooseSound.play(getSoundVolumeAsFloat());
                        }
                        break;
                    }


                    if (menuOptions[currentOption].equalsIgnoreCase("AI")) {
                        if (AIIndex == 0) {
                            AIIndex = AIOptions.length - 1;
                        } else {
                            AIIndex--;
                        }

                        settings.setSetting(settingsRegistry.getBattleAISetting(), AIOptions[AIIndex]);

                        AI = (String) settings.getSetting(settingsRegistry.getBattleAISetting());
                        settingsValueList.set(6, AI);

                        chooseSound.play(getSoundVolumeAsFloat());
                        break;
                    }

                    if (menuOptions[currentOption].equalsIgnoreCase("Battle Music Theme")) {
                        // If the index is at the start, go to the back of the array, ensuring it can loop indefinitely
                        if (battleThemeIndex == 0) {
                            battleThemeIndex = musicThemes.length - 1;
                        } else {
                            battleThemeIndex--;
                        }
                        // Sets the theme setting to be the one selected from the musicThemes array, based on the current index
                        settings.setSetting(settingsRegistry.getBattleMusicThemeSetting(), musicThemes[battleThemeIndex]);

                        // Updates the name of the theme to the newly selected one
                        battleTheme = (String) settings.getSetting(settingsRegistry.getBattleMusicThemeSetting());
                        settingsValueList.set(5, battleTheme);

                        chooseSound.play(getSoundVolumeAsFloat());
                        break;
                    }

                    if (menuOptions[currentOption].equalsIgnoreCase("Concurrent battle amount")) {
                        // Sets the minimum concurrent battles to 1
                        if (((int) settings.getSetting(settingsRegistry.getConcurrentBattleAmount()) > 1)) {
                            // Decreases the concurrent battles by 1
                            int concurrentBattles = (int) settings.getSetting(settingsRegistry.getConcurrentBattleAmount()) - 1;
                            settings.setSetting(settingsRegistry.getConcurrentBattleAmount(), concurrentBattles);

                            settingsValueList.set(9, String.valueOf(concurrentBattles));
                            chooseSound.play(getSoundVolumeAsFloat());
                        }
                        break;
                    }


                    boolean_settings_switch_on_off();
                    break;
                }
                case "ACTION": {
                    boolean_settings_switch_on_off();
                    break;
                }
            }
        }
    }

    /**
     * Method to change the value of a boolean Setting. Switches between true and false which changes the value
     * displayed on screen to either "On" or "Off".
     */
    private void boolean_settings_switch_on_off() {
        if (settings != null) {
            // Checks if the currently selected/hovered setting is the option for Persona Rectangles
            if (menuOptions[currentOption].equalsIgnoreCase("Use Persona Rectangles")) {
            /*
            If setting for using PersonaRectangles is currently true, change it to false
            Otherwise change it to true.
             */
                if (((Boolean) settings.getSetting(settingsRegistry.getRectangleStyleSetting()))) {
                    settingsValueList.set(2, "Off");
                    settings.setSetting(settingsRegistry.getRectangleStyleSetting(), false);
                } else {
                    settingsValueList.set(2, "On");
                    settings.setSetting(settingsRegistry.getRectangleStyleSetting(), true);
                }
                chooseSound.play(getSoundVolumeAsFloat());
            } else if (menuOptions[currentOption].equalsIgnoreCase("Use AI Alpha-beta pruning")) {
            /*
            If setting for using Alpha-beta pruning is currently true, change it to false
            Otherwise change it to true.
             */
                if (((Boolean) settings.getSetting(settingsRegistry.getAIAlphaBetaSetting()))) {
                    settingsValueList.set(3, "Off");
                    settings.setSetting(settingsRegistry.getAIAlphaBetaSetting(), false);
                } else {
                    settingsValueList.set(3, "On");
                    settings.setSetting(settingsRegistry.getAIAlphaBetaSetting(), true);
                }
                chooseSound.play(getSoundVolumeAsFloat());
            } else if (menuOptions[currentOption].equalsIgnoreCase("Use AI knowledge states")) {
                /*
                If setting for using knowledge states in the AI,
                 */
                if (((Boolean) settings.getSetting(settingsRegistry.getAIKnowlegdeStateEnabled()))) {
                    settingsValueList.set(7, "Off");
                    settings.setSetting(settingsRegistry.getAIKnowlegdeStateEnabled(), false);
                } else {
                    settingsValueList.set(7, "On");
                    settings.setSetting(settingsRegistry.getAIKnowlegdeStateEnabled(), true);
                }
                chooseSound.play(getSoundVolumeAsFloat());
            } else if (menuOptions[currentOption].equalsIgnoreCase("Nondeterminism (random battles)")) {
                /*
                If setting for using nondeterminism for battles,
                 */
                if (((Boolean) settings.getSetting(settingsRegistry.getNonDeterminism()))) {
                    settingsValueList.set(8, "Off");
                    settings.setSetting(settingsRegistry.getNonDeterminism(), false);
                } else {
                    settingsValueList.set(8, "On");
                    settings.setSetting(settingsRegistry.getNonDeterminism(), true);
                }
                chooseSound.play(getSoundVolumeAsFloat());
            }
        }
    }

    /**
     * This method gets called when this state is first loaded. It initializes the settings menu values with
     * those found in the settings.json file.
     */
    private void settingsInitializer() {
        if (settings != null) {
            musicVolume = settings.getSetting(settingsRegistry.getMusicVolumeSetting()) + "%";
            soundVolume = settings.getSetting(settingsRegistry.getSoundVolumeSetting()) + "%";
            settingsValueList.add(musicVolume);
            settingsValueList.add(soundVolume);

            settingsValueList.add((Boolean) settings.getSetting(settingsRegistry.getRectangleStyleSetting()) ? "On" : "Off");
            settingsValueList.add((Boolean) settings.getSetting(settingsRegistry.getAIAlphaBetaSetting()) ? "On" : "Off");

            aiTime = settings.getSetting(settingsRegistry.getAIProcessingTimeSetting()) + " ms";
            settingsValueList.add(aiTime);

            battleTheme = (String) settings.getSetting(settingsRegistry.getBattleMusicThemeSetting());
            settingsValueList.add(battleTheme);
            battleThemeIndex = Arrays.asList(musicThemes).indexOf(battleTheme); // Sets the music theme index to the position of the currently selected theme found in the settings file

            AI = (String) settings.getSetting(settingsRegistry.getBattleAISetting());
            settingsValueList.add(AI);

            settingsValueList.add((Boolean) settings.getSetting(settingsRegistry.getAIKnowlegdeStateEnabled()) ? "On" : "Off");
            AIIndex = Arrays.asList(AIOptions).indexOf(AI);
            settingsValueList.add((Boolean) settings.getSetting(settingsRegistry.getNonDeterminism()) ? "On" : "Off");
            var battleAmount = String.valueOf(settings.getSetting(settingsRegistry.getConcurrentBattleAmount()));
            settingsValueList.add(battleAmount);
        }
    }


    // Gets the sound volume from the settings file and returns it as a float
    private float getSoundVolumeAsFloat() {

        if (settings != null) {
            return (int) settings.getSetting(settingsRegistry.getSoundVolumeSetting()) / 100f;
        } else return 0.6f;
    }

    // Gets the music volume from the settings file and returns it as a float
    private float getMusicVolumeAsFloat() {
        if (settings != null) {
            return (int) settings.getSetting(settingsRegistry.getMusicVolumeSetting()) / 100f;
        } else return 0.3f;
    }

    @Override
    public void dispose() {
        menuMusic.stop();
        menuMusic.dispose();
        menuMusic = null;
    }

    private enum MenuStates {
        DEFAULT,
        SELECTING_GAMEVIEW,
        SETTINGS
    }
}
