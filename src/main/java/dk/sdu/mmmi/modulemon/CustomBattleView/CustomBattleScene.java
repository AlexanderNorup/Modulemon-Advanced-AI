package dk.sdu.mmmi.modulemon.CustomBattleView;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import dk.sdu.mmmi.modulemon.CommonMonster.IMonster;
import dk.sdu.mmmi.modulemon.Game;
import dk.sdu.mmmi.modulemon.common.AssetLoader;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.drawing.DrawingUtils;
import dk.sdu.mmmi.modulemon.common.drawing.Position;
import dk.sdu.mmmi.modulemon.common.drawing.Rectangle;
import dk.sdu.mmmi.modulemon.common.drawing.TextUtils;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;

public class CustomBattleScene {

    // LibGDX Drawing stuff
    private String teamAAIText = "";
    private Color teamATextColor = Color.WHITE;
    private String teamBAIText = "";
    private Color teamBTextColor = Color.WHITE;
    private Color startBattleColor = Color.WHITE;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;

    private boolean isTeamA = false;
    private int changingMonsterIndex = -1;
    private int selectedMonsterIndex = -1;

    public static final Color SelectColor = Color.valueOf("2a75bb");

    private Rectangle teamAContainer;
    private Rectangle teamBContainer;
    private Rectangle[] teamAMonsterBoxes;
    private Rectangle[] teamBMonsterBoxes;
    private IMonster[] teamAMonsters;
    private IMonster[] teamBMonsters;

    public CustomBattleScene(IGameSettings settings) {
        spriteBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        teamAContainer = DrawingUtils.createRectangle(Rectangle.class, 0, 0, 0, 0);
        teamBContainer = DrawingUtils.createRectangle(Rectangle.class, 0, 0, 0, 0);
        int teamSize = 6;
        teamAMonsters = new IMonster[teamSize];
        teamBMonsters = new IMonster[teamSize];
        teamAMonsterBoxes = new Rectangle[teamSize];
        teamBMonsterBoxes = new Rectangle[teamSize];

        for (int i = 0; i < teamSize; i++) {
            teamAMonsterBoxes[i] = DrawingUtils.createRectangle(settings, 0, 0, 0, 0);
            teamBMonsterBoxes[i] = DrawingUtils.createRectangle(settings, 0, 0, 0, 0);
        }
    }

    private static TextUtils text = TextUtils.getInstance();

