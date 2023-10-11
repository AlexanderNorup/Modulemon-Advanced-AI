/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.sdu.mmmi.modulemon.CommonMap.Data.EntityParts;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dk.sdu.mmmi.modulemon.CommonMap.Data.Direction;
import dk.sdu.mmmi.modulemon.CommonMap.Data.Entity;
import dk.sdu.mmmi.modulemon.CommonMap.Data.World;

import static dk.sdu.mmmi.modulemon.CommonMap.Data.Direction.*;

import dk.sdu.mmmi.modulemon.common.data.GameData;

import java.util.LinkedList;

public class MovingPart implements EntityPart {

    private boolean left, right, up, down;
    private Direction bufferedDirection = null;
    private LinkedList<Direction> queuedDirections = new LinkedList<>();

    /**
     * How far along the movement is.
     * 0          : Not started (ready to begin moving)
     * >0 && < 1  : Moving in-progress
     * >=1        : Arrived at target
     */
    private float movingTimer = 0;

    public void setLeft(boolean left) {
        if (left != this.left) {
            this.left = left;
            if (left) {
                bufferedDirection = WEST;
                queuedDirections.addFirst(WEST);
            } else {
                queuedDirections.removeIf(x -> x == WEST);
            }
        }
    }

    public void setRight(boolean right) {
        if (right != this.right) {
            this.right = right;
            if (right) {
                bufferedDirection = EAST;
                queuedDirections.addFirst(EAST);
            } else {
                queuedDirections.removeIf(x -> x == EAST);
            }
        }
    }

    public void setUp(boolean up) {
        if (up != this.up) {
            this.up = up;
            if (up) {
                bufferedDirection = NORTH;
                queuedDirections.addFirst(NORTH);
            } else {
                queuedDirections.removeIf(x -> x == NORTH);
            }
        }
    }

    public void setDown(boolean down) {
        if (down != this.down) {
            this.down = down;
            if (down) {
                bufferedDirection = SOUTH;
                queuedDirections.addFirst(SOUTH);
            } else {
                queuedDirections.removeIf(x -> x == SOUTH);
            }
        }
    }

    private boolean anyDirectionKeyPressed() {
        return down || up || left || right;
    }

    static final float scale = 4;
    static final float gridSize = 16 * scale;

    static final float movementSpeed = 5f;
    public void process(GameData gameData, World world, Entity entity) {
        PositionPart positionPart = entity.getPart(PositionPart.class);
        if (positionPart == null) {
            return;
        }

        float dt = gameData.getDelta();

        Vector2 currentPos = positionPart.getCurrentPos();

        if (positionPart.getTargetPos() != null) {
            float velocity = movementSpeed * dt;
            movingTimer += velocity;
        }

        float movementProgress = movingTimer;

        do{
            if (positionPart.getTargetPos() != null) {
                // We have a goal and is going to do some movement.

                // Movement progress may exceed 1. We don't want to move too far ahead, so we set cap it at one.
                float cappedMovementProgress = Math.min(1, movementProgress);
                movementProgress -= cappedMovementProgress; // If it capped above, there will still be some left in this variable.

                var newVisualPosition = positionPart.getCurrentPos().cpy().interpolate(positionPart.getTargetPos(), cappedMovementProgress, Interpolation.linear);
                positionPart.setVisualPos(newVisualPosition);
            }

            // Did we get there?
            if(movingTimer >= 1){
                // We got there!
                positionPart.setCurrentPos(positionPart.getTargetPos());
                positionPart.setTargetPos(null);

                movingTimer = Math.max(movingTimer-1f, 0); // Reset so can move again.
            }

            if(positionPart.getTargetPos() == null){
                if(!this.setNextTargetPos(positionPart)){
                    // We failed to set a next position. This must mean the entity dosn't want to go any further.
                    //  Therefore, we break the loop
                    movingTimer = 0; // Stop the timer as well
                    break;
                }
            }
        }while(movementProgress > 0);
    }

    /**
     * @param positionPart The position part of which to set the potential target position from input.
     * @return true if a new target position is set. Otherwise false.
     */
    private boolean setNextTargetPos(PositionPart positionPart) {
        if (positionPart.getTargetPos() == null && (bufferedDirection != null || !queuedDirections.isEmpty())) {
            // Not currently moving, ready to begin
            Direction directionToMoveTowards = null;
            if (bufferedDirection != null) {
                directionToMoveTowards = bufferedDirection;
                bufferedDirection = null;
            } else {
                directionToMoveTowards = queuedDirections.getFirst();
            }
            var currentPos = positionPart.getCurrentPos();
            var newTargetX = currentPos.x;
            var newTargetY = currentPos.y;
            switch (directionToMoveTowards) {
                case WEST:
                    newTargetX -= gridSize;
                    positionPart.setDirection(WEST);
                    break;
                case EAST:
                    newTargetX += gridSize;
                    positionPart.setDirection(EAST);
                    break;
                case NORTH:
                    newTargetY += gridSize;
                    positionPart.setDirection(NORTH);
                    break;
                case SOUTH:
                    newTargetY -= gridSize;
                    positionPart.setDirection(SOUTH);
                    break;
            }

            positionPart.setTargetPos(new Vector2(newTargetX, newTargetY));
            return true;
        }
        return false;
    }
}
