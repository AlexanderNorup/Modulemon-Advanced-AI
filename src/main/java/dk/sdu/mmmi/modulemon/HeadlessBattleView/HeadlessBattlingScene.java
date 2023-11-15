package dk.sdu.mmmi.modulemon.HeadlessBattleView;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import dk.sdu.mmmi.modulemon.Game;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.drawing.DrawingUtils;
import dk.sdu.mmmi.modulemon.common.drawing.Position;
import dk.sdu.mmmi.modulemon.common.drawing.Rectangle;
import dk.sdu.mmmi.modulemon.common.drawing.TextUtils;

public class HeadlessBattlingScene {
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private StringBuilder titleText = new StringBuilder("Simulating Battles");
    private float titleCounter = 0;
    private int teamAWins = 0;
    private int teamBWins = 0;
    private int teamAStartWins = 0;
    private int teamBStartWins = 0;
    private float avgTurnsToWinA = 0.0f;
    private float avgTurnsToWinB = 0.0f;
    private String teamAAIName;
    private String teamBAIName;

    private int currentBattles = 0;
    private float battleProgress = 0;
    private boolean doneBattling = false;

    private static TextUtils text = TextUtils.getInstance();

    private Rectangle teamABackground;
    private Rectangle teamBBackground;

    public HeadlessBattlingScene() {
        this.shapeRenderer = new ShapeRenderer();
        this.spriteBatch = new SpriteBatch();
        teamABackground = DrawingUtils.createRectangle(Rectangle.class, 0, 0, 0, 0);
        teamBBackground = DrawingUtils.createRectangle(Rectangle.class, 0, 0, 0, 0);
    }

    public void draw(GameData gameData) {
        var screenWidth = gameData.getDisplayWidth();
        var screenHeight = gameData.getDisplayHeight();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (teamABackground.getWidth() <= 0) {
            calculateBoxSizes(gameData);
        }
        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        teamABackground.draw(shapeRenderer, gameData.getDelta());
        teamBBackground.draw(shapeRenderer, gameData.getDelta());
        shapeRenderer.end();

        spriteBatch.setProjectionMatrix(Game.cam.combined);
        spriteBatch.begin();
        text.setCoordinateMode(TextUtils.CoordinateMode.TOP_LEFT);
        final int textTopMargin = 60;
        final int textLengthEstimate = 350;
        if (!doneBattling) {
            animateTitleText(gameData);
        }
        text.drawTitleFont(spriteBatch, titleText.toString(), Color.valueOf("ffcb05"), screenWidth / 2f - textLengthEstimate, screenHeight - textTopMargin);
        final int horizontalOffset = 175;
        float aXPos = (screenWidth / 2f) - horizontalOffset;
        float bXPos = (screenWidth / 2f) + horizontalOffset;
        final int winYGap = 50;
        final int boxOffset = 125;
        text.setCoordinateMode(TextUtils.CoordinateMode.CENTER);

        var winTextHeight = screenHeight / 2f + boxOffset;
        var winNumHeight = winTextHeight - winYGap;
        text.drawBigBoldRoboto(spriteBatch, "Team A Wins", Color.BLACK, aXPos, winTextHeight);
        text.drawBigBoldRoboto(spriteBatch, "Team B Wins", Color.BLACK, bXPos, winTextHeight);
        text.drawSmallRoboto(spriteBatch, teamAAIName, Color.BLACK, aXPos, winTextHeight - 20);
        text.drawSmallRoboto(spriteBatch, teamBAIName, Color.BLACK, bXPos, winTextHeight - 20);
        text.drawNormalBoldRoboto(spriteBatch, String.valueOf(teamAWins), Color.BLACK, aXPos, winNumHeight);
        text.drawNormalBoldRoboto(spriteBatch, String.valueOf(teamBWins), Color.BLACK, bXPos, winNumHeight);
        var startingWinTextHeight = winTextHeight - (winYGap * 2);
        var startingWinNumHeight = winTextHeight - (winYGap * 2.5f);
        text.drawSmallBoldRoboto(spriteBatch, "Wins when starting", Color.BLACK, aXPos, startingWinTextHeight);
        text.drawSmallBoldRoboto(spriteBatch, "Wins when starting", Color.BLACK, bXPos, startingWinTextHeight);
        text.drawSmallRoboto(spriteBatch, String.valueOf(teamAStartWins), Color.BLACK, aXPos, startingWinNumHeight);
        text.drawSmallRoboto(spriteBatch, String.valueOf(teamBStartWins), Color.BLACK, bXPos, startingWinNumHeight);
        var turnsToWinTextHeight = winTextHeight - (winYGap * 3.5f);
        var turnsToWinNumHeight = winTextHeight - (winYGap * 4f);
        text.drawSmallBoldRoboto(spriteBatch, "AVG turns to win", Color.BLACK, aXPos, turnsToWinTextHeight);
        text.drawSmallBoldRoboto(spriteBatch, "AVG turns to win", Color.BLACK, bXPos, turnsToWinTextHeight);
        text.drawSmallRoboto(spriteBatch, String.valueOf(avgTurnsToWinA), Color.BLACK, aXPos, turnsToWinNumHeight);
        text.drawSmallRoboto(spriteBatch, String.valueOf(avgTurnsToWinB), Color.BLACK, bXPos, turnsToWinNumHeight);

        text.drawNormalBoldRoboto(spriteBatch, String.format("Ongoing battles: %d", currentBattles), Color.WHITE, screenWidth / 2f, (screenHeight / 2f) - winYGap * 3f);
        text.drawBigBoldRoboto(spriteBatch, String.format("Battle progress %.1f %%", battleProgress * 100), Color.WHITE, screenWidth / 2f, (screenHeight / 2f) - winYGap * 4f);
        if (doneBattling) {
            text.drawNormalRoboto(spriteBatch, "Press [action] to return", Color.WHITE, screenWidth / 2f, (screenHeight / 2f) - winYGap * 5);
        }
        text.setCoordinateMode(TextUtils.CoordinateMode.TOP_LEFT);
        spriteBatch.end();
    }

