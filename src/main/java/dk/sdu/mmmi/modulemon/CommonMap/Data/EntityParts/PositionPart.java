/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts;

import com.badlogic.gdx.math.Vector2;

import static dk.sdu.mmmi.modulemon.CommonMap.Data.Direction.SOUTH;

import dk.sdu.mmmi.modulemon.CommonMap.Data.Direction;
import dk.sdu.mmmi.modulemon.CommonMap.Data.Entity;
import dk.sdu.mmmi.modulemon.CommonMap.Data.World;
import dk.sdu.mmmi.modulemon.common.data.GameData;

/**
 *
 * @author Alexander
 */
public class PositionPart implements EntityPart {
    private Direction direction; // facing direction in comparison to the unit circle

    /**
     * The actual position of this part
     * This is always inline with the grid
     */
    private Vector2 currentPos;

    /**
     * The visual position of this part
     */
    private Vector2 visualPos;

    private Vector2 visualOffsetPos = Vector2.Zero;

    /**
     * Where this part is trying to move. May be null, which means the part is not trying to move!
     * This is always inline with the grid
     */
    private Vector2 targetPos = null;

    public PositionPart(float x, float y) {
        currentPos = new Vector2(x,y);
        visualPos = currentPos;
        this.direction = SOUTH;
    }

    public Vector2 getCurrentPos() {
        return this.currentPos;
    }

    public void setCurrentPos(Vector2 newPos) {
        this.currentPos = newPos;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setTargetPos(Vector2 targetPos) {
        this.targetPos = targetPos;
    }

    public Vector2 getTargetPos(){
        return targetPos;
    }

    public void setVisualOffsetPos(Vector2 visualOffsetPos) {
        this.visualOffsetPos = visualOffsetPos;
    }

    public void setVisualPos(Vector2 newPos) {
        this.visualPos = newPos;
    }
    public Vector2 getPureVisualPos() {
        return visualPos;
    }

    public Vector2 getVisualPos(){
        return visualPos.cpy().add(visualOffsetPos);
    }

    @Override
    public void process(GameData gameData, World world, Entity entity) {
        
    }
}
