package dk.sdu.mmmi.modulemon;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import dk.sdu.mmmi.modulemon.common.OSGiFileHandleByteReader;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.data.GameKeys;
import dk.sdu.mmmi.modulemon.common.services.IBundleControllerService;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;
import dk.sdu.mmmi.modulemon.common.services.IGameViewService;
import dk.sdu.mmmi.modulemon.managers.GameInputManager;
import dk.sdu.mmmi.modulemon.managers.GameViewManager;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

public class Game implements ApplicationListener {
    public static int WIDTH;
    public static int HEIGHT;
    public static OrthographicCamera cam;
    private static Viewport viewport;
    private final GameData gameData = new GameData();
    private static GameViewManager gvm;
    private static List<IGameViewService> gameViewServiceList = new CopyOnWriteArrayList<>();
    private static IBundleControllerService bundleControllerService;
    private static Queue<Runnable> gdxThreadTasks = new LinkedList<>();

    private IGameSettings settings = null;

    public Game() {
        init();
    }


    public void init() {
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

        WIDTH = 1280;
        HEIGHT = 720;
        cfg.title = "Modulémon 2.0";
        cfg.addIcon("icons/cat-icon.png", Files.FileType.Classpath);
        cfg.width = WIDTH;
        cfg.height = HEIGHT;
        cfg.useGL30 = false;
        cfg.resizable = true;
        cfg.foregroundFPS = 60;
        cfg.vSyncEnabled = true;
        cfg.samples = 32; //Anti aliasing
        new LwjglApplication(this, cfg);
    }

    @Override
    public void create() {
        //Line below doesn't work yet, but just let it be - Alexander
        //Display.setIcon(hackIcon("/icons/cat-icon.png"));

        cam = new OrthographicCamera(WIDTH, HEIGHT);
        cam.setToOrtho(false, WIDTH, HEIGHT); // does the same as cam.translate()
        cam.update();
        Gdx.input.setInputProcessor(
                new GameInputManager(gameData)
        );

        viewport = new FitViewport(WIDTH, HEIGHT, cam);
        viewport.apply();

        gameData.setCamera(cam);
        gvm = new GameViewManager();
    }

    @Override
    public void render() {

        cam.update();
        gameData.setDisplayWidth(WIDTH);
        gameData.setDisplayHeight(HEIGHT);
        gameData.setDelta(Gdx.graphics.getDeltaTime());

        //Run tasks on the LibGDX thread for OSGi
        while (!gdxThreadTasks.isEmpty()) {
            gdxThreadTasks.poll().run();
        }

        gvm.update(gameData);
        gvm.draw(gameData);

        gameData.getKeys().update();

        update();
    }

    private void update() {
        // Update
        if (gameData.getKeys().isDown(GameKeys.K)
                && gameData.getKeys().isDown(GameKeys.LEFT_CTRL)
                && bundleControllerService != null) {
            Game.bundleControllerService.openController();
        }
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        if (Game.bundleControllerService != null) {
            Game.bundleControllerService.closeController();
        }
    }

    public void addGameViewServiceList(IGameViewService gameViewService) {
        System.out.println("GameService Loaded: " + gameViewService.getClass().getName());
        gameViewServiceList.add(gameViewService);
    }

    public void removeGameViewServiceList(IGameViewService gameViewService) {
        if (gvm.getCurrentGameView().equals(gameViewService)) {
            gdxThreadTasks.add(() -> gvm.setDefaultView());
            System.out.println("Threw player out of scene because it was unloaded!");
        }
        gameViewServiceList.remove(gameViewService);
    }

    public static List<IGameViewService> getGameViewServiceList() {
        return gameViewServiceList;
    }

    public void addBundleController(IBundleControllerService bundleControllerService) {
        System.out.println("A bundle controller was injected.");
        Game.bundleControllerService = bundleControllerService;
    }

    public void removeBundleController(IBundleControllerService bundleControllerService) {
        bundleControllerService.closeController();
        Game.bundleControllerService = null;
    }

    public void setSettingsService(IGameSettings settings) {
        this.settings = settings;
        gdxThreadTasks.add(() -> {
            if (settings.getSetting(SettingsRegistry.getInstance().getMusicVolumeSetting()) == null) {
                settings.setSetting(SettingsRegistry.getInstance().getMusicVolumeSetting(), 30);
            }
            if (settings.getSetting(SettingsRegistry.getInstance().getSoundVolumeSetting()) == null) {
                settings.setSetting(SettingsRegistry.getInstance().getSoundVolumeSetting(), 60);
            }
            if (settings.getSetting(SettingsRegistry.getInstance().getRectangleStyleSetting()) == null) {
                settings.setSetting(SettingsRegistry.getInstance().getRectangleStyleSetting(), false);
            }
            if (settings.getSetting(SettingsRegistry.getInstance().getAIProcessingTimeSetting()) == null) {
                settings.setSetting(SettingsRegistry.getInstance().getAIProcessingTimeSetting(), 1000);
            }
            if (settings.getSetting(SettingsRegistry.getInstance().getAIAlphaBetaSetting()) == null) {
                settings.setSetting(SettingsRegistry.getInstance().getAIAlphaBetaSetting(), true);
            }
            if (settings.getSetting(SettingsRegistry.getInstance().getAIKnowlegdeStateEnabled()) == null) {
                settings.setSetting(SettingsRegistry.getInstance().getAIKnowlegdeStateEnabled(), true);
            }
            if (settings.getSetting(SettingsRegistry.getInstance().getBattleMusicThemeSetting()) == null) {
                settings.setSetting(SettingsRegistry.getInstance().getBattleMusicThemeSetting(), "Original");
            }

            if (settings.getSetting(SettingsRegistry.getInstance().getBattleAISetting()) == null){
                settings.setSetting(SettingsRegistry.getInstance().getBattleAISetting(), "MCTS");
            }
            if(settings.getSetting(SettingsRegistry.getInstance().getNonDeterminism()) == null){
                settings.setSetting(SettingsRegistry.getInstance().getNonDeterminism(), true);
            }
            if(settings.getSetting(SettingsRegistry.getInstance().getConcurrentBattleAmount()) == null){
                settings.setSetting(SettingsRegistry.getInstance().getConcurrentBattleAmount(), 5);
            }
            gvm.setSettings(settings);
        });
    }

    public void removeSettingsService(IGameSettings settings) {
        this.settings = null;
    }

    private ByteBuffer[] hackIcon(String resourceName) {
        ByteBuffer[] byteBuffer = new ByteBuffer[1];
        Pixmap pixmap = new Pixmap(new OSGiFileHandleByteReader(resourceName, this.getClass()));
        if (pixmap.getFormat() != Pixmap.Format.RGBA8888) {
            Pixmap rgba = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGBA8888);
            rgba.drawPixmap(pixmap, 0, 0);
            pixmap = rgba;
        }

        byteBuffer[0] = ByteBuffer.allocateDirect(pixmap.getPixels().limit());
        byteBuffer[0].put(pixmap.getPixels()).flip();
        pixmap.dispose();
        return byteBuffer;
    }
}
