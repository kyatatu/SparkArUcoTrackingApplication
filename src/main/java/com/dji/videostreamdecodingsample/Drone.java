package com.dji.videostreamdecodingsample;

/**
 * Created by c0114144 on 2018/01/02.
 */

public interface Drone {
    void init(Callback callback);
    void send(UserInput input);

    abstract class Callback{
        abstract void onInitialized(boolean success, String errorMessage);
    }
}
