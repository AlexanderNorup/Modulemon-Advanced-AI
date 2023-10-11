package dk.sdu.mmmi.modulemon.common.drawing;

public class MathUtils {
    public static float map(float value, float inputMin, float inputMax, float outputMin, float outputMax){
        return (value - inputMin) * (outputMax - outputMin) / (inputMax - inputMin) + outputMin;
    }

    public static float moveTowards(float initial, float target, float animationSpeed, float dt){
        float diff = initial - target;
        if(Math.abs(diff) < Math.max(animationSpeed*dt, .25f)){
            return target;
        }

        if(diff > 0){
            //We have to move left
            return initial - animationSpeed * dt;
        }else {
            //We have to move right
            return initial + animationSpeed * dt;
        }
    }
}
