package com.dji.videostreamdecodingsample;

/**
 * Created by c0114144 on 2017/12/21.
 */

public interface FlightControl {

    void update(UserInput input);

    void initialize(Drone drone);

    void destroy();
}
