package dk.sdu.mmmi.modulemon.common.data;

import com.badlogic.gdx.graphics.Texture;
import dk.sdu.mmmi.modulemon.common.data.entityparts.EntityPart;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Entity implements Serializable {
    private final UUID ID = UUID.randomUUID();

    private float redValue = 1;
    private float greenValue = 1;
    private float blueValue = 1;
    private float alphaValue = 1;

    private float posX;
    private float posY;

    private Texture spriteTexture = null;

    private float[] shapeX = new float[4];
    private float[] shapeY = new float[4];
    private float radius;
    private Map<Class, EntityPart> parts;
    
    public Entity() {
        parts = new ConcurrentHashMap<Class, EntityPart>();
    }
    
    public void add(EntityPart part) {
        parts.put(part.getClass(), part);
    }
    
    public void remove(Class partClass) {
        parts.remove(partClass);
    }
    
    public <E extends EntityPart> E getPart(Class partClass) {
        return (E) parts.get(partClass);
    }
    
    public void setRadius(float r){
        this.radius = r;
    }
    
    public float getRadius(){
        return radius;
    }

    public String getID() {
        return ID.toString();
    }

    public float[] getShapeX() {
        return shapeX;
    }

    public void setShapeX(float[] shapeX) {
        this.shapeX = shapeX;
    }

    public float[] getShapeY() {
        return shapeY;
    }

    public void setShapeY(float[] shapeY) {
        this.shapeY = shapeY;
    }

    public float getPosX() {
        return posX;
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public float getPosY() {
        return posY;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public Texture getSpriteTexture() {
        return spriteTexture;
    }

    public void setSpriteTexture(Texture spriteTexture) {
        this.spriteTexture = spriteTexture;
    }

    public float getRedValue() {
        return redValue;
    }

    public float getGreenValue() {
        return greenValue;
    }

    public float getBlueValue() {
        return blueValue;
    }

    public float getAlphaValue() {
        return alphaValue;
    }

    public void setRedValue(float redValue) {
        this.redValue = redValue;
    }

    public void setGreenValue(float greenValue) {
        this.greenValue = greenValue;
    }

    public void setBlueValue(float blueValue) {
        this.blueValue = blueValue;
    }

    public void setAlphaValue(float alphaValue) {
        this.alphaValue = alphaValue;
    }
}