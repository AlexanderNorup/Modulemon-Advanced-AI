package dk.sdu.mmmi.modulemon.common.drawing;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import dk.sdu.mmmi.modulemon.common.OSGiFileHandle;

public class TextUtils {
    private static final Object _instanceLock = new Object();
    private static TextUtils _instance;
    private GlyphLayout glyphLayout;
    private BitmapFont titleFont;
    private BitmapFont bigRobotoFont;
    private BitmapFont normalRobotoFont;
    private BitmapFont smallRobotoFont;
    private BitmapFont bigBoldRobotoFont;
    private BitmapFont normalBoldRobotoFont;
    private BitmapFont smallBoldRobotoFont;

    private CoordinateMode currentCoordinateMode = CoordinateMode.TOP_LEFT;

    private TextUtils() {
        glyphLayout = new GlyphLayout();
        FreeTypeFontGenerator titleFontGenerator = new FreeTypeFontGenerator(new OSGiFileHandle("/fonts/Modulemon-Solid.ttf", this.getClass()));
        FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(new OSGiFileHandle("/fonts/Roboto-Medium.ttf", this.getClass()));
        // Since we are using Roboto Medium, which looks a lot like Roboto-Bold,
        // the font size for the "bold" version is set to 2 higher, to give the feeling of it appearing bigger/bold.
        FreeTypeFontGenerator fontGeneratorBold = new FreeTypeFontGenerator(new OSGiFileHandle("/fonts/Roboto-Bold.ttf", this.getClass()));

        // Font size
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 80;
        titleFont = titleFontGenerator.generateFont(parameter);

        parameter.size = 34;
        bigRobotoFont = fontGenerator.generateFont(parameter);
        parameter.size = 36;
        bigBoldRobotoFont = fontGeneratorBold.generateFont(parameter);

        parameter.size = 24;
        normalRobotoFont = fontGenerator.generateFont(parameter);
        parameter.size = 26;
        normalBoldRobotoFont = fontGeneratorBold.generateFont(parameter);

        parameter.size = 16;
        smallRobotoFont = fontGenerator.generateFont(parameter);
        parameter.size = 18;
        smallBoldRobotoFont = fontGeneratorBold.generateFont(parameter);
        fontGenerator.dispose();
    }

    public static TextUtils getInstance() {
        if (_instance == null) {
            synchronized (_instanceLock) {
                if (_instance == null) {
                    _instance = new TextUtils();
                }
            }
        }
        return _instance;
    }

    public void setCoordinateMode(CoordinateMode currentCoordinateMode) {
        this.currentCoordinateMode = currentCoordinateMode;
    }

    public CoordinateMode getCoordinateMode() {
        return currentCoordinateMode;
    }

    public void drawTitleFont(SpriteBatch batch, String text, Color color, float x, float y) {
        glyphLayout.setText(titleFont, text);
        titleFont.setColor(color);
        var adjusted = getAdjustedCoordinates(glyphLayout, x, y);
        titleFont.draw(
                batch,
                text,
                adjusted.getX(),
                adjusted.getY()
        );
    }

    public void drawBigRoboto(SpriteBatch batch, String text, Color color, float x, float y) {
        glyphLayout.setText(bigRobotoFont, text);
        bigRobotoFont.setColor(color);
        var adjusted = getAdjustedCoordinates(glyphLayout, x, y);
        bigRobotoFont.draw(
                batch,
                text,
                adjusted.getX(),
                adjusted.getY()
        );
    }

    public void drawBigBoldRoboto(SpriteBatch batch, String text, Color color, float x, float y) {
        glyphLayout.setText(bigBoldRobotoFont, text);
        bigBoldRobotoFont.setColor(color);
        var adjusted = getAdjustedCoordinates(glyphLayout, x, y);
        bigBoldRobotoFont.draw(
                batch,
                text,
                adjusted.getX(),
                adjusted.getY()
        );
    }

    public void drawNormalRoboto(SpriteBatch batch, String text, Color color, float x, float y) {
        glyphLayout.setText(normalRobotoFont, text);
        normalRobotoFont.setColor(color);
        var adjusted = getAdjustedCoordinates(glyphLayout, x, y);
        normalRobotoFont.draw(
                batch,
                text,
                adjusted.getX(),
                adjusted.getY()
        );
    }

    public void drawNormalBoldRoboto(SpriteBatch batch, String text, Color color, float x, float y) {
        glyphLayout.setText(normalBoldRobotoFont, text);
        normalBoldRobotoFont.setColor(color);
        var adjusted = getAdjustedCoordinates(glyphLayout, x, y);
        normalBoldRobotoFont.draw(
                batch,
                text,
                adjusted.getX(),
                adjusted.getY()
        );
    }

    public void drawSmallRoboto(SpriteBatch batch, String text, Color color, float x, float y) {
        glyphLayout.setText(smallRobotoFont, text);
        smallRobotoFont.setColor(color);
        var adjusted = getAdjustedCoordinates(glyphLayout, x, y);
        smallRobotoFont.draw(
                batch,
                text,
                adjusted.getX(),
                adjusted.getY()
        );
    }

    public void drawSmallBoldRoboto(SpriteBatch batch, String text, Color color, float x, float y) {
        glyphLayout.setText(smallBoldRobotoFont, text);
        smallBoldRobotoFont.setColor(color);
        var adjusted = getAdjustedCoordinates(glyphLayout, x, y);
        smallBoldRobotoFont.draw(
                batch,
                text,
                adjusted.getX(),
                adjusted.getY()
        );
    }

    private Position getAdjustedCoordinates(GlyphLayout layout, float x, float y) {
        return switch (currentCoordinateMode) {
            case TOP_LEFT -> new Position(x, y);
            case BOTTOM_RIGHT -> new Position(x - layout.width, y + layout.height);
            case CENTER -> new Position(x - (layout.width / 2f), y + (layout.height/2f));
        };
    }

    public enum CoordinateMode {
        TOP_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }
}
