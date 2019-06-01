package com.dji.videostreamdecodingsample;

import android.util.Log;

/**
 * Created by c0114144 on 2017/12/21.
 */

public class AutoFlightController implements FlightControl {

    protected Drone mDrone;
    private long startTime;
    private long stopTime;
    private MarkerDetectStatus markerDetectStatus;

    public AutoFlightController(MarkerDetectStatus status) {
        this.markerDetectStatus = status;
        Log.d("controller", "auto");
    }

    @Override
    public void update(UserInput input) {
        stopTime = System.currentTimeMillis();
        int currentTime = (int) ((stopTime - startTime) / 1000);
        Log.d("currentTime:", currentTime + "");

        //検出できている時の飛行処理
        if (markerDetectStatus.isDetected()) {
            input.resetElements();
            if (currentTime <= 5) {
                input.setFlightControlElements(0, 0, -30, 0);
            } else if (currentTime <= 10) {
                input.setFlightControlElements(0, (float) 0.2, 0, 0);
            } else if (currentTime <= 20) {
                input.setFlightControlElements(0, 0, 30, 0);
            } else {
                input.resetElements();
                //検出飛行終了
                markerDetectStatus.setDetected(false);
            }
        } else {
            //検出途中（探索中）の飛行処理

            //実験用の飛行処理
            if (currentTime <= 60) {
                input.setFlightControlElements(0, (float)0.3, (float)7.5, 0);
            } else {
                //1回ボタンを押した時の飛行終了
                input.resetElements();
            }
        }
        //ドローンに送信
        mDrone.send(input);
        Log.d("auto", "update");
    }

    @Override
    public void initialize(Drone drone) {
        mDrone = drone;
        //ドローンが初期化された時点の時間
        startTime = System.currentTimeMillis();
        Log.d("auto", "initialized");
    }

    @Override
    public void destroy() {
        mDrone = null;
    }
}
