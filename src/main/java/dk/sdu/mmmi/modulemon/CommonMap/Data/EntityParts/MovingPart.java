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
import dk.sdu.mmmi.modulemon.common.animations.BaseAnimation;
import static dk.sdu.mmmi.modulemon.CommonMap.Data.Direction.*;

import dk.sdu.mmmi.modulemon.common.data.GameData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

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
        if(left != this.left) {
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
        if(right != this.right) {
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
        if(up != this.up) {
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
        if(down != this.down) {
            this.down = down;
            if (down) {
                bufferedDirection = SOUTH;
                queuedDirections.addFirst(SOUTH);
            } else {
                queuedDirections.removeIf(x -> x == SOUTH);
            }
        }
    }

    private boolean anyDirectionKeyPressed(){
        return down || up || left || right;
    }

    public void process(GameData gameData, World world, Entity entity) {
        PositionPart positionPart = entity.getPart(PositionPart.class);
        if(positionPart == null){
            return;
        }

        float dt = gameData.getDelta();
        float scale = 4;
        float gridSize = 16 * scale;

        Vector2 currentPos = positionPart.getCurrentPos();

        if(movingTimer <= 0 && (bufferedDirection != null || !queuedDirections.isEmpty())) {
            // Not currently moving, ready to begin
            Direction directionToMoveTowards = null;
            if(bufferedDirection != null){
                directionToMoveTowards = bufferedDirection;
                bufferedDirection = null;
            }else {
                directionToMoveTowards = queuedDirections.getFirst();
            }

            var newTargetX = currentPos.x;
            var newTargetY = currentPos.y;
            switch (directionToMoveTowards){
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
        }


        if(positionPart.getTargetPos() != null){
            // We have a target to move towards. Start by updating the moving timer!
            float movementSpeed = 5f;
            movingTimer += movementSpeed * dt;

            if(movingTimer >= 1){
                // We reached our target!
                positionPart.setCurrentPos(positionPart.getTargetPos());
                positionPart.setVisualPos(positionPart.getTargetPos());
                positionPart.setTargetPos(null);

                movingTimer = 0; // Reset so can move again.
                if(!anyDirectionKeyPressed() && bufferedDirection != null){
                    bufferedDirection = null;
                }
            }else{

                // We have not reached the target yet. Move towards it!
                //  Even though interpolate() returns Vector, it also modifies itself. Used for chaining I guess, but how I hate methods with side-effects.
                //  And they're apprently too cool for .close(), so they do .cpy() instead.
                var newVisualPosition = positionPart.getPureVisualPos().cpy().interpolate(positionPart.getTargetPos(), movingTimer, Interpolation.linear);
                positionPart.setVisualPos(newVisualPosition);
            }
        }else{
            // Not moving, or movement is cancelled. Reset moving timer, so we can move again!
            movingTimer = 0;
            positionPart.setVisualPos(currentPos);
        }
    }


}
