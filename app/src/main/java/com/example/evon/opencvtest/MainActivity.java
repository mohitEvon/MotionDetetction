package com.example.evon.opencvtest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.video.Video.createBackgroundSubtractorMOG2;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private static Scalar CONTOUR_COLOR = null;
    private static double areaThreshold = 0.025;
    private CameraBridgeViewBase mOpenCvCameraView;

    private BackgroundSubtractorMOG2 sub;
    private Mat mGray;
    private Mat mRgb;
    private Mat mFGMask;
    private List<MatOfPoint> contours;
    private double lRate = 0.5;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.layout);


        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
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
        //creates a new BackgroundSubtractorMOG class with the arguments

        //sub = new BackgroundSubtractorMOG(3, 4, 0.8, 0.5);
        sub = createBackgroundSubtractorMOG2();
        //creates matrices to hold the different frames
        mRgb = new Mat();
        mFGMask = new Mat();
        mGray = new Mat();

        //arraylist to hold individual contours
        contours = new ArrayList<MatOfPoint>();
    }

    public void onCameraViewStopped() {

    }


    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        contours.clear();
        CONTOUR_COLOR = new Scalar(255);
        //gray frame because it requires less resource to process
        mGray = inputFrame.gray();

        //this function converts the gray frame into the correct RGB format for the BackgroundSubtractorMOG apply function
        Imgproc.cvtColor(mGray, mRgb, Imgproc.COLOR_GRAY2BGR);

        //apply detects objects moving and produces a foreground mask
        //the lRate updates dynamically dependent upon seekbar changes
        sub.apply(mRgb, mFGMask, lRate);

        //erode and dilate are used  to remove noise from the foreground mask
        Imgproc.erode(mFGMask, mFGMask, new Mat());
        Imgproc.dilate(mFGMask, mFGMask, new Mat());

        //drawing contours around the objects by first called findContours and then calling drawContours
        //RETR_EXTERNAL retrieves only external contours
        //CHAIN_APPROX_NONE detects all pixels for each contour
      //  Imgproc.findContours(mFGMask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        Imgproc.findContours(mFGMask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE,new Point(0,0));

        //draws all the contours in red with thickness of 2
        //  Imgproc.drawContours(mRgb, contours, -1, new Scalar(255, 0, 0), 2);

        for (int ind = 0; ind < contours.size(); ind++) {
            try {


                    //Convert contours(i) from MatOfPoint to MatOfPoint2f
                    MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(ind).toArray());
                    //Processing on mMOP2f1 which is in type MatOfPoint2f

                    //Convert back to MatOfPoint
                    MatOfPoint points = new MatOfPoint(contours.get(ind).toArray());

                    // Get bounding rect of contour
                    Rect rect = Imgproc.boundingRect(points);
                Log.w("rect.area()",""+rect.area());

                if(rect.area()>250) {
                    Imgproc.rectangle(mRgb, rect.br(), rect.tl(), CONTOUR_COLOR, 20);

                }








            } catch (Exception e) {
                e.printStackTrace();
            }
        }





        return mRgb;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}

