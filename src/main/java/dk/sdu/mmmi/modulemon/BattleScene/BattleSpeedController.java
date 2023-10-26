package dk.sdu.mmmi.modulemon.BattleScene;

import dk.sdu.mmmi.modulemon.common.drawing.MathUtils;

public class BattleSpeedController {

    private static final int incrementAmount = 200;
    private int speed = 1000;

    public void increaseSpeed(){
        speed +=incrementAmount;
    }

    public void decreaseSpeed(){
        speed = Math.max(0, speed - incrementAmount);
    }

    public void setSpeed(int speed){
        this.speed = Math.max(0, speed);
    }

    public int getSpeed(){
        return speed;
    }

    @Override
    public String toString() {
        return "Battle delay: " + speed + "ms";
    }
}
