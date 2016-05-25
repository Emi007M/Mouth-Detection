// Copyright 2011 Google Inc. All Rights Reserved.

package com.example.android.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback, FaceDetectionListener{
    /**
     * A simple wrapper around a Camera and a SurfaceView that renders a
     * centered preview of the Camera to the surface. We need to center the
     * SurfaceView because not all devices have cameras that support preview
     * sizes at the same aspect ratio as the device's display.
     */
    private final String TAG = "Preview";

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;

    Matrix matrix = new Matrix();
    private boolean newFace = false;
    private ImageView mouthBox;
    private int camId;
    private RectF currentFace;
    private CameraPreview camPreview;
    private int sampleSize;

    CameraPreview(Context context) {
        super(context);

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();

            Camera.Parameters params = mCamera.getParameters();
            params.set("jpeg-quality", 50);
            params.setPictureFormat(PixelFormat.JPEG);
           // params.setPictureSize(2048, 1232);

            List<Size> sizes = params.getSupportedPictureSizes();
            Size size = sizes.get(Integer.valueOf((sizes.size()-1)/2)); //choose a medium resolution
            params.setPictureSize(size.width, size.height);
            camera.setParameters(params);
            //camera.setDisplayOrientation(90);

            List<Size> sizes2 = params.getSupportedPreviewSizes();
            Size size2 = sizes.get(0);

            params.setPreviewSize(size2.width, size2.height);
            //camera.setPreviewDisplay(mHolder);

            //set color efects to none
            params.setColorEffect(Camera.Parameters.EFFECT_NONE);

            //set antibanding to none
            if (params.getAntibanding() != null) {
                params.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);
            }

            // set white ballance
            if (params.getWhiteBalance() != null) {
                params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
            }

            //set flash
            if (params.getFlashMode() != null) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }

            //set zoom
            if (params.isZoomSupported()) {
                params.setZoom(0);
            }

            //set focus mode
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);

            //params.setPreviewFormat(ImageFormat.RGB_565);
            
            mCamera.setParameters(params);
            mCamera.setFaceDetectionListener(this);
        }
       
    }

    public void switchCamera(Camera camera) {
        setCamera(camera);
        try {
            camera.setPreviewDisplay(mHolder);
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
        //Camera.Parameters parameters = camera.getParameters();
        //parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        //parameters.setRotation(90);
        requestLayout();

        //camera.setParameters(parameters);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                            (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                            width, (height + scaledChildHeight) / 2);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
           //     Camera.Parameters parameters = mCamera.getParameters();
           //     //parameters.setRotation(90);
           //     mCamera.setParameters(parameters);

                mCamera.setPreviewCallback(new Camera.PreviewCallback() {


                    private long timestamp=0;
                    public synchronized void onPreviewFrame(byte[] data, Camera camera) {

                        if(!newFace) return;

                        // Convert to JPG
                        Size previewSize = camera.getParameters().getPreviewSize();
                        YuvImage yuvimage=new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 80, baos);
                        byte[] jdata = baos.toByteArray();


                        Log.e("CameraTest","Time Gap = "+(System.currentTimeMillis()-timestamp));
                        timestamp=System.currentTimeMillis();

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inDither = false;
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        options.inPreferQualityOverSpeed = false;

                        //options.inSampleSize = sampleSize;


                        Bitmap image = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, options);


                        ViewFinderView.prepareMatrix(matrix, 0, image.getWidth(), image.getHeight(), false);
                        matrix.mapRect(currentFace);


                        Bitmap mouthImg = FaceUtils.getMouthFromBitmap(image, false, currentFace);

                        if(image!=null) mouthBox.setImageBitmap(mouthImg);

                        camera.addCallbackBuffer(data);

                        newFace = false;

                        return;
                    }
                });

                mCamera.setFaceDetectionListener(this);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();

        mCamera.setParameters(parameters);


        //mCamera.setPreviewCallback((Camera.PreviewCallback) this);

        mCamera.startPreview();
    }

    /*
     * (non-Javadoc)
     * @see
     * android.hardware.Camera.FaceDetectionListener#onFaceDetection(android
     * .hardware.Camera.Face[], android.hardware.Camera)
     */
    @Override
    public void onFaceDetection(Face[] faces, Camera camera) {
        Log.d("facedetection", "Faces Found: " + faces.length );
        ViewFinderView view = ((ViewFinderView)(((Activity)getContext()).findViewById(R.id.viewfinder_view)));
        view.setFaces(Arrays.asList(faces));

        if(faces.length>0){

            newFace = true;
            currentFace = new RectF();
            currentFace.set(faces[0].rect);
        }

    }
    
    public CameraPreview(Context context, AttributeSet attr) {
        super(context, attr);
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    //////////


public void setFace(RectF r){
    currentFace = r;
    newFace = true;
}

    public void setMouthBox(ImageView v){
        mouthBox = v;
    }



    public void setMouthBox(Camera c, ImageView v, int id, CameraPreview p){
        mCamera = c;
        Camera.Size s = mCamera.getParameters().getPictureSize();
        mouthBox = v;
        camId = id;
        camPreview = p;
        sampleSize = s.width/256; //recovering a subsampled picture with width of 256px
    }

}
