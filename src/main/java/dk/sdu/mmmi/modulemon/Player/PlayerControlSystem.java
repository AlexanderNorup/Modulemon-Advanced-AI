package dk.sdu.mmmi.modulemon.Player;

import com.badlogic.gdx.graphics.Texture;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.*;
import dk.sdu.mmmi.modulemon.CommonMap.Data.Entity;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.CommonMap.Data.World;
import dk.sdu.mmmi.modulemon.CommonMap.Services.IEntityProcessingService;
import dk.sdu.mmmi.modulemon.common.drawing.Position;

import java.util.Collection;

import static dk.sdu.mmmi.modulemon.common.data.GameKeys.*;

public class PlayerControlSystem implements IEntityProcessingService {
    @Override
    public void process(GameData gameData, World world) {
        for (Entity player : world.getEntities(Player.class)) {
            MovingPart movingPart = player.getPart(MovingPart.class);
            var isMoving = false;
            if (movingPart != null) {
                movingPart.setLeft(gameData.getKeys().isDown(LEFT));
                movingPart.setRight(gameData.getKeys().isDown(RIGHT));
                movingPart.setUp(gameData.getKeys().isDown(UP));
                movingPart.setDown(gameData.getKeys().isDown(DOWN));
                isMoving = movingPart.anyDirectionKeyPressed();
            }

            Collection<EntityPart> entityParts = player.getParts();
            for (EntityPart entityPart : entityParts) {
                entityPart.process(gameData, world, player);
            }

            updateShape(player, isMoving);
        }
    }

    private void updateShape(Entity entity, boolean isMoving) {

        SpritePart spritePart = entity.getPart(SpritePart.class);
        PositionPart positionPart = entity.getPart(PositionPart.class);


        Texture result = null;
        switch (positionPart.getDirection()) {
            case EAST:
                result = spritePart.getRightSprite(isMoving);
                break;
            case WEST:
                result = spritePart.getLeftSprite(isMoving);
                break;
            case NORTH:
                result = spritePart.getUpSprite(isMoving);
                break;
            case SOUTH:
                result = spritePart.getDownSprite(isMoving);
                break;
            default:
                System.out.println(("Did not match any direction"));
        }

        spritePart.setCurrentSprite(result);
        //entity.setSpriteTexture(result);
    }


}
