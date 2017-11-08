package com.example.hotvision;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.*;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "HV::Activity";

    private VisionView mOpenCvCameraView;
    private List<Size> mResolutionList;
    private MenuItem[] mEffectMenuItems;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;

    private SeekBarListener hBar;
    private SeekBarListener sBar;
    private SeekBarListener vBar;
    private SeekBarListener minBar;
    private SeekBarListener maxBar;
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main_2);
        
        // setup camera
        mOpenCvCameraView = (VisionView) findViewById(R.id.camera_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
        // create seekbar listeners
        hBar = new SeekBarListener(this);
        sBar = new SeekBarListener(this);
        vBar = new SeekBarListener(this);
        minBar = new SeekBarListener(this);
        maxBar = new SeekBarListener(this);
        ((SeekBar)findViewById(R.id.seekBar_H)).setOnSeekBarChangeListener(hBar);
        ((SeekBar)findViewById(R.id.seekBar_S)).setOnSeekBarChangeListener(sBar);
        ((SeekBar)findViewById(R.id.seekBar_V)).setOnSeekBarChangeListener(vBar);  
        ((SeekBar)findViewById(R.id.SeekBarMax)).setOnSeekBarChangeListener(maxBar);
        ((SeekBar)findViewById(R.id.SeekBarMin)).setOnSeekBarChangeListener(minBar); 
    }

    public void SeekBarChanged()
    {
    	Log.d(TAG, "H-" + hBar.val + " S-" +  sBar.val + " V-" + vBar.val);
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    	mOpenCvCameraView.SetCamSettings();
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {  	
    	
    	System.out.println("H" + hBar.val);
    	System.out.println("S" + sBar.val);
    	System.out.println("V" + vBar.val);
    	System.out.println("Max" + maxBar.val);
    	System.out.println("Min" + minBar.val);
    	
    	Mat hsvImage = new Mat();
    	Imgproc.cvtColor(inputFrame.rgba(), hsvImage, Imgproc.COLOR_BGR2HSV);
    	
    	// hsv filter
    	float h = 180 * hBar.val;
    	Scalar lowerBound = new Scalar(h, 255.0f * sBar.val, 255.0f * vBar.val);
    	Core.inRange(hsvImage, lowerBound, new Scalar(h + 10, 255, 255), hsvImage);	
    	
    	// blur little
    	Imgproc.GaussianBlur(hsvImage, hsvImage, new org.opencv.core.Size(9, 9), 2);
    	

    	// get circles
    	Mat circles = new Mat();
        Imgproc.HoughCircles(hsvImage, circles, Imgproc.HOUGH_GRADIENT, 1.0,
                (double)hsvImage.rows()/16, // change this value to detect circles with different distances to each other
                100.0, 30.0, (int)(minBar.val*1000), (int)(maxBar.val*1000)); // change the last two parameters
                // (min_radius & max_radius) to detect larger circles
        
        for (int x = 0; x < circles.cols(); x++) {
            double[] c = circles.get(0, x);
            Point center = new Point(Math.round(c[0]), Math.round(c[1]));
            // circle center
            Imgproc.circle(hsvImage, center, 1, new Scalar(0,100,100), 3, 8, 0 );
            // circle outline
            int radius = (int) Math.round(c[2]);
            Imgproc.circle(hsvImage, center, radius, new Scalar(255,0,255), 3, 8, 0 );
        }
        
    	
        return hsvImage;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	
        List<String> effects = mOpenCvCameraView.getEffectList();

        if (effects == null) {
            Log.e(TAG, "Color effects are not supported by device!");
            return true;
        }

        mColorEffectsMenu = menu.addSubMenu("Color Effect");
        mEffectMenuItems = new MenuItem[effects.size()];

        int idx = 0;
        ListIterator<String> effectItr = effects.listIterator();
        while(effectItr.hasNext()) {
           String element = effectItr.next();
           mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
           idx++;
        }

        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
        idx = 0;
        while(resolutionItr.hasNext()) {
            Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
         }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getGroupId() == 1)
        {
            mOpenCvCameraView.setEffect((String) item.getTitle());
            Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 2)
        {
            int id = item.getItemId();
            Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG,"onTouch event");
        return false;
    }
}
