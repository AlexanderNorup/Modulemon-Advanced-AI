/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dk.sdu.mmmi.modulemon.NPC;

import com.badlogic.gdx.graphics.Texture;
import dk.sdu.mmmi.modulemon.CommonMap.Data.Entity;
import dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts.*;
import dk.sdu.mmmi.modulemon.common.data.GameData;
import dk.sdu.mmmi.modulemon.CommonMap.Data.World;
import dk.sdu.mmmi.modulemon.CommonMap.Services.IEntityProcessingService;

import java.util.Collection;

/**
 *
 * @author Gorm Krings
 */
public class NPCControlSystem implements IEntityProcessingService{

    @Override
    public void process(GameData gameData, World world) {

            for (Entity npc : world.getEntities(NPC.class)) {
                MovingPart movingPart = npc.getPart(MovingPart.class);
                AIControlPart controlPart = npc.getPart(AIControlPart.class);

                if (movingPart != null && controlPart != null) {
                    movingPart.setLeft(controlPart.shouldGoLeft());
                    movingPart.setRight(controlPart.shouldGoRight());
                    movingPart.setUp(controlPart.shouldGoUp());
                    movingPart.setDown(controlPart.shouldGoDown());
                }

                Collection<EntityPart> entityParts = npc.getParts();
                for (EntityPart entityPart : entityParts) {
                    entityPart.process(gameData, world, npc);
                }

                updateShape(npc);
        }
    }

    private void updateShape(Entity entity) {
        SpritePart spritePart = entity.getPart(SpritePart.class);
        PositionPart positionPart = entity.getPart(PositionPart.class);

        Texture result = null;
        switch (positionPart.getDirection()) {
            case EAST:
                result = spritePart.getRightSprite();
                break;
            case WEST:
                result = spritePart.getLeftSprite();
                break;
            case NORTH:
                result = spritePart.getUpSprite();
                break;
            case SOUTH:
                result = spritePart.getDownSprite();
                break;
            default:
                System.out.println(("Did not match any direction"));
        }
        spritePart.setCurrentSprite(result);
    }
    
}
