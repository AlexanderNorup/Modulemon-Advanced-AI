package dk.sdu.mmmi.modulemon.BattleScene;

public class BattleSpeedController {

    private static final int incrementAmount = 100;
    private int speed = 1500;

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
        return "Min. turn delay: " + speed + "ms";
    }
}
