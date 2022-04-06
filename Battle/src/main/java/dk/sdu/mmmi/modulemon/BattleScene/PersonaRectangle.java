package dk.sdu.mmmi.modulemon.BattleScene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.Random;

public class PersonaRectangle {
    private float width;
    private float height;

    private float[] rectBaseCoordinates;
    private float[] rectDriftOffset;
    private float[] rectTimes;

    private float borderWidth;
    private Color borderColor;
    private Color fillColor;

    private float driftLevel;
    private float animationSpeed;

    public PersonaRectangle(float x, float y, float width, float height) {
        this.width = width;
        this.height = height;

        //Defaults
        borderWidth = 2;
        borderColor = Color.BLACK;
        fillColor = Color.WHITE;
        driftLevel = 14;
        animationSpeed = 0.8f;

        //Setup rect time and coordinates
        rebuildBaseCoordinates(x, y);

        Random r = new Random();
        this.rectTimes = new float[]{
                randomFloat(r, -10, 10),
                randomFloat(r, -10, 10),
                randomFloat(r, -10, 10),
                randomFloat(r, -10, 10),
                randomFloat(r, -10, 10),
                randomFloat(r, -10, 10),
                randomFloat(r, -10, 10),
                randomFloat(r, -10, 10)
        };
        this.rectDriftOffset = new float[this.rectBaseCoordinates.length];
        updateDrift(1);
    }

    public void draw(ShapeRenderer shapeRenderer, float dt) {
        updateDrift(dt);
        ImmediateModeRenderer renderer = shapeRenderer.getRenderer();

        // x1, y1 = Bottom left corner
        float x1 = this.rectBaseCoordinates[0] - this.rectDriftOffset[0];
        float y1 = this.rectBaseCoordinates[1] - this.rectDriftOffset[1];
        // x2, y2 = Bottom right corner
        float x2 = this.rectBaseCoordinates[2] + this.rectDriftOffset[2];
        float y2 = this.rectBaseCoordinates[3] - this.rectDriftOffset[3];
        // x3, y3 = Top right corner
        float x3 = this.rectBaseCoordinates[4] + this.rectDriftOffset[4];
        float y3 = this.rectBaseCoordinates[5] + this.rectDriftOffset[5];
        // x4, y4 = Top left corner
        float x4 = this.rectBaseCoordinates[6] - this.rectDriftOffset[6];
        float y4 = this.rectBaseCoordinates[7] + this.rectDriftOffset[7];

        //Draw border
        if(this.borderWidth > 0) {
            renderer.color(this.borderColor);
            renderer.vertex(x1 - this.borderWidth, y1 - this.borderWidth, 0.0F);
            renderer.color(this.borderColor);
            renderer.vertex(x2 + this.borderWidth, y2 - this.borderWidth, 0.0F);
            renderer.color(this.borderColor);
            renderer.vertex(x4 - this.borderWidth, y4 + this.borderWidth, 0.0F);
            renderer.color(this.borderColor);
            renderer.vertex(x2 + this.borderWidth, y2 - this.borderWidth, 0.0F);
            renderer.color(this.borderColor);
            renderer.vertex(x3 + this.borderWidth, y3 + this.borderWidth, 0.0F);
            renderer.color(this.borderColor);
            renderer.vertex(x4 - this.borderWidth, y4 + this.borderWidth, 0.0F);
        }

        //Draw fill
        renderer.color(this.fillColor);
        renderer.vertex(x1, y1, 0.0F);
        renderer.color(this.fillColor);
        renderer.vertex(x2, y2, 0.0F);
        renderer.color(this.fillColor);
        renderer.vertex(x4, y4, 0.0F);
        renderer.color(this.fillColor);
        renderer.vertex(x2, y2, 0.0F);
        renderer.color(this.fillColor);
        renderer.vertex(x3, y3, 0.0F);
        renderer.color(this.fillColor);
        renderer.vertex(x4, y4 , 0.0F);
    }

    public float getBaseX() {
        return this.rectBaseCoordinates[0];
    }

    public void setBaseX(float baseX) {
        if(baseX != this.rectBaseCoordinates[0])
            rebuildBaseCoordinates(baseX,this.rectBaseCoordinates[1]);
    }

    public float getBaseY() {
        return this.rectBaseCoordinates[1];
    }

    public void setBaseY(float baseY) {
        if(baseY != this.rectBaseCoordinates[1])
            rebuildBaseCoordinates(this.rectBaseCoordinates[0], baseY);
    }

    public void setBasePosition(Position pos){
        if(pos.getX() != this.rectBaseCoordinates[0]
            || pos.getY() != this.rectBaseCoordinates[1])
        rebuildBaseCoordinates(pos.getX(), pos.getY());
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(float borderWidth) {
        this.borderWidth = borderWidth;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public float getDriftLevel() {
        return driftLevel;
    }

    public void setDriftLevel(float driftLevel) {
        this.driftLevel = driftLevel;
    }

    public float getAnimationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    /// PRIVATE METHODS

    private void updateDrift(float dt) {
        for (int i = 0; i < this.rectTimes.length; i++) {
            this.rectDriftOffset[i] = getDriftLevel(this.rectTimes[i]);
            this.rectTimes[i] += this.animationSpeed * dt;
        }
    }

    private void rebuildBaseCoordinates(float baseX, float baseY){
        this.rectBaseCoordinates = new float[]{
                baseX, baseY, //Bottom left corner
                baseX + width, baseY, //Bottom right corner
                baseX + width, baseY + height, //Top right corner
                baseX, baseY + height //Top left corner
        };
    }

    private float getDriftLevel(float time){
        return map(getRawDriftLevel(time), -2, 2, 0, this.driftLevel);
    }

    private float getRawDriftLevel(float time) {
        return (float) (Math.sin(2 * time) + Math.sin(Math.PI * time));
    }

    private float randomFloat(Random random, int min, int max) {
        return min + random.nextFloat() * (max - min);
    }

    private float map(float value, float inputMin, float inputMax, float outputMin, float outputMax){
        return (value - inputMin) * (outputMax - outputMin) / (inputMax - inputMin) + outputMin;
    }
}
