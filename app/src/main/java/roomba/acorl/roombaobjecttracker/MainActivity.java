package roomba.acorl.roombaobjecttracker;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    static final String TAG = "mainactivity";
    static final int REQUEST_ENABLE_BT = 1;
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ArrayAdapter<String> mArrayAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private String selectedDevice = "";
    private BluetoothDevice serverDevice = null;
    private BluetoothAdapter mBlueToothAdapter = null;
    private ToggleButton toggleButton;
    private TextView directionTextView;
    private ConnectThread connectThread;
    private RobotInterface robot;

    private boolean isFirst = true;
    private String directionResult = "";
    private Point pOne = new Point(0, 0);
    private Point pTwo = new Point(0, 0);

    private CameraBridgeViewBase mOpenCvCameraView;

    /*private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            // This is where you do your work in the UI thread.
            // Your worker tells you in the message what to do.


        }
    };*/

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    public ListView btDeviceLV;



    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver= new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Toast toast = Toast.makeText(getApplicationContext(), "BT Found", Toast.LENGTH_LONG);
                toast.show();

                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mArrayAdapter.notifyDataSetChanged();
                Log.d(TAG, device.getName() + "->" + device.getAddress());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        mArrayAdapter = new ArrayAdapter<>(this, R.layout.list_black_text, R.id.list_content);
        mArrayAdapter.add("None");

        // Register the BroadcastReceiver

        /*int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);*/


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        establishBluetooth();

        initUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        establishBluetooth();
        registerReceiver(mReceiver, new IntentFilter(CONNECTIVITY_SERVICE));

        OpenCVLoader.initAsync( OpenCVLoader.OPENCV_VERSION_3_1_0,
                this, mLoaderCallback );
    }

    private void initUI() {
        btDeviceLV = (ListView) findViewById(R.id.btDeviceLV);
        btDeviceLV.setAdapter(mArrayAdapter);

        btDeviceLV.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt, long myLong) {
                selectedDevice = (String) (btDeviceLV.getItemAtPosition(myItemInt));
                selectedDevice = selectedDevice.split("\n")[1];

                serverDevice = mBlueToothAdapter.getRemoteDevice(selectedDevice);
                connectThread = new ConnectThread(serverDevice);
                connectThread.run();
                btDeviceLV.setVisibility(View.GONE);
                mOpenCvCameraView.setVisibility(View.VISIBLE);

            }
        });

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.Roomba_Object_Tracker_Camera_View);

        mOpenCvCameraView.setCvCameraViewListener(this);

        directionTextView = (TextView) findViewById(R.id.direction_text);

        /*toggleButton = (ToggleButton) findViewById(R.id.toggleSend);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(MainActivity.this, "Video Shows now", Toast.LENGTH_SHORT).show();
                }
            }
        });*/
    }

    private void establishBluetooth() {
        mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBlueToothAdapter == null) {
            Log.wtf(TAG, "YA'LL AIN'T GOT NO BLUETOOTH");
        }

        if (!mBlueToothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }

        pairedDevices = mBlueToothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mArrayAdapter.notifyDataSetChanged();
            }
        }
        mBlueToothAdapter.startDiscovery();

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://roomba.acorl.roombaobjecttracker/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://roomba.acorl.roombaobjecttracker/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mRgba = inputFrame.rgba();
        Mat mRgbaT = mRgba.t();
        Core.flip(mRgba.t(), mRgbaT, -1);
        Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());

        Mat image = mRgba;
        Mat imageCopy = new Mat();

        image.copyTo(imageCopy);

        image = new DetectionRun().CleanImage(image);

        image = new DetectionRun().DetectObject(image, imageCopy, isFirst);

        if (!isFirst) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    directionTextView.setText(directionResult);
                }
            });

        }

        else
            isFirst = false;


        return image;
    }

    class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final

            boolean isPaired = pairedDevices.contains(device);

            if(!isPaired) {
                device.createBond();
            }

            /*ParcelUuid[] uuids = device.getUuids();
            UUID uuid = uuids[0].getUuid();*/
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;


        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBlueToothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Toast toast = Toast.makeText(getApplicationContext(), "BT Connected", Toast.LENGTH_LONG);
                toast.show();
                // Create our robot interface based on socket created
                robot = new RobotInterface(mmSocket);

            } catch (IOException connectException) {
                Log.wtf(TAG, "DA FAQ?!?!? " + connectException.getMessage());
                // Unable to connect; close the socket and get out
                Toast toast = Toast.makeText(getApplicationContext(), "BT Conenction Failed", Toast.LENGTH_LONG);
                toast.show();
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }


            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    class DetectionRun {
        private Scalar lBound = new Scalar(50, 0, 100);
        private Scalar uBound = new Scalar(70, 255, 255);
        private Mat hsvImage;
        private Mat blurredImage;

//	private static Mat videoImage;
//	private static BufferedImage buffImage;
//	private static byte[] buffToMat;

        private Mat ReadImage(){
            Mat img = Imgcodecs.imread("Green Ball 2.jpg");
            //This check to make sure that the image was read in. If it does, then it calls the next method.
            //If not, then it quits the program.
            if (img != null){
                System.out.println("The image was read in...");
            }else{
                System.out.println("The image was not read in, possibly empty value..." + "\n" +
                        "Quitting the program...");
                System.exit(0);
            }
            return img;
        }

        public void WriteImage(Mat img){
            //Imgcodecs.imwrite("D:/OpenCVTest1/Updated image.jpg", img);
            Imgcodecs.imwrite("image2.jpg", img);
        }

        public Mat CleanImage(Mat img){
            //This attempts to read in the program for editing.
            blurredImage = new Mat();
            hsvImage = new Mat();

            try{

                //Removes some noise from the picture
                //System.out.println("Cleaning up picture noise...");
                Imgproc.blur(img, blurredImage, new Size(7,7));

                //Use gaussian blur to help clean the image as well
                //System.out.println("Testing out the gaussian blur...");
                Imgproc.GaussianBlur(blurredImage, img, new Size(7,7), 0);

                //Convert the image to HSV (Hue-Saturation-Value)
                //System.out.println("Converting picture to HSV...");
                Imgproc.cvtColor(img, hsvImage, Imgproc.COLOR_BGR2HSV);

                //Passes the changed image to the DetectObject method so that a
                //desired object will be discovered.
                //new Test().WriteImage(hsvImage);
            }catch(Exception e){
                //System.out.println("STUPID THING WON'T WORK! Here's your dumb error: " + e);
                System.exit(0);
            }
            return hsvImage;
        }


        public Mat DetectObject(Mat img, Mat copy, boolean isFirst){
            //Find the centers of the object.
            Mat mask = new Mat();
            Mat hierarchy = new Mat();
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            //This should create the mask for the object in the image
            Core.inRange(img, lBound, uBound, mask);
            for (int i =0; i < 2; i++){
                Imgproc.erode(mask, img, new Mat());
                Imgproc.dilate(img, mask, new Mat());
            }

            //Imgcodecs.imwrite("TestMask.png", mask);

            //This will find the contours of the detected object.
            //Takes a source mask, sends it to a destination contours, hierarchy is a backup variable,
            //Imgproc.RETR_TREE is the mode and Imgproc.CHAIN_APPROX_SIMPLE is the method in which it finds the contours.
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            if(contours.size() > 0){
                MatOfPoint matTest = null;
                double matMax = 0;
                MatOfPoint maxContour = null;
                for(int i = 0; i < contours.size(); i++){
                    matTest = contours.get(i);
                    double test = Imgproc.contourArea(matTest);
                    if (matMax < test){
                        matMax = test;
                        maxContour = matTest;
                    }
                }
                Point center = new Point();

                float radius[] = new float[100];
                MatOfPoint2f newMaxC = new MatOfPoint2f(maxContour.toArray());

                Imgproc.minEnclosingCircle(newMaxC, center, radius);
                //Imgproc.moments(newMaxC);


                int rad = Math.round(radius[0]);
                Scalar red = new Scalar(0, 0, 255);

                if (radius[0] > 10){
                    Imgproc.circle(mask, center, rad, red, 5);
                    Imgproc.circle(copy, center, rad, red, 5);
                }


                if (isFirst) {
                    //Drawing a square around a circle.
                    pOne = new Point((center.x - (rad * 2)), (center.y - (rad * 2)));
                    pTwo = new Point((center.x + (rad * 2)), (center.y + (rad * 2)));
                    Imgproc.rectangle(copy, pOne, pTwo, new Scalar(0, 255, 0), 5);
                }

                else {
                    Imgproc.rectangle(copy, pOne, pTwo, new Scalar(0, 255, 0), 5);

                    System.out.println("CENTER: (" + center.x + ", " + center.y+")");
                    System.out.println("pOne: (" + pOne.x + ", " + pOne.y + ")");
                    System.out.println("pTwo: (" + pTwo.x + ", " + pTwo.y + ")");

                    if (center.x < pOne.x)
                        directionResult = "LEFT";
                    else if (center.x > pTwo.x)
                        directionResult = "RIGHT";
                    else if (center.y < pOne.y)
                        directionResult = "FORWARD";
                    else if (center.y > pTwo.y)
                        directionResult = "BACKWARD";
                    else
                        directionResult = "STEADY";

                    System.out.println(directionResult);
                }



                //Imgcodecs.imwrite("TestMaskColor.jpg", copy);

            }
            return copy;
        }

        /*Method of motivation!!*/
        public void FeelGood(){
            System.out.println("Cassidy is the best ever!!!!");
        }
    }


}




