package com.dji.videostreamdecodingsample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import com.dji.videostreamdecodingsample.media.DJIVideoStreamDecoder;
import com.dji.videostreamdecodingsample.media.NativeHelper;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.camera.Camera;
import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;
import es.ava.aruco.MarkerDetector;

import java.io.ByteArrayOutputStream;

import java.util.Vector;

public class MainActivity extends Activity implements DJIVideoStreamDecoder.IYuvDataListener, OnClickListener {
    private static final String TAG = MainActivity.class.getName();

    //TextureViewを使用したいときはfalseに
    private static final boolean useSurface = true;

    private static final float MARKER_SIZE = (float) 0.017;

    private TextureView videostreamPreviewTtView;
    private SurfaceView videostreamPreviewSf;
    private SurfaceHolder videostreamPreviewSh;

    private Button mFinishBtn;
    private Button mFlightBtn;
    private Button markerSearchBtn;
    private Button mMediaManagerBtn;
    private OnScreenJoystick mScreenJoystickRight;
    private OnScreenJoystick mScreenJoystickLeft;

    private BaseProduct mProduct;
    private FlightControllerManager mController;
    private DJIDrone mDJIDrone;
    private Camera mCamera;
    private DJICodecManager mCodecManager;

    private MarkerDetectStatus mMarkerDetectStatus;

