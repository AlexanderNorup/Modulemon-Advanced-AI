package dk.sdu.mmmi.modulemon.common.drawing;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import dk.sdu.mmmi.modulemon.common.SettingsRegistry;
import dk.sdu.mmmi.modulemon.common.services.IGameSettings;

public class DrawingUtils {

    public static void borderedRect(ShapeRenderer shapeRenderer, float x, float y, float width, float height, Color borderColor, Color fillColor, float borderWidth ){
        //Border box
        shapeRenderer.setColor(borderColor);
        shapeRenderer.rect(
                x - borderWidth,
                 y - borderWidth,
                width + borderWidth*2,
                height + borderWidth*2
        );

        //Filler
        shapeRenderer.setColor(fillColor);
        shapeRenderer.rect(
                x,
                y,
                width,
                height
        );
    }

    public static Rectangle createRectangle(Class<? extends Rectangle> clazz, float x, float y, float width, float height) {
        try {
            return (Rectangle) clazz.getDeclaredConstructors()[0].newInstance(x, y, width, height);
        } catch (Exception ex) {
            System.out.println("[WARNING] Failed to create rectangles of type: " + clazz.getName());
        }
        //Default to regular rectangle
        return new Rectangle(x, y, width, height);
    }

    public static Rectangle createRectangle(IGameSettings settings, float x, float y, float width, float height) {
        if((Boolean) settings.getSetting(SettingsRegistry.getInstance().getRectangleStyleSetting())){
            return createRectangle(PersonaRectangle.class, x, y, width, height);
        }else{
            return createRectangle(Rectangle.class, x, y, width, height);
        }
    }
}