    public void draw(GameData gameData) {
        float dt = gameData.getDelta();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        spriteBatch.setProjectionMatrix(Game.cam.combined);
        spriteBatch.begin();

        text.setCoordinateMode(TextUtils.CoordinateMode.CENTER);
        final int textTopMargin = 60;
        text.drawTitleFont(spriteBatch, "Custom Battle", Color.valueOf("ffcb05"), gameData.getDisplayWidth() / 2f, gameData.getDisplayHeight() - textTopMargin);
        text.setCoordinateMode(TextUtils.CoordinateMode.TOP_LEFT); // Reset
        spriteBatch.end();

        //DRAW THE BOXES

        if (teamBContainer.getWidth() <= 0) {
            calculateBoxSizes(gameData);
        }

        spriteBatch.begin();
        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.setColor(Color.WHITE);

        Gdx.gl.glEnable(GL20.GL_BLEND); //Alows for opacity
        if (gameData.getCamera() != null)
            shapeRenderer.setProjectionMatrix(gameData.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        teamAContainer.draw(shapeRenderer, dt);
        teamBContainer.draw(shapeRenderer, dt);

        for (int i = 0; i < teamAMonsterBoxes.length; i++) {
            if(selectedMonsterIndex == i){
                if(isTeamA){
                    teamAMonsterBoxes[i].setBorderColor(SelectColor);
                    teamAMonsterBoxes[i].setBorderWidth(4);
                }else{
                    teamBMonsterBoxes[i].setBorderColor(SelectColor);
                    teamBMonsterBoxes[i].setBorderWidth(4);
                }
            }else{
                teamAMonsterBoxes[i].setBorderColor(Color.BLACK);
                teamAMonsterBoxes[i].setBorderWidth(2);
                teamBMonsterBoxes[i].setBorderColor(Color.BLACK);
                teamBMonsterBoxes[i].setBorderWidth(2);
            }
            teamAMonsterBoxes[i].draw(shapeRenderer, dt);
            teamBMonsterBoxes[i].draw(shapeRenderer, dt);
        }

        spriteBatch.end();
        shapeRenderer.end();

        spriteBatch.begin();
        final int gapAboveContainersText = 20;
        text.setCoordinateMode(TextUtils.CoordinateMode.CENTER);
        text.drawNormalBoldRoboto(spriteBatch, "Your Team", Color.WHITE, teamAContainer.getX() + teamAContainer.getWidth() / 2f, teamAContainer.getY() + teamAContainer.getHeight() + gapAboveContainersText);
        text.drawNormalBoldRoboto(spriteBatch, "Opponent Team", Color.WHITE, teamBContainer.getX() + teamBContainer.getWidth() / 2f, teamBContainer.getY() + teamBContainer.getHeight() + gapAboveContainersText);

        for (int i = 0; i < teamAMonsterBoxes.length; i++) {
            drawMonsterWithName(teamAMonsters[i], teamAMonsterBoxes[i], changingMonsterIndex == i && isTeamA);
            drawMonsterWithName(teamBMonsters[i], teamBMonsterBoxes[i], changingMonsterIndex == i && !isTeamA);
        }


        final float arrowSpacingFromEdge = 20;
        final float controllerSelectorY = teamAContainer.getY() - gapAboveContainersText;

        text.drawNormalRoboto(spriteBatch, "<", Color.WHITE, teamAContainer.getX() + arrowSpacingFromEdge ,controllerSelectorY );
        text.drawNormalRoboto(spriteBatch, ">", Color.WHITE, teamAContainer.getX() - arrowSpacingFromEdge + teamAContainer.getWidth() , controllerSelectorY);
        text.drawNormalRoboto(spriteBatch, teamAAIText, teamATextColor, teamAContainer.getX() + teamAContainer.getWidth() / 2f, controllerSelectorY);

        text.drawNormalRoboto(spriteBatch, "<", Color.WHITE, teamBContainer.getX() + arrowSpacingFromEdge ,controllerSelectorY );
        text.drawNormalRoboto(spriteBatch, ">", Color.WHITE, teamBContainer.getX() - arrowSpacingFromEdge + teamBContainer.getWidth() , controllerSelectorY);
        text.drawNormalRoboto(spriteBatch, teamBAIText, teamBTextColor, teamBContainer.getX() + teamBContainer.getWidth() / 2f, controllerSelectorY);

        text.drawBigBoldRoboto(spriteBatch, "START BATTLE!", startBattleColor, gameData.getDisplayWidth() / 2f, 50 );

        text.setCoordinateMode(TextUtils.CoordinateMode.TOP_LEFT);
        spriteBatch.end();
    }

    private void drawMonsterWithName(IMonster monster, Rectangle rectangle, boolean drawMonsterSwitcher) {
        final int monsterSpriteGap = 10;
        final float monsterSpriteSize = rectangle.getWidth() - monsterSpriteGap * 2;

        if (monster == null) {
            text.drawSmallRoboto(spriteBatch, "No monster", Color.LIGHT_GRAY,
                    rectangle.getX() + rectangle.getWidth() / 2f,
                    rectangle.getY() + rectangle.getHeight() / 2f);
        } else {
            //Draw the monster
            var sprite = AssetLoader.getInstance().getImageAsset(monster.getFrontSprite(), getClass());
            sprite.setPosition(rectangle.getX() + monsterSpriteGap, rectangle.getY() + monsterSpriteGap * 2);
            sprite.setSize(monsterSpriteSize, monsterSpriteSize);
            sprite.draw(spriteBatch, 1);
            text.drawSmallRoboto(spriteBatch, monster.getName(), Color.BLACK,
                    rectangle.getX() + rectangle.getWidth() / 2f,
                    rectangle.getY() + monsterSpriteGap);
        }

        if(drawMonsterSwitcher){
            final float arrowSpacingFromEdge = 10;
            text.drawNormalRoboto(spriteBatch, "<", Color.BLACK,
                    rectangle.getX() + arrowSpacingFromEdge,
                    rectangle.getY() + rectangle.getHeight() / 2f );
            text.drawNormalRoboto(spriteBatch, ">", Color.BLACK,
                    rectangle.getX() - arrowSpacingFromEdge + rectangle.getWidth(),
                    rectangle.getY() + rectangle.getHeight() / 2f );
        }
    }

    public void setTeamAMonsters(IMonster[] teamAMonsters) {
        if (teamAMonsters.length != this.teamAMonsters.length) {
            throw new IllegalArgumentException("The monster array must be exactly %d entries long.".formatted(this.teamAMonsters.length));
        }
        this.teamAMonsters = teamAMonsters;
    }

    public void setTeamBMonsters(IMonster[] teamBMonsters) {
        if (teamBMonsters.length != this.teamBMonsters.length) {
            throw new IllegalArgumentException("The monster array must be exactly %d entries long.".formatted(this.teamBMonsters.length));
        }
        this.teamBMonsters = teamBMonsters;
    }

    public void setTeamA(boolean teamA) {
        isTeamA = teamA;
    }

    public void setChangingMonsterIndex(int changingMonsterIndex) {
        this.changingMonsterIndex = changingMonsterIndex;
    }

    public void setSelectedMonsterIndex(int selectedMonsterIndex) {
        this.selectedMonsterIndex = selectedMonsterIndex;
    }

    public void setTeamAAIText(String teamAAIText) {
        this.teamAAIText = teamAAIText;
    }

    public void setTeamBAIText(String teamBAIText) {
        this.teamBAIText = teamBAIText;
    }

    public void setTeamATextColor(Color teamATextColor) {
        this.teamATextColor = teamATextColor;
    }

    public void setTeamBTextColor(Color teamBTextColor) {
        this.teamBTextColor = teamBTextColor;
    }

    public void setStartBattleColor(Color startBattleColor) {
        this.startBattleColor = startBattleColor;
    }

    private void calculateBoxSizes(GameData gameData) {
        final int monsterContainerBottomOffset = 130;
        final int monsterContainerCenterOffset = 20;
        final int freeSpaceToLeaveAtTop = 150; // Estimated size of text + mer'
        final int containerGap = 20;
        final float containerWidth = 300;
        final float containerHeight = gameData.getDisplayHeight() - monsterContainerBottomOffset - containerGap - freeSpaceToLeaveAtTop;
        teamAContainer.setPosition(new Position((gameData.getDisplayWidth() / 2f) - monsterContainerCenterOffset - containerWidth, monsterContainerBottomOffset));
        teamBContainer.setPosition(new Position((gameData.getDisplayWidth() / 2f) + monsterContainerCenterOffset, monsterContainerBottomOffset));
        teamAContainer.setWidth(containerWidth);
        teamBContainer.setWidth(containerWidth);
        teamAContainer.setHeight(containerHeight);
        teamBContainer.setHeight(containerHeight);

        final float monsterBoxSize = 110;
        final float topOfContainers = teamAContainer.getY() + teamAContainer.getHeight();
        final float monsterGap = 20;
        for (int i = 0; i < teamAMonsterBoxes.length; i++) {
            teamAMonsterBoxes[i].setHeight(monsterBoxSize);
            teamAMonsterBoxes[i].setWidth(monsterBoxSize);
            teamBMonsterBoxes[i].setHeight(monsterBoxSize);
            teamBMonsterBoxes[i].setWidth(monsterBoxSize);

            final float rowCount = i % 2 == 0 ? i + 1 : i;
            float boxY = topOfContainers - monsterBoxSize - monsterGap - (monsterGap / 2f * (rowCount - 1)) - (monsterBoxSize / 2f * (rowCount - 1));
            if (i % 2 == 0) {
                // Even
                teamAMonsterBoxes[i].setPosition(new Position(teamAContainer.getX() + monsterGap, boxY));
                teamBMonsterBoxes[i].setPosition(new Position(teamBContainer.getX() + monsterGap, boxY));
            } else {
                // Odd
                teamAMonsterBoxes[i].setPosition(new Position(teamAContainer.getX() + teamAContainer.getWidth() - monsterGap - teamAMonsterBoxes[i].getWidth(), boxY));
                teamBMonsterBoxes[i].setPosition(new Position(teamBContainer.getX() + teamBContainer.getWidth() - monsterGap - teamBMonsterBoxes[i].getWidth(), boxY));
            }
        }
    }
}