    private Handler handler;

    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);
        initUi();

        //フィールドの初期化
        mDJIDrone = new DJIDrone();
        mController = new FlightControllerManager(mDJIDrone);
        mController.changeController(new ManualFlightController());
        mMarkerDetectStatus = new MarkerDetectStatus(false);

        handler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mController.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDJIDrone.init(new DJIDrone.Callback() {
            @Override
            void onInitialized(boolean success, String errorMessage) {
                if (success) {
                    showToast("DJIDrone initialized");
                } else {
                    showToast(errorMessage);
                }
            }
        });
        if (useSurface) {
            initPreviewerSurfaceView();
        } else {
            initPreviewerTextureView();
        }
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, null);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
        }
        notifyStatusChange();
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        if (mCamera != null) {
            if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
            }
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
        mController.stop();
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        //finishTask();
        if (mCodecManager != null) {
            mCodecManager.destroyCodec();
        }
        mController.destroy();
        showToast("SendVirtualStickDataTask End.");
        super.onDestroy();
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUi() {
        mFinishBtn = (Button) findViewById(R.id.btn_finish);
        mFlightBtn = (Button) findViewById(R.id.btn_flight);
        markerSearchBtn = (Button) findViewById(R.id.btn_marker_search);
        mMediaManagerBtn = (Button) findViewById(R.id.btn_mediaManager);
        //前進、後退、横移動
        mScreenJoystickRight = (OnScreenJoystick) findViewById(R.id.directionJoystickRight);
        //上昇、下降、左右
        mScreenJoystickLeft = (OnScreenJoystick) findViewById(R.id.directionJoystickLeft);

        mFinishBtn.setOnClickListener(this);
        mFlightBtn.setOnClickListener(this);
        markerSearchBtn.setOnClickListener(this);
        mMediaManagerBtn.setOnClickListener(this);

        mScreenJoystickLeft.setJoystickListener(new OnScreenJoystickListener() {
            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }

                //上昇、下降の最高速度
                float verticalJoyControlMaxSpeed = 1;
                //左右向き替えの最高速度
                float yawJoyControlMaxSpeed = 30;

                //実際の速さ
                float yaw = (float) (yawJoyControlMaxSpeed * pX);
                float throttle = (float) (verticalJoyControlMaxSpeed * pY / 5);
                Log.d("left :", yaw + "-" + throttle);

                UserInput input = new UserInput();
                input.setFlightControlElements(0, 0, yaw, throttle);

                mController.updateInput(input);
            }
        });


        mScreenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }

                //前進、後退、横移動の最高速度
                float pitchJoyControlMaxSpeed = 5;
                float rollJoyControlMaxSpeed = 5;

                //実際の速さ(速すぎたためそれぞれ割り算)
                float pitch = (float) (pitchJoyControlMaxSpeed * pX / 12);
                float roll = (float) (rollJoyControlMaxSpeed * pY / 8);
                Log.d("right :", pitch + "-" + roll);

                UserInput input = new UserInput();
                input.setFlightControlElements(pitch, roll, 0, 0);

                mController.updateInput(input);
            }

        });

        videostreamPreviewTtView = (TextureView) findViewById(R.id.livestream_preview_ttv);
        videostreamPreviewSf = (SurfaceView) findViewById(R.id.livestream_preview_sf);
        if (useSurface) {
            videostreamPreviewSf.setVisibility(View.VISIBLE);
            videostreamPreviewTtView.setVisibility(View.GONE);
        } else {
            videostreamPreviewSf.setVisibility(View.GONE);
            videostreamPreviewTtView.setVisibility(View.VISIBLE);
        }
    }

    private void notifyStatusChange() {

        mProduct = VideoDecodingApplication.getProductInstance();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                //Log.d(TAG, "camera recv video data size: " + size);
                if (useSurface) {
                    DJIVideoStreamDecoder.getInstance().parse(videoBuffer, size);
                } else if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }

            }
        };

        if (null == mProduct || !mProduct.isConnected()) {
            mCamera = null;
            showToast("Disconnected");
        } else {
            if (!mProduct.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                    VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    /**
     * Init a fake texture view to for the codec manager, so that the video raw data can be received
     * by the camera
     */
    private void initPreviewerTextureView() {
        videostreamPreviewTtView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "real onSurfaceTextureAvailable");
                if (mCodecManager == null) {
                    mCodecManager = new DJICodecManager(getApplicationContext(), surface, width, height);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mCodecManager != null) mCodecManager.cleanSurface();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    /**
     * Init a surface view for the DJIVideoStreamDecoder
     */
    private void initPreviewerSurfaceView() {
        videostreamPreviewSh = videostreamPreviewSf.getHolder();
        videostreamPreviewSh.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");

                NativeHelper.getInstance().init();
                DJIVideoStreamDecoder.getInstance().init(getApplicationContext(), videostreamPreviewSh.getSurface());
                DJIVideoStreamDecoder.getInstance().setYuvDataListener(MainActivity.this);
                DJIVideoStreamDecoder.getInstance().resume();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                DJIVideoStreamDecoder.getInstance().changeSurface(holder.getSurface());
                //Log.d(TAG, "surfaceChanged" + ", surface name:" + holder.getSurface().toString());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                DJIVideoStreamDecoder.getInstance().stop();
                DJIVideoStreamDecoder.getInstance().destroy();
                NativeHelper.getInstance().release();
                Log.d(TAG, "surfaceDestroyed");
            }
        });
    }

    @Override
    public void onYuvDataReceived(byte[] yuvFrame, int width, int height) {
        //In this demo, we test the YUV data by saving it into JPG files.

        if (DJIVideoStreamDecoder.getInstance().frameIndex % 60 == 0) {
            byte[] y = new byte[width * height];
            byte[] u = new byte[width * height / 4];
            byte[] v = new byte[width * height / 4];
            byte[] nu = new byte[width * height / 4];
            byte[] nv = new byte[width * height / 4];
            System.arraycopy(yuvFrame, 0, y, 0, y.length);
            for (int i = 0; i < u.length; i++) {
                v[i] = yuvFrame[y.length + 2 * i];
                u[i] = yuvFrame[y.length + 2 * i + 1];
            }
            int uvWidth = width / 2;
            int uvHeight = height / 2;
            for (int j = 0; j < uvWidth / 2; j++) {
                for (int i = 0; i < uvHeight / 2; i++) {
                    byte uSample1 = u[i * uvWidth + j];
                    byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
                    byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
                    byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
                    nu[2 * (i * uvWidth + j)] = uSample1;
                    nu[2 * (i * uvWidth + j) + 1] = uSample1;
                    nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
                    nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
                    nv[2 * (i * uvWidth + j)] = vSample1;
                    nv[2 * (i * uvWidth + j) + 1] = vSample1;
                    nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
                    nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
                }
            }
            //nv21test
            byte[] bytes = new byte[yuvFrame.length];
            System.arraycopy(y, 0, bytes, 0, y.length);
            for (int i = 0; i < u.length; i++) {
                bytes[y.length + (i * 2)] = nv[i];
                bytes[y.length + (i * 2) + 1] = nu[i];
            }

            // YuvImageにする
            YuvImage yuv = new YuvImage(bytes, ImageFormat.NV21, width, height, null);

            // jpegにする
            ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 70, jpeg);

            // Bitmapにする
            Bitmap bmp = BitmapFactory.decodeByteArray(jpeg.toByteArray(), 0, jpeg.size());

            // Matにする
            Mat yuvToMat = new Mat();
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
            org.opencv.android.Utils.bitmapToMat(bmp, yuvToMat);

            //YUV形式を最終的にMat形式に変換したらマーカー検出開始
            markerDetect(yuvToMat);
            //かなり処理が重いが素早くsurfaceの切り替えを行えばデコーディング自体は可能


            Log.d(TAG,
                    "onYuvDataReceived: frame index: "
                            + DJIVideoStreamDecoder.getInstance().frameIndex);


            //検出中（画像認識中）のリアルタイムなカメラ映像をメイン画面に
            Canvas canvas = videostreamPreviewSh.lockCanvas();
            canvas.drawBitmap(bmp, new Rect(0, 0, width, height), new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), null);
            videostreamPreviewSh.unlockCanvasAndPost(canvas);
        }
    }

    /**
     * ArUcoライブラリによるマーカー検出開始
     * 検出したらそれに合わせた自律飛行開始
     *
     * @param rgba
     */
    private void markerDetect(Mat rgba) {
        //マーカー検出の準備
        MarkerDetector mDetector = new MarkerDetector();
        Vector<Marker> detectedMarkers = new Vector<>();
        CameraParameters camParams = new CameraParameters();

        //マーカー検出開始
        mDetector.detect(rgba, detectedMarkers, camParams, MARKER_SIZE);

        Log.d("marker", "detected " + detectedMarkers.size());

        //マーカー発見時の処理
        for (int i = 0; i < detectedMarkers.size(); i++) {
            Marker marker = detectedMarkers.get(i);
            Log.d("detect", "processed " + i + "id=" + marker.getMarkerId());
            if (detectedMarkers.size() != 0) {
                showToast("marker detected.");
                //マーカーを検出した
                mMarkerDetectStatus.setDetected(true);
                Log.d("detectFlag:", mMarkerDetectStatus.isDetected() + "");
                //検出後の自律飛行
                mController.changeController(new AutoFlightController(mMarkerDetectStatus));
            }
            captureAction();
        }
        Log.d("detectFlag:", mMarkerDetectStatus.isDetected() + "");
    }

    // Method for taking photo
    private void captureAction() {

        final Camera camera = VideoDecodingApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null) {
                                            showToast("take photo: success");
                                        } else {
                                            showToast(djiError.getDescription());
                                        }
                                    }
                                });
                            }
                        }, 0);
                    }
                }
            });
        }
    }

    /**
     * 仮想スティックの表示切替
     *
     * @param flag
     */
    private void joystickDisplaySwitch(boolean flag) {
        if (flag) {
            findViewById(R.id.directionJoystickLeft).setVisibility(View.VISIBLE);
            findViewById(R.id.directionJoystickRight).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.directionJoystickLeft).setVisibility(View.INVISIBLE);
            findViewById(R.id.directionJoystickRight).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * OnClickListener
     */
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_finish:
                mController.changeController(new ManualFlightController());
                DJIVideoStreamDecoder.getInstance().changeSurface(videostreamPreviewSh.getSurface());
                joystickDisplaySwitch(true);
                break;
            case R.id.btn_flight:
                mMarkerDetectStatus.setDetected(false);
                mController.changeController(new AutoFlightController(mMarkerDetectStatus));
                break;
            case R.id.btn_marker_search:
                DJIVideoStreamDecoder.getInstance().changeSurface(null);
                joystickDisplaySwitch(false);
                showToast("Marker Tracking.");
                break;
            case R.id.btn_mediaManager:
                //TextureView（useSurface = false）を使うときは/* */を外す
                /*
                Intent intent = new Intent(this, MediaManagerActivity.class);
                startActivity(intent);
                */
                break;
            default:
                break;
        }
    }
}
