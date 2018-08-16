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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;

import java.util.ArrayList;
import java.util.Iterator;
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
       mOpenCvCameraView.setMaxFrameSize(500,400);
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
        mGray = inputFrame.gray();
        CONTOUR_COLOR = new Scalar(255);
        //gray frame because it requires less resource to process
        mGray = inputFrame.gray();

        //this function converts the gray frame into the correct RGB format for the BackgroundSubtractorMOG apply function
        Imgproc.cvtColor(mGray, mRgb, Imgproc.COLOR_GRAY2BGR);
        sub.apply(mRgb, mFGMask, lRate);
        Imgproc.GaussianBlur(mGray, mRgb, new Size(55, 55), 55);
        Imgproc.cvtColor(mGray, mRgb, Imgproc.COLOR_GRAY2BGR);
        //erode and dilate are used  to remove noise from the foreground mask
       // Imgproc.dilate(mFGMask, mFGMask, new Mat(), new Point(-1, -1), 1);
        //Imgproc.erode(mFGMask, mFGMask, new Mat(), new Point(-1, -1), 3);

        //drawing contours around the objects by first called findContours and then calling drawContours
        //RETR_EXTERNAL retrieves only external contours
        //CHAIN_APPROX_NONE detects all pixels for each contour
         Imgproc.findContours(mFGMask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
       try {


               for (int ind = 0; ind < contours.size(); ind++) {
                   // MatOfPoint2f approxCurve = new MatOfPoint2f();
                   MatOfPoint contour2f = new MatOfPoint(contours.get(ind).toArray());
                   // double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
                   // Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
                   // Get bounding rect of contour
                   // MatOfPoint points = new MatOfPoint(approxCurve.toArray());
                   Rect rect = Imgproc.boundingRect(contour2f);
                   //   Log.w("rect.area()", "" + rect.area());
                   if (Imgproc.contourArea(contours.get(ind)) > 2000) {
                       Imgproc.rectangle(mRgb, rect.tl(), rect.br(), CONTOUR_COLOR, 10);
                   }
                   // Imgproc.rectangle(mRgb, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0, 255), 3);
               }

            } catch (Exception e) {
                e.printStackTrace();
            }


      /*  try
        {
            ArrayList<Rect> rects = getContourArea(mFGMask);

            for (int ind = 0; ind < rects.size(); ind++) {
                Imgproc.rectangle(mRgb, rects.get(ind).tl(), rects.get(ind).br(), CONTOUR_COLOR, 20);
            }
        } catch (
                Exception e)

        {
            e.printStackTrace();
        }*/
        try {
          //  gridDetection(mFGMask,mRgb);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mRgb;
    }

    public static void gridDetection(Mat mFGMask, Mat mRgb) {
        try {
            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(mFGMask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            double maxArea = 0;
            MatOfPoint max_contour = new MatOfPoint();

            Iterator<MatOfPoint> iterator = contours.iterator();
            while (iterator.hasNext()) {
                MatOfPoint contour = iterator.next();
                double area = Imgproc.contourArea(contour);
                if (area > maxArea) {
                    maxArea = area;
                    max_contour = contour;
                }
            }

            double epsilon = 0.1 * Imgproc.arcLength(new MatOfPoint2f(max_contour.toArray()), true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(max_contour.toArray()), approx, epsilon, true);
            Rect rect = Imgproc.boundingRect(max_contour);
            Imgproc.rectangle(mRgb, rect.tl(), rect.br(), CONTOUR_COLOR, 20);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static ArrayList<Rect> getContourArea(Mat mat) {
        Mat hierarchy = new Mat();
        Mat image = mat.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(image, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        Rect rect = null;
        double maxArea = 300;
        ArrayList<Rect> arr = new ArrayList<Rect>();
        for (int i = 0; i < contours.size(); i++) {
            Mat contour = contours.get(i);
            double contourArea = Imgproc.contourArea(contour);
            if (contourArea > maxArea) {
                rect = Imgproc.boundingRect(contours.get(i));
                arr.add(rect);
            }
        }
        return arr;
    }


    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
        return new Scalar(pointMatRgba.get(0, 0));

    }
}

