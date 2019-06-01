package com.dji.videostreamdecodingsample;

import android.util.Log;

/**
 * Created by c0114144 on 2017/12/22.
 */

public class UserInput {
    private float pitch;
    private float roll;
    private float yaw;
    private float throttle;

    public UserInput() {
        setFlightControlElements(0, 0, 0, 0);
    }

    public void setFlightControlElements(float pitch, float roll, float yaw, float throttle){
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
        this.throttle = throttle;

        Log.d("pitch:", + pitch + ", roll:" + roll + ", yaw:" + yaw + ", throttle:" + throttle);
    }

    public void resetElements(){
        pitch = 0;
        roll = 0;
        yaw = 0;
        throttle = 0;
    }

    public float getPitch() {
        return pitch;
    }

    public float getRoll() {
        return roll;
    }

    public float getYaw() {
        return yaw;
    }

    public float getThrottle() {
        return throttle;
    }
}
