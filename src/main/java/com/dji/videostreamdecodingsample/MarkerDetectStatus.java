package com.dji.videostreamdecodingsample;

/**
 * Created by c0114144 on 2018/01/13.
 */

public class MarkerDetectStatus {
    private boolean detected;

    public MarkerDetectStatus(boolean detected) {
        this.detected = detected;
    }

    public void setDetected(boolean detected) {
        this.detected = detected;
    }

    public boolean isDetected() {
        return this.detected;
    }
}
