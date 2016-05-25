// Copyright 2011 Google Inc. All Rights Reserved.

package com.example.android.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * @author anirudhd@google.com (Anirudh Dewani)
 */
public class ViewFinderView extends View {

    private static final String TAG = "facedetection";
    Paint paint = new Paint();
    List<Camera.Face> faces = new ArrayList<Camera.Face>();
    Matrix matrix = new Matrix();
    RectF rect = new RectF();
    private int mDisplayOrientation;
    private int mOrientation;

    private ImageView mouthBox;
    private Camera mCamera;
    private int camId;
    private RectF currentFace;
    private CameraPreview camPreview;
    private int sampleSize;

    private double t0,t1,t2,t3,t4;
    private boolean crop;

    /**
     * @param context
     */
    public ViewFinderView(Context context) {
        super(context);
        t0=t1=t2=t3=t4=0;
        crop = false;
    }

    private void dumpRect(RectF rect, String msg) {
        Log.v(TAG, msg + "=(" + rect.left + "," + rect.top
                + "," + rect.right + "," + rect.bottom + ")");
    }

    @Override
    public void onDraw(Canvas canvas) {
        //canvas.drawRect(50f, 50f, 200f, 200f, paint);
        canvas.drawARGB(0, 0, 0, 0);
        Face f = new Face();

        prepareMatrix(matrix, 0, getWidth(), getHeight(), true);
        //canvas.save();
        //matrix.postRotate(mOrientation); // postRotate is clockwise
        //canvas.rotate(-mOrientation); // rotate is counter-clockwise (for
                                      // canvas)
        for (Face face : faces) {
            rect.set(face.rect);
            dumpRect(rect, "before");
            matrix.mapRect(rect);
            dumpRect(rect, "after");
            canvas.drawRect(rect, paint);


        }
        //canvas.restore();
        Log.d(TAG, "Drawing Faces - " + faces.size());
        super.onDraw(canvas);
    }

    public void setDisplayOrientation(int orientation) {
        mDisplayOrientation = orientation;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
        invalidate();
    }

    /**
     * @param //asList
     */
    public void setFaces(List<Camera.Face> faces) {
        this.faces = faces;
        t0 = System.currentTimeMillis();
        invalidate();
        t0=System.currentTimeMillis() - t0;
//
//        if(crop && !faces.isEmpty()) {
//            t1 = System.currentTimeMillis();
//            crop();
//            t1 = System.currentTimeMillis() - t1;
//
//            Log.d("TIMES", "complete=" + t1 +
//                    " takePicture()=" + t2 +
//                    " crop=" + t3 +
//                    " display=" + t4+
//                    " drawing rect=" + t0);
//
//        }

    }

    public ViewFinderView(Context context, AttributeSet attr) {
        super(context, attr);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
    }

    public static void prepareMatrix(Matrix matrix, int displayOrientation,
            int viewWidth, int viewHeight, boolean mirror) {

        matrix.setScale(mirror ? -1 : 1, 1);
        // Need mirror for front camera.
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);



    }


    public void crop(){

            currentFace = new RectF();
            currentFace.set(faces.get(0).rect);

            t2 = System.currentTimeMillis();
            mCamera.takePicture(null, null, jpegCallback);
            t2 = System.currentTimeMillis() - t2;

    }
//
//    public void setMouthBox(Camera c, ImageView v, int id, CameraPreview p){
//        mCamera = c;
//        Camera.Size s = mCamera.getParameters().getPictureSize();
//        mouthBox = v;
//        camId = id;
//        camPreview = p;
//        sampleSize = s.width/256; //recovering a subsampled picture with width of 256px
//    }



    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
//            new SaveImageTask().execute(data);
//            resetCam();
//            Log.d(TAG, "onPictureTaken - jpeg");
            mCamera.startPreview();
            mCamera.startFaceDetection();

            t3 = System.currentTimeMillis();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inPreferQualityOverSpeed = false;

            options.inSampleSize = sampleSize;


            Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            prepareMatrix(matrix, 0, image.getWidth(), image.getHeight(), false);
            matrix.mapRect(currentFace);


            Bitmap mouthImg = FaceUtils.getMouthFromBitmap(image, false, currentFace);

            t3=System.currentTimeMillis()-t3;
            t4=System.currentTimeMillis();
            if(mouthImg!= null)
                mouthBox.setImageBitmap(mouthImg);
            else
                mouthBox.setImageBitmap(image);

            t4=System.currentTimeMillis()-t4;



            return;
        }
    };

    public void setCrop(){
        crop = crop ? false : true;
    }
}
