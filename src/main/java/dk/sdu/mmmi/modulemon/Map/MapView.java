package dk.sdu.mmmi.modulemon.Map;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import dk.sdu.mmmi.modulemon.CommonBattleClient.IBattleView;
import dk.sdu.mmmi.modulemon.CommonMap.Data.Entity;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.MonsterTeamPart;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.PositionPart;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.SpritePart;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityType;
import dk.sdu.mmmi.modulemon.CommonMap.Data.World;
import dk.sdu.mmmi.modulemon.CommonMap.IMapEvent;
import dk.sdu.mmmi.modulemon.CommonMap.IMapView;
import dk.sdu.mmmi.modulemon.CommonMap.Services.IEntityProcessingService;
import dk.sdu.mmmi.modulemon.CommonMap.Services.IGamePluginService;
import dk.sdu.mmmi.modulemon.CommonMap.Services.IPostEntityProcessingService;
import dk.sdu.mmmi.modulemon.CommonMap.TextMapEvent;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.common.AssetLoader;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.data.GameKeys;
import dk.sdu.mmmi.modulemon.common.data.IGameViewManager;
import dk.sdu.mmmi.modulemon.common.drawing.*;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;
import dk.sdu.mmmi.modulemon.common.services.IGameViewService;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


public class MapView implements IGameViewService, IMapView {
    private TiledMap tiledMap;
    private TiledMapTileLayer overhangLayer;
    private BatchTiledMapRenderer tiledMapRenderer;
    private OrthographicCamera cam;
    private ShapeRenderer shapeRenderer;
    private Music mapMusic;
    private TextUtils textUtils;
    private Color switchIndicatorColor = Color.BLACK;
    private Rectangle pauseMenu;
    private Rectangle monsterTeamMenu;
    private Rectangle teamActionMenu;
    private Rectangle summaryMenu;
    private Rectangle[] monsterRectangles = new Rectangle[6];
    private String pauseMenuTitle = "GAME PAUSED";
    private String[] pauseActions = new String[]{"Resume", "Team", "Inventory", "Quit"};
    private String[] teamActions = new String[]{"Summary", "Switch", "Cancel"};
    private List<IMonster> monsterTeam = new ArrayList<>();
    private float mapLeft;
    private float mapRight;
    private float mapBottom;
    private float mapTop;
    private float playerPosX;
    private float playerPosY;
    private int tilePixelSize;
    private int selectedOptionIndex = 0;
    private int selectedOptionIndexMonsterTeam = 0;
    private int teamOptionIndex = 0;
    private int firstSelected = -1; // Default case = nothing selected
    private int secondSelected = -1; // Default case = nothing selected
    private int temporarySecondSelected = -1; // Default case = nothing selected
    private boolean currentlySwitching = false;
    private boolean showSwitchingText = false;
    private boolean isPaused;
    private boolean showMonsterTeam;
    private boolean showTeamOptions;
    private boolean showSummary;
    private MonsterTeamPart mtp;
    private SpriteBatch spriteBatch;
    private AssetLoader loader = AssetLoader.getInstance();
    private SettingsRegistry settingsRegistry = SettingsRegistry.getInstance();
    private static World world = new World();
    private final GameData gameData = new GameData();
    private static final List<IEntityProcessingService> entityProcessorList = new CopyOnWriteArrayList<>();
    private static final List<IPostEntityProcessingService> postProcessingList = new CopyOnWriteArrayList<>();
    private static final List<IGamePluginService> gamePluginList = new CopyOnWriteArrayList<>();
    private static Queue<Runnable> gdxThreadTasks = new LinkedList<>();
    private static Queue<IMapEvent> mapEvents = new LinkedList<>();
    private IGameViewManager gameViewManager;
    private IBattleView battleView;
    private Entity player;
    private Class<? extends Rectangle> rectToUse = Rectangle.class;
    private IGameSettings settings;

    @Override
    public void init(IGameViewManager gameViewManager) {
        mapMusic = loader.getMusicAsset("/music/village_theme.ogg", MapView.class);
        tiledMap = new TmxMapLoader().load("maps/ForestOverworld.tmx");
        overhangLayer = (TiledMapTileLayer) tiledMap.getLayers().get("Top");
        int scale = 4;
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap, scale);
        mapMusic.play();
        mapMusic.setLooping(true);

