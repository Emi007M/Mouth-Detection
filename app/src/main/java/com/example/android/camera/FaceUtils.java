package com.example.android.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class FaceUtils {
	
	private static float currentEyeDistance;
	

	
	public static Bitmap getMouthFromBitmap(Bitmap bitmap, boolean recycle, RectF rect){
		//First we want to face, we can use our own method
		//Bitmap face = getSingleFaceFromBitmap(bitmap, recycle);

        
        //We know the lower area of the face is the mouth, CNN's are robust against tilted or moved objects
        //So this is method of cropping is fine
//        int mX = (int)Math.round((width * 0.36));
//        int mY = (int)Math.round((height * 0.68));
//        width = (int)Math.round((width * 0.26));
//        height = (int)Math.round((height * 0.26));

		int x = (int)(rect.left+rect.width()*0.25);
		int y = (int)(rect.top+rect.height()*0.5);
		int height = (int)(rect.height()*0.5);
		int width = height;

		//Bitmap croppedMouth = Bitmap.createBitmap(bitmap, (int)rect.left, (int)rect.top, (int)rect.width(), (int)rect.height());
		Bitmap croppedMouth = Bitmap.createBitmap(bitmap, x, y, width, height);

		if(recycle){
        	recycleBitmap(bitmap);
        }
        
		return croppedMouth;

	}
	
	private static void recycleBitmap(Bitmap bitmap){
		bitmap.recycle();
	}
}
