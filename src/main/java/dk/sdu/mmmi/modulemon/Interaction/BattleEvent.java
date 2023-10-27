package dk.sdu.mmmi.modulemon.Interaction;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import dk.sdu.mmmi.modulemon.CommonMap.Data.Entity;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.PositionPart;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityType;
import dk.sdu.mmmi.modulemon.CommonMap.IMapEvent;
import dk.sdu.mmmi.modulemon.CommonMap.IMapView;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.AssetLoader;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.data.GameKeys;
import dk.sdu.mmmi.modulemon.common.drawing.Rectangle;
import dk.sdu.mmmi.modulemon.common.drawing.TextUtils;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;

import java.util.Queue;

public class BattleEvent implements IMapEvent {
    private Queue<String> lines;
    private Rectangle textBox;
    private Rectangle exlamationBox;
    private Entity aggresor;
    private Entity victim;
    private IMapView mapView;
    private Sound alertSound;
    private boolean alertSoundPlayed = false;
    private IGameSettings settings;

    private BattleState currentState;

    public BattleEvent(Queue<String> lines, Entity aggressor, Entity victim, IMapView map, IGameSettings settings) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Argument 'lines' is null or has no elements");
        }
        if (aggressor == null || victim == null) {
            throw new IllegalArgumentException("Missing one or more entities");
        }
        this.aggresor = aggressor;
        this.victim = victim;
        this.lines = lines;
        this.mapView = map;
        alertSound = AssetLoader.getInstance().getSoundAsset("/sounds/alert.ogg", this.getClass());
        this.settings = settings;
        textBox = new Rectangle(20, 20, -1, -1);
        exlamationBox = new Rectangle(-1, -1, -1, -1);
        currentState = BattleState.BEFORE_BATTLE;
    }

    public void addLine(String line) {
        this.lines.add(line);
    }

    @Override
    public boolean isEventDone() {
        return lines.isEmpty();
    }

    @Override
    public void start(GameData gameData) {
        // Empty
    }

    @Override
    public void update(GameData gameData) {
        Camera cam = gameData.getCamera();

        textBox.setHeight(100f);
        textBox.setWidth(gameData.getDisplayWidth() - 50);
        textBox.setX((cam.position.x - cam.viewportWidth / 2f) + 20);
        textBox.setY((cam.position.y - cam.viewportHeight / 2f) + 20);

        exlamationBox.setHeight(28f);
        exlamationBox.setWidth(12.5f);
    }

    @Override
    public void draw(GameData gameData, SpriteBatch spriteBatch, ShapeRenderer shapeRenderer) {
        if (currentState == BattleState.ONGOING_BATTLE)
            return;
        //Draw rectangle
        Camera cam = gameData.getCamera();
        shapeRenderer.setProjectionMatrix(cam.combined);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        textBox.draw(shapeRenderer, gameData.getDelta());

        PositionPart victimPosition = victim.getPart(PositionPart.class);
        var visualVictimPosition = victimPosition.getVisualPos();


        if (victimPosition != null) {
            exlamationBox.setY(visualVictimPosition.y + 59);
            exlamationBox.setX(visualVictimPosition.x + 12.5f);
            exlamationBox.draw(shapeRenderer, gameData.getDelta());
        }

        shapeRenderer.end();

        //Draw text
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();


        if (victimPosition != null)
            TextUtils.getInstance().drawBigRoboto(spriteBatch,
                    "!",
                    Color.BLACK,
                    visualVictimPosition.x + 16,
                    visualVictimPosition.y + 85);

        if(!alertSoundPlayed){
            playAlertSound();
        }


        TextUtils.getInstance().drawNormalRoboto(spriteBatch,
                lines.peek(),
                Color.BLACK,
                (cam.position.x - cam.viewportWidth / 2f) + 25,
                (cam.position.y - cam.viewportHeight / 2f) + 100
        );

        TextUtils.getInstance().drawSmallRoboto(spriteBatch,
                "Press [ENTER] to continue...",
                Color.BLACK,
                (cam.position.x - cam.viewportWidth / 2f) + cam.viewportWidth - 240,
                (cam.position.y - cam.viewportHeight / 2f) + 40
        );
        spriteBatch.end();
    }

    private void playAlertSound(){
        var volume =  settings != null
                ? (float)settings.getSetting(SettingsRegistry.getInstance().getSoundVolumeSetting()) / 100f
                : 0.6f;

        alertSound.play(volume);
        alertSoundPlayed = true;
    }

    @Override
    public void handleInput(GameData gameData) {
        if (gameData.getKeys().isPressed(GameKeys.ACTION)) {
            this.lines.poll();
            if (lines.isEmpty()) {
                mapView.startEncounter(aggresor, victim);
                currentState = BattleState.ONGOING_BATTLE;
            }
        }
    }

    private enum BattleState {
        BEFORE_BATTLE,
        ONGOING_BATTLE
    }
}
