package com.dji.videostreamdecodingsample;

import android.util.Log;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * Created by c0114144 on 2017/12/21.
 */

public class DJIDrone implements Drone{

    private FlightController mFlightController;
    private FlightAssistant mFlightAssistant;

    public DJIDrone(){
    }

    @Override
    public void init(final Callback callback){
        //接続されたドローンの情報を受け取る
        final Aircraft aircraft = VideoDecodingApplication.getAircraftInstance();
        mFlightAssistant = new FlightAssistant();

        //接続してなければ
        if (aircraft == null || !aircraft.isConnected()) {
            mFlightController = null;
            //接続されたらフライトコントローラーを初期化
        } else {
            mFlightController = aircraft.getFlightController();
            mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            //障害物検知センサをオンに設定する
            mFlightAssistant.setCollisionAvoidanceEnabled(true, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                }
            });
        }

        if (mFlightController != null) {

            mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        callback.onInitialized(false, djiError.getDescription());
                    } else {
                        callback.onInitialized(true, null);
                    }
                }
            });
        }
    }

    @Override
    public void send(UserInput input){
        //フライトコントローラーにスティックのピッチ、ロール、ヨー、スロットルの情報を渡す
        if (mFlightController != null) {
            Log.d("DJIDrone", "send");
            mFlightController.sendVirtualStickFlightControlData(
                    new FlightControlData(
                            input.getPitch(), input.getRoll(), input.getYaw(), input.getThrottle()
                    ), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                        }
                    }
            );
        }
        Log.d("DJIDrone", "pitch:" + input.getPitch() + ", roll:" + input.getRoll() + ", yaw:" + input.getYaw() + "throttle:" + input.getThrottle());
    }
}
