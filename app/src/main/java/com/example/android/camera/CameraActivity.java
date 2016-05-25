/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

// ----------------------------------------------------------------------

public class CameraActivity extends Activity {
    private CameraPreview mPreview;
    Camera mCamera;
    private int mOrientation;
    private int mOrientationCompensation;
    private int mDisplayOrientation;


    // The first rear facing camera
    int defaultCameraId;
    private MyOrientationEventListener mOrientationListener;
    private ViewFinderView mViewFinderView;

    private ImageView mouthBox;
    private int camId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        // mPreview = new Preview(this);
        setContentView(R.layout.main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mPreview = (CameraPreview) findViewById(R.id.surface_view);
        mViewFinderView = (ViewFinderView) findViewById(R.id.viewfinder_view);
        // Find the total number of cameras available
        Button button = (Button) findViewById(R.id.btn_face_detection);

        final Button crop_btn = (Button) findViewById(R.id.btn_crop);
        crop_btn.setText("Crop");
        crop_btn.setEnabled(false);

        mouthBox = (ImageView) findViewById(R.id.imageView);
        mPreview.setMouthBox(mouthBox);


        button.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                mCamera.startFaceDetection();
                //mViewFinderView.setDisplayOrientation(mDisplayOrientation);
                Log.d("facedetection", "Button Clicked");
                v.setEnabled(false);
                crop_btn.setEnabled(true);
            }
        });

        crop_btn.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                mViewFinderView.setCrop();
                String buttonText = crop_btn.getText().toString();
                if(buttonText=="Crop") crop_btn.setText("Stop");
                else crop_btn.setText("Crop");
                Log.d("cropping", "Button Clicked");
            }
        });

        //mOrientationListener = new MyOrientationEventListener(this);
        //mOrientationListener.enable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int numCams = Camera.getNumberOfCameras();

        camId=0;
        if(numCams > 0){

            //search for front camera
            for (int i = 0; i < numCams; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    camId = i;
                    break;
                }
            }

            try{
                mCamera = Camera.open(camId);
                //mCamera.startPreview();
                mPreview.setCamera(mCamera);
            } catch (RuntimeException ex){
                Toast.makeText(this, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
            }
        }


        mPreview.setMouthBox(mCamera, mouthBox, camId, mPreview);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }

    public static int roundOrientation(int orientation) {
        return ((orientation + 45) / 90 * 90) % 360;
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    private class MyOrientationEventListener
            extends OrientationEventListener {

        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN)
                return;
            mOrientation = roundOrientation(orientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mOrientation
                    + CameraActivity.getDisplayRotation(CameraActivity.this);
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                setOrientationIndicator(mOrientationCompensation);
            }
        }
    }

    private void setOrientationIndicator(int degree) {
//        if (mViewFinderView != null)
//            mViewFinderView.setOrientation(degree);
    }

}

// ----------------------------------------------------------------------

