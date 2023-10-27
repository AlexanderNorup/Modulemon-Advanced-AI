package dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import dk.sdu.mmmi.modulemon.CommonMap.Data.Entity;
import dk.sdu.mmmi.modulemon.CommonMap.Data.World;
import dk.sdu.mmmi.modulemon.common.data.GameData;

import java.util.Collections;
import java.util.List;
import java.util.PrimitiveIterator;

public class SpritePart implements EntityPart {
    private List<Texture> upSprite;
    private List<Texture> downSprite;
    private List<Texture> leftSprite;
    private List<Texture> rightSprite;

    private List<Texture> upIdleSprite;
    private List<Texture> downIdleSprite;
    private List<Texture> leftIdleSprite;
    private List<Texture> rightIdleSprite;

    private float timer = 0;
    private float duration = 0.15f;
    private int frameIndex = 0;

    private Texture currentSprite;
    private List<Texture> currentSpritePool;

    public SpritePart(Texture upSprite, Texture downSprite, Texture leftSprite, Texture rightSprite) {
        this.upSprite = Collections.singletonList(upSprite);
        this.downSprite = Collections.singletonList(downSprite);
        this.leftSprite = Collections.singletonList(leftSprite);
        this.rightSprite = Collections.singletonList(rightSprite);
    }

    public SpritePart(
            List<Texture> upSprite,
            List<Texture> downSprite,
            List<Texture> leftSprite,
            List<Texture> rightSprite,
            List<Texture> upIdleSprite,
            List<Texture> downIdleSprite,
            List<Texture> leftIdleSprite,
            List<Texture> rightIdleSprite) {
        this.upSprite = upSprite;
        this.downSprite = downSprite;
        this.leftSprite = leftSprite;
        this.rightSprite = rightSprite;
        this.upIdleSprite = upIdleSprite;
        this.downIdleSprite = downIdleSprite;
        this.leftIdleSprite = leftIdleSprite;
        this.rightIdleSprite = rightIdleSprite;
        this.currentSpritePool = downIdleSprite;
    }

    public Texture getUpSprite(boolean isMoving) {
        if(!isMoving && upIdleSprite != null){
            updateAnimation(upIdleSprite, isMoving);
            return upIdleSprite.get(frameIndex);
        } else {
            updateAnimation(upSprite, isMoving);
            return upSprite.get(frameIndex% upSprite.size());
        }
    }

    public Texture getUpSprite() {
        return upSprite.get(0);
    }

    public Texture getDownSprite(boolean isMoving) {
        if(!isMoving && downIdleSprite != null){
            updateAnimation(downIdleSprite, isMoving);
            return downIdleSprite.get(frameIndex);
        } else {
            updateAnimation(downSprite, isMoving);
            return downSprite.get(frameIndex % downSprite.size());
        }
    }

    public Texture getDownSprite() {
        return downSprite.get(0);
    }

    public Texture getLeftSprite(boolean isMoving) {
        if(!isMoving && leftIdleSprite != null){
            updateAnimation(leftIdleSprite, isMoving);
            return leftIdleSprite.get(frameIndex);
        } else {
            updateAnimation(leftSprite, isMoving);
            return leftSprite.get(frameIndex % leftSprite.size());
        }
    }

    public Texture getLeftSprite() {
        return leftSprite.get(0);
    }

    public Texture getRightSprite(boolean isMoving) {
        if(!isMoving && rightIdleSprite != null){
            updateAnimation(rightIdleSprite, isMoving);
            return rightIdleSprite.get(frameIndex);
        } else {
            updateAnimation(rightSprite, isMoving);
            return rightSprite.get(frameIndex % rightSprite.size());
        }
    }

    public Texture getRightSprite() {
        return rightSprite.get(0);
    }

    public Texture getCurrentSprite() {
        timer += Gdx.graphics.getDeltaTime();
        return currentSprite;
    }

    public void setCurrentSprite(Texture currentSprite) {
        this.currentSprite = currentSprite;
    }

    private void updateAnimation(List<Texture> animationTextures, boolean isMoving) {
        if(timer >= duration){
            timer = 0;
            frameIndex = (frameIndex + 1) % animationTextures.size();
        }
        if(this.currentSpritePool != null) {
            if(!this.currentSpritePool.contains(animationTextures.get(frameIndex % animationTextures.size()))){
                timer = isMoving ? 0 : -1;
                frameIndex = 0;
            }
        }
        this.currentSpritePool = animationTextures;
    }

    @Override
    public void process(GameData gameData, World world, Entity entity) {

    }
}