    private void animateTitleText(GameData gameData) {
        titleCounter += gameData.getDelta();
        if (titleCounter >= 1) {
            if (titleText.toString().equalsIgnoreCase("simulating battles...")) {
                titleText.delete(18, titleText.length());
            } else {
                titleText.append('.');
            }
            titleCounter = 0;
        }
    }

    private void calculateBoxSizes(GameData gameData) {
        final int bottomOffset = 250;
        final int monsterContainerCenterOffset = 20;
        final int freeSpaceToLeaveAtTop = 200;
        final float containerWidth = 300;
        final float containerHeight = gameData.getDisplayHeight() - bottomOffset - freeSpaceToLeaveAtTop;
        teamABackground.setPosition(new Position((gameData.getDisplayWidth() / 2f) - monsterContainerCenterOffset - containerWidth, bottomOffset));
        teamBBackground.setPosition(new Position((gameData.getDisplayWidth() / 2f) + monsterContainerCenterOffset, bottomOffset));
        teamABackground.setWidth(containerWidth);
        teamBBackground.setWidth(containerWidth);
        teamABackground.setHeight(containerHeight);
        teamBBackground.setHeight(containerHeight);
    }

    public void setTeamAWins(int teamAWins) {
        this.teamAWins = teamAWins;
    }

    public void setTeamBWins(int teamBWins) {
        this.teamBWins = teamBWins;
    }

    public void setTeamAStartWins(int teamAStartWins) {
        this.teamAStartWins = teamAStartWins;
    }

    public void setTeamBStartWins(int teamBStartWins) {
        this.teamBStartWins = teamBStartWins;
    }

    public void setAvgTurnsToWinA(float avgTurnsToWinA) {
        if(!Float.isNaN(avgTurnsToWinA))
            this.avgTurnsToWinA = avgTurnsToWinA;
    }

    public void setAvgTurnsToWinB(float avgTurnsToWinB) {
        if(!Float.isNaN(avgTurnsToWinB))
            this.avgTurnsToWinB = avgTurnsToWinB;
    }

    public void setTeamAAIName(String teamAAI) {
        this.teamAAIName = teamAAI;
    }

    public void setTeamBAIName(String teamBAI) {
        this.teamBAIName = teamBAI;
    }

    public void setDoneBattling(boolean doneBattling) {
        this.doneBattling = doneBattling;
    }

    public void setCurrentBattles(int currentBattles) {
        this.currentBattles = currentBattles;
    }

    public void setBattleProgress(float battleProgress) {
        this.battleProgress = battleProgress;
    }
}
