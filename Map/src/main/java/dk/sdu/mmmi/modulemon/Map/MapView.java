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
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import dk.sdu.mmmi.modulemon.CommonBattle.IBattleParticipant;
import dk.sdu.mmmi.modulemon.CommonBattle.MonsterTeamPart;
import dk.sdu.mmmi.modulemon.CommonBattleClient.IBattleCallback;
import dk.sdu.mmmi.modulemon.CommonBattleClient.IBattleResult;
import dk.sdu.mmmi.modulemon.CommonBattleClient.IBattleView;
import dk.sdu.mmmi.modulemon.CommonMap.IMapView;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.common.AssetLoader;
import dk.sdu.mmmi.modulemon.common.data.*;
import dk.sdu.mmmi.modulemon.common.drawing.Rectangle;
import dk.sdu.mmmi.modulemon.common.drawing.TextUtils;
import dk.sdu.mmmi.modulemon.common.services.IEntityProcessingService;
import dk.sdu.mmmi.modulemon.common.services.IGamePluginService;
import dk.sdu.mmmi.modulemon.common.services.IGameViewService;
import dk.sdu.mmmi.modulemon.common.services.IPostEntityProcessingService;

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
    private static World world = new World();
    private final GameData gameData = new GameData();
    private static final List<IEntityProcessingService> entityProcessorList = new CopyOnWriteArrayList<>();
    private static final List<IPostEntityProcessingService> postProcessingList = new CopyOnWriteArrayList<>();
    private static final List<IGamePluginService> gamePluginList = new CopyOnWriteArrayList<>();
    private static Queue<Runnable> gdxThreadTasks = new LinkedList<>();
    private IGameStateManager gameStateManager;
    private IBattleView battleView;
    private Entity player;


    @Override
    public void init(IGameStateManager gameStateManager) {
        mapMusic = loader.getMusicAsset("/music/village_theme.ogg", MapView.class);
        tiledMap = new OSGiTmxLoader().load("/maps/SeasonalOverworld.tmx");
        overhangLayer = (TiledMapTileLayer) tiledMap.getLayers().get("Top");
        int scale = 4;
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap, scale);
        mapMusic.play();
        mapMusic.setVolume(0.1f);
        mapMusic.setLooping(true);

        // Pixel size
        tilePixelSize = 16 * scale;

        // Sprites
        spriteBatch = new SpriteBatch();

        // Pausing
        isPaused = false;
        showMonsterTeam = false;
        showTeamOptions = false;
        summaryMenu = new Rectangle(100, 100, 380, 300);
        pauseMenu = new Rectangle(100, 100, 200, 250);
        monsterTeamMenu = new Rectangle(100, 100, 400, 550);
        teamActionMenu = new Rectangle(100, 100, 200, 200);
        for (int i = 0; i < monsterRectangles.length; i++) {
            Rectangle rect = new Rectangle(100, 100, 320, 70);
            monsterRectangles[i] = rect;
        }
        shapeRenderer = new ShapeRenderer();
        gdxThreadTasks.add(() -> textUtils = TextUtils.getInstance());


        // Battle
        this.gameStateManager = gameStateManager;

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
    public void update(GameData gameData, IGameStateManager gameStateManager) {
        while (!gdxThreadTasks.isEmpty()) {
            gdxThreadTasks.poll().run();
        }
        for (IEntityProcessingService entityProcessorService : entityProcessorList) {
            entityProcessorService.process(gameData, world);
        }

        for (IPostEntityProcessingService postProcessingService : postProcessingList) {
            postProcessingService.process(gameData, world);
        }
    }


    @Override
    public void draw(GameData gameData) {
        if (cam == null)
            initializeCameraDrawing(gameData);
        tiledMapRenderer.setView(cam);
        tiledMapRenderer.render();
        for (Entity entity : world.getEntities()) {
            if (entity.getSpriteTexture() != null) {
                Texture sprite = entity.getSpriteTexture();

                spriteBatch.setProjectionMatrix(cam.combined);
                spriteBatch.begin();
                spriteBatch.draw(sprite, entity.getPosX(), entity.getPosY());
                spriteBatch.end();

                if (entity.getType().equals(EntityType.PLAYER)) {
                    playerPosX = entity.getPosX();
                    playerPosY = entity.getPosY();
                    if (playerPosY > mapBottom && playerPosY < mapTop) {
                        cam.position.set(cam.position.x, playerPosY, 0);
                        cam.update();
                    }
                    if (playerPosX > mapLeft && playerPosX < mapRight) {
                        cam.position.set(playerPosX, cam.position.y, 0);
                        cam.update();
                    }
                }
            }
        }
        tiledMapRenderer.getBatch().begin();
        tiledMapRenderer.renderTileLayer(overhangLayer);
        tiledMapRenderer.getBatch().end();

        if (showTeamOptions) {
            MonsterTeam.drawTeamOptions(gameData, shapeRenderer, spriteBatch, textUtils, teamActionMenu, monsterTeamMenu, teamActions);
        }
        if (showMonsterTeam) {
            for (Entity entity : world.getEntities()) {
                if(entity.getType().equals(EntityType.PLAYER)){
                    mtp = entity.getPart(MonsterTeamPart.class);
                    monsterTeam = mtp.getMonsterTeam();

                    MonsterTeam.drawMonsterTeam(gameData, shapeRenderer, spriteBatch, textUtils, cam, showSwitchingText, monsterTeamMenu, monsterTeam, monsterRectangles);
                    break;
                }
            }
        }

        if (showSummary) {
            MonsterTeam.drawSummary(gameData, shapeRenderer, spriteBatch, textUtils, summaryMenu, monsterTeamMenu, mtp, selectedOptionIndexMonsterTeam);
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
    public void handleInput(GameData gameData, IGameStateManager gameStateManager) {
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
                    if (selectedOptionIndex < pauseActions.length)
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
            if (gameData.getKeys().isPressed(GameKeys.ESC)) {
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
                    gameData.setPaused(isPaused);
                }
            }
            if (gameData.getKeys().isPressed(GameKeys.ENTER)) {
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
                    }


                    else if (currentlySwitching) {
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
                        showTeamOptions = true;
                        monsterRectangles[selectedOptionIndexMonsterTeam].setBorderColor(Color.valueOf("ffcb05"));
                    }

                }
                if (!showMonsterTeam) {
                    if (pauseActions[selectedOptionIndex].equals("Resume")) {
                        isPaused = false;
                        gameData.setPaused(isPaused);
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
                        gameStateManager.setDefaultState();
                    }
                }
            }
            return;
        }
        if (gameData.getKeys().isPressed(GameKeys.ESC)) {
            isPaused = true;
            //currentlySwitching = false;
            gameData.setPaused(isPaused);
        }
        if(gameData.getKeys().isPressed(GameKeys.E)){
            for(Entity entity: world.getEntities()){
                if(entity.getType().equals(EntityType.PLAYER)){
                    player = entity;
                    break;
                }
            }
            startEncounter(player, player);
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
    }

    public void addEntityProcessingService(IEntityProcessingService eps) {
        this.entityProcessorList.add(eps);
    }

    public void removeEntityProcessingService(IEntityProcessingService eps) {
        this.entityProcessorList.remove(eps);
    }

    public void addPostProcessingService (IPostEntityProcessingService pps){
        this.postProcessingList.add(pps);
    }

    public void removePostProcessingService (IPostEntityProcessingService pps){
        this.postProcessingList.remove(pps);
    }

    public void addGamePluginService (IGamePluginService plugin){
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
    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public void startEncounter(Entity participant1, Entity participant2){
        IBattleParticipant playerParticipant = ((MonsterTeamPart)  participant1.getPart(MonsterTeamPart.class))
                .toBattleParticipant(true);
        IBattleParticipant enemyParticipant = ((MonsterTeamPart)  participant2.getPart(MonsterTeamPart.class))
                .toBattleParticipant(false);

        gameStateManager.setState((IGameViewService) battleView, true); // Do not dispose the map
        battleView.startBattle(playerParticipant, enemyParticipant, new IBattleCallback() {
            @Override
            public void onBattleEnd(IBattleResult result) {
                gameStateManager.setState(MapView.this);
            }
        });
    }


}
