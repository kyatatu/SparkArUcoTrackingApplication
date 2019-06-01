package com.dji.videostreamdecodingsample;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by c0114144 on 2017/12/21.
 */

public class FlightControllerManager {

    private Timer mUpdateTimer;
    private FlightControl currentController;
    private Drone mDrone;
    private UserInput lastInput;

    public FlightControllerManager(Drone drone) {
        this.mDrone = drone;
        this.lastInput = new UserInput();
        lastInput.setFlightControlElements(0, 0, 0, 0);
    }

    public void start(){
        if(mUpdateTimer == null){
            mUpdateTimer = new Timer();
            mUpdateTimer.schedule(new SendVirtualStickDataTask(), 0, 200);
        }
    }

    public void stop(){
        if(null != mUpdateTimer){
            mUpdateTimer.cancel();
            mUpdateTimer.purge();
        }
    }

    public void destroy(){
        if(null != currentController) currentController.destroy();
        stop();
    }

    public void changeController(FlightControl newController){
        if(null != currentController) currentController.destroy();
        this.lastInput.resetElements();
        this.currentController = newController;
        currentController.initialize(mDrone);
    }

    public void updateInput(UserInput input){
        this.lastInput = input;
    }

    private class SendVirtualStickDataTask extends TimerTask{
        @Override
        public void run() {
            currentController.update(lastInput);
        }
    }
}
