package dk.sdu.mmmi.modulemon.HeadlessBattleView;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import dk.sdu.mmmi.modulemon.Game;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.common.drawing.TextUtils;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;

public class HeadlessBattleScene {
    private SpriteBatch spriteBatch;
    public static final Color SelectColor = Color.valueOf("2a75bb");
    private Color startColor = Color.WHITE;
    private Color amountColor = Color.WHITE;
    private String[] teams = {"Team A", "Team B"};
    private String teamAAIText = "";
    private String teamBAIText = "";
    private int teamIndex = -1;
    private int AIIndex = -1;
    private int battleAmount = -1;
    private boolean choosing = false;

    public HeadlessBattleScene(IGameSettings settings) {
        spriteBatch = new SpriteBatch();
    }

    private static TextUtils text = TextUtils.getInstance();

    public void draw(GameData gameData) {
        var screenWidth = gameData.getDisplayWidth();
        var screenHeight = gameData.getDisplayHeight();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        spriteBatch.setProjectionMatrix(Game.cam.combined);
        spriteBatch.begin();

        text.setCoordinateMode(TextUtils.CoordinateMode.CENTER);
        final int textTopMargin = 60;
        text.drawTitleFont(spriteBatch, "Simulate Battles", Color.valueOf("ffcb05"), gameData.getDisplayWidth() / 2f, gameData.getDisplayHeight() - textTopMargin);
        text.setCoordinateMode(TextUtils.CoordinateMode.TOP_LEFT); // Reset
        spriteBatch.end();

        spriteBatch.begin();
        text.setCoordinateMode(TextUtils.CoordinateMode.BOTTOM_RIGHT);
        int textGap = 75;
        int leftOffset = 150;
        int topOffset = 50;

        for (var i = 0; i < teams.length; i++) {
            var teamTextColor = teamIndex == i ? SelectColor : Color.WHITE;
            var AITextColor = AIIndex == i ? SelectColor : Color.WHITE;
            var yPos = ((screenHeight / 2f) + topOffset) - i * textGap;
            var AIText = i == 0 ? teamAAIText : teamBAIText;
            text.setCoordinateMode(TextUtils.CoordinateMode.BOTTOM_RIGHT);
            text.drawBigBoldRoboto(spriteBatch, teams[i], teamTextColor, screenWidth / 2f, yPos);
            text.setCoordinateMode(TextUtils.CoordinateMode.CENTER);
            var textHeight = 15;
            text.drawNormalBoldRoboto(spriteBatch, AIText, AITextColor, screenWidth / 2f + leftOffset, yPos + textHeight);
            if (choosing && AIIndex == i) {
                var arrowSpacingFromEdge = 20;
                var textLength = 220;
                var horizontalOffset = 260;
                text.drawNormalRoboto(spriteBatch, "<", Color.WHITE, screenWidth / 2f + horizontalOffset + arrowSpacingFromEdge - textLength, yPos + textHeight);
                text.drawNormalRoboto(spriteBatch, ">", Color.WHITE, screenWidth / 2f + horizontalOffset - arrowSpacingFromEdge, yPos + textHeight);
            }
        }
        text.setCoordinateMode(TextUtils.CoordinateMode.BOTTOM_RIGHT);
        var amountHeight = (screenHeight / 2f) - 100;
        text.drawNormalBoldRoboto(spriteBatch, "Battle amount:", choosing ? Color.WHITE : amountColor, screenWidth / 2f, amountHeight);
        text.setCoordinateMode(TextUtils.CoordinateMode.CENTER);
        var textHeight = 7.5f;
        if (choosing && AIIndex == -1) {
            var arrowSpacingFromEdge = 20;
            var textLength = 125;
            var horizontalOffset = 212.5f;
            text.drawNormalRoboto(spriteBatch, "<", Color.WHITE, screenWidth / 2f + horizontalOffset + arrowSpacingFromEdge - textLength, amountHeight + textHeight);
            text.drawNormalRoboto(spriteBatch, ">", Color.WHITE, screenWidth / 2f + horizontalOffset - arrowSpacingFromEdge, amountHeight + textHeight);
        }
        text.drawNormalBoldRoboto(spriteBatch, String.valueOf(battleAmount), choosing ? amountColor : Color.WHITE, screenWidth / 2f + leftOffset, amountHeight + textHeight);
        text.setCoordinateMode(TextUtils.CoordinateMode.CENTER);
        text.drawBigBoldRoboto(spriteBatch, "Start", startColor, screenWidth / 2f, (screenHeight / 2f) - 200);
        text.setCoordinateMode(TextUtils.CoordinateMode.TOP_LEFT);
        spriteBatch.end();

    }

    public void setStartColor(Color startColor) {
        this.startColor = startColor;
    }

    public void setTeamIndex(int teamIndex) {
        this.teamIndex = teamIndex;
    }

    public void setAIIndex(int AIIndex) {
        this.AIIndex = AIIndex;
    }

    public void setAmountColor(Color amountColor) {
        this.amountColor = amountColor;
    }

    public void setTeamAAIText(String teamAAIText) {
        this.teamAAIText = teamAAIText;
    }

    public void setTeamBAIText(String teamBAIText) {
        this.teamBAIText = teamBAIText;
    }

    public void setChoosing(boolean choosing) {
        this.choosing = choosing;
    }

    public void setBattleAmount(int battleAmount) {
        this.battleAmount = battleAmount;
    }
}
