package com.dji.videostreamdecodingsample;

import android.util.Log;

/**
 * Created by c0114144 on 2017/12/21.
 */

public class ManualFlightController implements FlightControl {

    private Drone mDrone;

    public ManualFlightController() {
        Log.d("controller", "manual");
    }

    @Override
    public void update(UserInput input) {
        mDrone.send(input);
    }

    @Override
    public void initialize(Drone drone) {
        mDrone = drone;
        Log.d("manual", "initialized");
    }

    @Override
    public void destroy() {
        mDrone = null;
    }
}