        // Pixel size
        tilePixelSize = 16 * scale;

        // Sprites
        spriteBatch = new SpriteBatch();

        // Pausing
        isPaused = false;
        showMonsterTeam = false;
        showTeamOptions = false;
        for (int i = 0; i < monsterRectangles.length; i++) {
            Rectangle rect = new Rectangle(100, 100, 320, 70);
            monsterRectangles[i] = rect;
        }
        shapeRenderer = new ShapeRenderer();
        gdxThreadTasks.add(() -> textUtils = TextUtils.getInstance());

        // Battle
        this.gameViewManager = gameViewManager;
    }

    private void initializeCameraDrawing(GameData gameData) {
        cam = gameData.getCamera();

        // Setting bounds for map
        MapProperties properties = tiledMap.getProperties();
        int mapWidth = properties.get("width", Integer.class);
        int mapHeight = properties.get("height", Integer.class);
        mapLeft = (cam.viewportWidth / 2f);
        mapRight = mapWidth * tilePixelSize - (cam.viewportWidth / 2f);
        mapBottom = 360;
        mapTop = mapBottom + mapHeight * tilePixelSize - (cam.viewportWidth / 2f) - 80;
        cam.position.set(mapRight / 2f, mapTop / 2f, 0);
    }

    @Override
    public void update(GameData gameData, IGameViewManager gameViewManager) {
        while (!gdxThreadTasks.isEmpty()) {
            gdxThreadTasks.poll().run();
        }

        if (!mapEvents.isEmpty()) {
            mapEvents.peek().update(gameData);
            if (mapEvents.peek().isEventDone()) {
                mapEvents.poll();
            } else {
                return; // Dont update anything else.
            }
        }
        if(!gameData.isPaused()){
            for (IEntityProcessingService entityProcessorService : entityProcessorList) {
                entityProcessorService.process(gameData, world);
            }

            for (IPostEntityProcessingService postProcessingService : postProcessingList) {
                postProcessingService.process(gameData, world);
            }
        }

        if (settings != null) {
            if (mapMusic.getVolume() != (int) settings.getSetting(SettingsRegistry.getInstance().getMusicVolumeSetting()) / 100f) {
                mapMusic.setVolume((int) settings.getSetting(SettingsRegistry.getInstance().getMusicVolumeSetting()) / 100f);
            }

            if ((Boolean) settings.getSetting(SettingsRegistry.getInstance().getRectangleStyleSetting()) && !(pauseMenu instanceof PersonaRectangle)) {
                rectToUse = PersonaRectangle.class;
                summaryMenu = DrawingUtils.createRectangle(rectToUse, 100, 100, 380, 300);
                pauseMenu = DrawingUtils.createRectangle(rectToUse, 100, 100, 200, 250);
                monsterTeamMenu = DrawingUtils.createRectangle(rectToUse, 100, 100, 400, 550);
                teamActionMenu = DrawingUtils.createRectangle(rectToUse, 100, 100, 200, 200);
            } else if (!(Boolean) settings.getSetting(SettingsRegistry.getInstance().getRectangleStyleSetting()) && (pauseMenu instanceof PersonaRectangle || pauseMenu == null)) {
                rectToUse = Rectangle.class;
                summaryMenu = DrawingUtils.createRectangle(rectToUse, 100, 100, 380, 300);
                pauseMenu = DrawingUtils.createRectangle(rectToUse, 100, 100, 200, 250);
                monsterTeamMenu = DrawingUtils.createRectangle(rectToUse, 100, 100, 400, 550);
                teamActionMenu = DrawingUtils.createRectangle(rectToUse, 100, 100, 200, 200);
            }
        } else {
            mapMusic.setVolume(0.3f);
            summaryMenu = DrawingUtils.createRectangle(Rectangle.class, 100, 100, 380, 300);
            pauseMenu = DrawingUtils.createRectangle(Rectangle.class, 100, 100, 200, 250);
            monsterTeamMenu = DrawingUtils.createRectangle(Rectangle.class, 100, 100, 400, 550);
            teamActionMenu = DrawingUtils.createRectangle(Rectangle.class, 100, 100, 200, 200);
        }
    }

    private List<Rectangle> collisionRectangles = null;

    @Override
    public void draw(GameData gameData) {
        if (cam == null)
            initializeCameraDrawing(gameData);
        tiledMapRenderer.setView(cam);
        tiledMapRenderer.render();

        for (Entity entity : world.getEntities()) {
            SpritePart spritePart = entity.getPart(SpritePart.class);
            if (spritePart != null && spritePart.getCurrentSprite() != null) {
                Texture sprite = spritePart.getCurrentSprite();

                spriteBatch.setProjectionMatrix(cam.combined);
                spriteBatch.begin();
                PositionPart positionPart = entity.getPart(PositionPart.class);
                var visualEntityPosition = positionPart.getVisualPos();
                spriteBatch.draw(sprite, visualEntityPosition.x, visualEntityPosition.y);
                spriteBatch.end();

                if (entity.getType().equals(EntityType.PLAYER)) {
                    playerPosX = visualEntityPosition.x;
                    playerPosY = visualEntityPosition.y;
                }
            }
        }
        tiledMapRenderer.getBatch().begin();
        tiledMapRenderer.renderTileLayer(overhangLayer);
        tiledMapRenderer.getBatch().end();

        //Re-center camera
        /*
        The value of the leftmost position of the camera and the rightmost position of the camera
        gets compared to the player's position. If the player is too far to the left or the right,
        the camera will be "locked" in place at the leftmost or rightmost legal position,
        such that it does not go out of bounds. The same applies to the top and bottom positions.
         */
        var camTargetX = Math.min(Math.max(mapLeft, playerPosX), mapRight);
        var camTargetY = Math.min(Math.max(mapBottom, playerPosY), mapTop);
        cam.position.set(camTargetX,camTargetY,0);
        cam.update();


        // DEBUG DRAWING
        if(gameData.getKeys().isDown(GameKeys.K)){
            if(collisionRectangles == null){
                // Generate and cache them
                collisionRectangles = new ArrayList<>();

                TiledMapTileLayer collsionLayer = (TiledMapTileLayer) tiledMap.getLayers().get(0);
                for(int height = 0; height < collsionLayer.getHeight(); height++){
                    for(int width = 0; width < collsionLayer.getWidth(); width++){
                        var cell = collsionLayer.getCell(height, width);
                        if(cell.getTile().getProperties().containsKey("blocked")){
                            // Cell is blocked
                            var newRect = new Rectangle(height*tilePixelSize, width* tilePixelSize, tilePixelSize, tilePixelSize);
                            newRect.setFillColor(Color.RED);
                            collisionRectangles.add(newRect);
                        }
                    }
                }
            }
            shapeRenderer.setAutoShapeType(true);
            shapeRenderer.setProjectionMatrix(cam.combined);
            shapeRenderer.begin();
            collisionRectangles.forEach(x -> x.draw(shapeRenderer, gameData.getDelta()));
            shapeRenderer.end();
        }

        // Draw events
        if (!mapEvents.isEmpty()) {
            mapEvents.peek().draw(gameData, spriteBatch, shapeRenderer);
        }

        //DRAW MENU START
        if (showMonsterTeam) {
            for (Entity entity : world.getEntities()) {
                if (entity.getType().equals(EntityType.PLAYER)) {
                    mtp = entity.getPart(MonsterTeamPart.class);
                    if (mtp == null) {
                        showMonsterTeam = false;
                        pauseMenu.setFillColor(Color.WHITE);
                        break;
                    }
                    monsterTeam = mtp.getMonsterTeam();

                    MonsterTeam.drawMonsterTeam(gameData, shapeRenderer, spriteBatch, textUtils, cam, showSwitchingText, monsterTeamMenu, monsterTeam, monsterRectangles);
                    break;
                }
            }
        }

        if (showTeamOptions) {
            if (mtp == null) {
                pauseMenu.setFillColor(Color.WHITE);
                showTeamOptions = false;
                for (Rectangle monsterRectangle : monsterRectangles) {
                    monsterRectangle.setBorderColor(Color.BLACK);
                }
                teamOptionIndex = 0;
            } else {
                MonsterTeam.drawTeamOptions(gameData, shapeRenderer, spriteBatch, textUtils, teamActionMenu, monsterTeamMenu, teamActions);
            }
        }

        if (showSummary) {
            if (mtp == null) {
                showSummary = false;
                for (Rectangle monsterRectangle : monsterRectangles) {
                    monsterRectangle.setFillColor(Color.WHITE);
                    monsterRectangle.setBorderColor(Color.BLACK);
                }
                monsterTeamMenu.setFillColor(Color.WHITE);
                teamActionMenu.setFillColor(Color.WHITE);
                pauseMenu.setFillColor(Color.WHITE);
                teamOptionIndex = 0;
            } else {
                MonsterTeam.drawSummary(gameData, shapeRenderer, spriteBatch, textUtils, summaryMenu, monsterTeamMenu, mtp, selectedOptionIndexMonsterTeam);
            }
        }


        if (isPaused) {
            //Drawing pause menu box
            shapeRenderer.setAutoShapeType(true);
            shapeRenderer.setProjectionMatrix(cam.combined);
            shapeRenderer.setColor(Color.WHITE);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            pauseMenu.setX(cam.position.x + cam.viewportWidth / 3f);
            pauseMenu.setY(cam.position.y - cam.viewportHeight / 8f);
            pauseMenu.draw(shapeRenderer, gameData.getDelta());
            shapeRenderer.end();

            //Drawing pause menu text
            spriteBatch.setProjectionMatrix(cam.combined);
            spriteBatch.begin();

            textUtils.drawNormalRoboto(
                    spriteBatch,
                    pauseMenuTitle,
                    Color.BLACK,
                    pauseMenu.getX() + 19,
                    pauseMenu.getY() + pauseMenu.getHeight() - 10);

            //Drawing options
            for (int i = 0; i < pauseActions.length; i++) {
                textUtils.drawSmallRoboto(spriteBatch, pauseActions[i], Color.BLACK, pauseMenu.getX() + 42, pauseMenu.getY() + (pauseMenu.getHeight() * 2 / 3f) - (i * 40));
            }

            spriteBatch.end();


            // Drawing selection triangle
            // Yoinked from BattleScene

            // Empty on purpose such that the other triangles are not drawn.
            if (!showSummary) {
                if (showTeamOptions) {
                    MonsterTeam.drawTeamOptionsTriangle(shapeRenderer, teamActionMenu, teamOptionIndex, teamActions);
                } else if (showMonsterTeam) {
                    MonsterTeam.drawMonsterTeamTriangle(shapeRenderer, switchIndicatorColor, monsterTeamMenu, selectedOptionIndexMonsterTeam, monsterTeam);
                } else {
                    MonsterTeam.drawDefaultTriangle(shapeRenderer, pauseMenu, selectedOptionIndex, pauseActions);
                }
            }
        }
    }

    @Override
    public void handleInput(GameData gameData, IGameViewManager gameViewManager) {
        //If map event, run handleInput() and return.
        if (!mapEvents.isEmpty()) {
            mapEvents.peek().handleInput(gameData);
        }

        gameData.setPaused(isPaused);
        if (isPaused) {
            if (gameData.getKeys().isPressed(GameKeys.DOWN)) {
                if (showTeamOptions && !showSummary) {
                    if (teamOptionIndex < teamActions.length)
                        teamOptionIndex++;
                    else
                        teamOptionIndex = 0;
                } else if (showMonsterTeam) {
                    if (firstSelected >= 0 && firstSelected != selectedOptionIndexMonsterTeam && temporarySecondSelected != -1) {
                        // Resets the color back to black every time we go up or down the list
                        monsterRectangles[temporarySecondSelected].setBorderColor(Color.BLACK);
                    }
                    if (selectedOptionIndexMonsterTeam < monsterTeam.size() - 1) {
                        selectedOptionIndexMonsterTeam++;
                    } else {
                        selectedOptionIndexMonsterTeam = 0;
                    }
                    temporarySecondSelected = selectedOptionIndexMonsterTeam;
                    // If the first monster to be switched has been selected, and it's not equal to the one being hovered
                    if (firstSelected >= 0 && firstSelected != selectedOptionIndexMonsterTeam && temporarySecondSelected != -1) {
                        // Color the currently hovered monster's border Cyan
                        monsterRectangles[temporarySecondSelected].setBorderColor(Color.valueOf("29d4ff"));
                    }
                } else {
                    if (selectedOptionIndex < pauseActions.length - 1)
                        selectedOptionIndex++;
                    else
                        selectedOptionIndex = 0;
                }
            }
            if (gameData.getKeys().isPressed(GameKeys.UP)) {
                if (showTeamOptions && !showSummary) {
                    if (teamOptionIndex <= 0)
                        teamOptionIndex = teamActions.length - 1;
                    else
                        teamOptionIndex--;
                } else if (showMonsterTeam) {
                    if (firstSelected >= 0 && firstSelected != selectedOptionIndexMonsterTeam) {
                        // Resets the color back to black every time we go up or down the list
                        monsterRectangles[temporarySecondSelected].setBorderColor(Color.BLACK);
                    }
                    if (selectedOptionIndexMonsterTeam <= 0)
                        selectedOptionIndexMonsterTeam = monsterTeam.size() - 1;
                    else {
                        selectedOptionIndexMonsterTeam--;
                    }
                    temporarySecondSelected = selectedOptionIndexMonsterTeam;
                    // If the first monster to be switched has been selected, and it's not equal to the one being hovered
                    if (firstSelected >= 0 && firstSelected != selectedOptionIndexMonsterTeam) {
                        // Color the currently hovered monster's border Cyan
                        monsterRectangles[temporarySecondSelected].setBorderColor(Color.valueOf("29d4ff"));
                    }
                } else {
                    if (selectedOptionIndex <= 0)
                        selectedOptionIndex = pauseActions.length - 1;
                    else
                        selectedOptionIndex--;
                }
            }
            if (gameData.getKeys().isPressed(GameKeys.BACK)) {
                if (showSummary) {
                    showSummary = false;
                    for (Rectangle monsterRectangle : monsterRectangles) {
                        monsterRectangle.setFillColor(Color.WHITE);
                        monsterRectangle.setBorderColor(Color.BLACK);
                    }
                    monsterTeamMenu.setFillColor(Color.WHITE);
                    teamActionMenu.setFillColor(Color.WHITE);
                    monsterRectangles[selectedOptionIndexMonsterTeam].setBorderColor(Color.valueOf("ffcb05"));
                } else if (showTeamOptions) {
                    showTeamOptions = false;
                    for (Rectangle monsterRectangle : monsterRectangles) {
                        monsterRectangle.setBorderColor(Color.BLACK);
                    }
                    teamOptionIndex = 0;
                } else if (showMonsterTeam) {
                    showMonsterTeam = false;
                    resetMonsterTeamDrawing();
                    selectedOptionIndexMonsterTeam = 0;
                    pauseMenu.setFillColor(Color.WHITE);
                } else {
                    isPaused = false;
                    gameData.getKeys().setKey(GameKeys.ACTION, false);
                    return;
                }
            }
            if (gameData.getKeys().isPressed(GameKeys.ACTION)) {
                if (showMonsterTeam) {
                    if (showTeamOptions) {
                        if (teamActions[teamOptionIndex].equals("Summary")) {
                            if (!showSummary) {
                                showSummary = true;
                                monsterTeamMenu.setFillColor(Color.LIGHT_GRAY);
                                for (Rectangle monsterRectangle : monsterRectangles) {
                                    monsterRectangle.setFillColor(Color.LIGHT_GRAY);
                                }
                                teamActionMenu.setFillColor(Color.LIGHT_GRAY);
                            }
                        }
                        if (teamActions[teamOptionIndex].equals("Switch")) {
                            showTeamOptions = false;
                            currentlySwitching = true;
                            if (firstSelected == -1) { // If nothing is selected
                                firstSelected = selectedOptionIndexMonsterTeam; // Select the current monster
                                switchIndicatorColor = new Color(Color.valueOf("ffcb05"));
                                monsterRectangles[firstSelected].setBorderColor(Color.valueOf("ffcb05"));
                                showSwitchingText = true;
                                return;
                            }
                        }
                        if (teamActions[teamOptionIndex].equals("Cancel")) {
                            showTeamOptions = !showTeamOptions;
                            monsterRectangles[selectedOptionIndexMonsterTeam].setBorderColor(Color.BLACK);
                        }
                    } else if (currentlySwitching) {
                        if (secondSelected == -1) {
                            secondSelected = selectedOptionIndexMonsterTeam; // Select the second current monster
                        }
                        if (firstSelected == secondSelected) { // If the same monster has been chosen twice, reset
                            resetMonsterTeamDrawing();
                            return;
                        }
                        if (firstSelected != -1 && secondSelected != -1) {
                            IMonster newFirstMonster = monsterTeam.get(secondSelected); // Switching the two Monsters.
                            IMonster newSecondMonster = monsterTeam.get(firstSelected);
                            monsterTeam.set(firstSelected, newFirstMonster);
                            monsterTeam.set(secondSelected, newSecondMonster);
                            resetMonsterTeamDrawing();
                            mtp.setMonsterTeam(monsterTeam); // Set the player's monster team to the new order
                            currentlySwitching = false;
                            teamOptionIndex = 0;
                        }
                    } else {
                        if (!monsterTeam.isEmpty()) {
                            showTeamOptions = true;
                            monsterRectangles[selectedOptionIndexMonsterTeam].setBorderColor(Color.valueOf("ffcb05"));
                        }
                    }

                }
                if (!showMonsterTeam) {
                    if (pauseActions[selectedOptionIndex].equals("Resume")) {
                        isPaused = false;
                        gameData.getKeys().setKey(GameKeys.ACTION, false);
                        return;
                    }
                    if (pauseActions[selectedOptionIndex].equals("Team")) {
                        showMonsterTeam = true;
                        pauseMenu.setFillColor(Color.LIGHT_GRAY);
                    }
                    if (pauseActions[selectedOptionIndex].equals("Inventory"))
                        System.out.println("Not implemented yet!");


                    if (pauseActions[selectedOptionIndex].equals("Quit")) {
                        isPaused = false;
                        gameData.setPaused(isPaused);
                        if (cam != null)
                            cam.position.set(gameData.getDisplayWidth() / 2, gameData.getDisplayHeight() / 2, 0);
                        gameViewManager.setDefaultView();
                    }
                }
            }
            return;
        }
        if (gameData.getKeys().isPressed(GameKeys.BACK)) {
            isPaused = true;
            //currentlySwitching = false;
            gameData.setPaused(isPaused);
        }
    }

    /**
     * Resets all the drawing done to the Monster Team back to normal.
     * Sets the colors back to black and resets indexes.
     */
    private void resetMonsterTeamDrawing() {
        showSwitchingText = false;
        switchIndicatorColor = new Color(Color.BLACK); // Sets the triangle back to black
        // Sets the borders back to black
        if (firstSelected >= 0) {
            monsterRectangles[firstSelected].setBorderColor(Color.BLACK);
        }
        if (secondSelected >= 0) {
            monsterRectangles[secondSelected].setBorderColor(Color.BLACK);
        }
        if (temporarySecondSelected >= 0) {
            monsterRectangles[temporarySecondSelected].setBorderColor(Color.BLACK);
        }
        // Resets the indexes
        firstSelected = -1;
        secondSelected = -1;
        currentlySwitching = false;
        teamOptionIndex = 0;
    }

    @Override
    public void dispose() {
        if (cam != null)
            cam.position.set(cam.viewportWidth / 2, cam.viewportHeight / 2, 0);
        mapMusic.stop();
        mapMusic.dispose();
        mapMusic = null;
    }

    public void setSettingsService(IGameSettings settings) {
        this.settings = settings;
        if (settings.getSetting(SettingsRegistry.getInstance().getMusicVolumeSetting()) == null) {
            settings.setSetting(SettingsRegistry.getInstance().getMusicVolumeSetting(), 30);
        }
        if (settings.getSetting(SettingsRegistry.getInstance().getSoundVolumeSetting()) == null) {
            settings.setSetting(SettingsRegistry.getInstance().getSoundVolumeSetting(), 60);
        }
        if (settings.getSetting(SettingsRegistry.getInstance().getRectangleStyleSetting()) == null) {
            settings.setSetting(SettingsRegistry.getInstance().getRectangleStyleSetting(), false);
        }
    }

    public void removeSettingsService(IGameSettings settings) {
        this.settings = null;
    }

    public void addEntityProcessingService(IEntityProcessingService eps) {
        this.entityProcessorList.add(eps);
    }

    public void removeEntityProcessingService(IEntityProcessingService eps) {
        this.entityProcessorList.remove(eps);
    }

    public void addPostProcessingService(IPostEntityProcessingService pps) {
        this.postProcessingList.add(pps);
    }

    public void removePostProcessingService(IPostEntityProcessingService pps) {
        this.postProcessingList.remove(pps);
    }

    public void addGamePluginService(IGamePluginService plugin) {
        this.gamePluginList.add(plugin);
        gdxThreadTasks.add(() -> plugin.start(gameData, world));
    }

    public void removeGamePluginService(IGamePluginService plugin) {
        this.gamePluginList.remove(plugin);
        gdxThreadTasks.add(() -> plugin.stop(gameData, world));
    }

    public void setBattleView(IBattleView battleView) {
        this.battleView = battleView;
    }

    public void removeBattleView(IBattleView battleView) {
        this.battleView = null;
    }

    @Override
    public float getMapLeft() {
        return mapLeft - (cam.viewportWidth / 2f);
    }

    @Override
    public float getMapRight() {
        return mapRight + (cam.viewportWidth / 2f);
    }

    @Override
    public float getMapBottom() {
        return mapBottom - (cam.viewportHeight / 2f);
    }

    @Override
    public float getMapTop() {
        return mapTop + (cam.viewportHeight / 2f);
    }

    @Override
    public int getTileSize() {
        return tilePixelSize;
    }

    @Override
    public boolean isCellBlocked(float x, float y) {
        TiledMapTileLayer collsionLayer = (TiledMapTileLayer) tiledMap.getLayers().get(0);
        TiledMapTileLayer.Cell cell = collsionLayer.getCell((int) Math.floor(x / tilePixelSize), (int) Math.floor(y / tilePixelSize));
        return cell.getTile().getProperties().containsKey("blocked");
    }

    @Override
    public void startEncounter(Entity player, Entity enemy) {
        if (battleView == null) {
            return;
        }

        MonsterTeamPart playerMonsterTeamPart = player.getPart(MonsterTeamPart.class);
        MonsterTeamPart enemyMonsterTeamPart = enemy.getPart(MonsterTeamPart.class);
        List<IMonster> playerMonsters = playerMonsterTeamPart.getMonsterTeam();
        List<IMonster> enemyMonsters = enemyMonsterTeamPart.getMonsterTeam();

        gameViewManager.setView((IGameViewService) battleView, false); // Do not dispose the map
        cam.position.set(cam.viewportWidth / 2, cam.viewportHeight / 2, 0);
        mapMusic.stop();
        battleView.startBattle(playerMonsters, enemyMonsters, result -> {
            gameViewManager.setView(MapView.this);
            mapMusic.play();
            boolean playerWon = result.getWinner() == result.getPlayer();
            String winnerName = playerWon ? "Player" : "Enemy";
            addMapEvent(new TextMapEvent(new LinkedList<>(Arrays.asList(String.format("%s won the battle!", winnerName)))));
            if (!playerWon) {
                enemyMonsterTeamPart.healAllMonsters();
            }
        });
    }

    @Override
    public void addMapEvent(IMapEvent event) {
        mapEvents.add(event);
    }

    @Override
    public String toString() {
        return "Start the adventure!";
    }
}
