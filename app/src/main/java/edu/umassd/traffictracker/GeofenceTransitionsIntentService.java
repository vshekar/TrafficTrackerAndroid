package edu.umassd.traffictracker;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Shekar on 12/18/2015.
 */



public class GeofenceTransitionsIntentService extends Service {
    String TAG = "Geofence Transition Service";
    boolean DEBUG = true;
    boolean serviceStarted = false;
    public static Intent trackingServiceIntent;
    private final IBinder mBinder = new LocalBinder();
    public GeofenceTransitionsIntentService() {
        super();
    }

    private Intent trackingActivityIntent;
    private PendingIntent trackingActivityPendingIntent;
    //Create a notification. This will tell the user whether they are being tracked or not
    private NotificationCompat.Builder mNotifyBuilder;
    private Handler handler = new Handler();
    Timer timer = new Timer();
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            handler.post(new Runnable() {
                             @Override
                             public void run() {

                                 //new uploadData().execute();
                                 uploadFiles();

                             }
                         }
            );
        }
    };


    // Sets an ID for the notification
    private int mNotificationId = 001;

    public class LocalBinder extends Binder {
        GeofenceTransitionsIntentService getService() {
            // Return this instance of LocalService so clients can call public methods
            return GeofenceTransitionsIntentService.this;
        }
    }

    public IBinder onBind(Intent intent){
        return mBinder;
    }


    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = "Geofence Error!";
            if(DEBUG)Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        trackingServiceIntent = new Intent(this,TrackingService.class);

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ) {
            if(DEBUG)Log.e(TAG, "GEOFENCE_TRANSITION_ENTER");
            Toast.makeText(getApplicationContext(), "Entered UMass Dartmouth Campus!", Toast.LENGTH_LONG).show();
            mNotifyBuilder.setContentText("Entered UMass. Status : Tracking");

            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(mNotificationId,mNotifyBuilder.build());
            //Starting service
            this.startService(trackingServiceIntent);
            this.serviceStarted = true;
        }


        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            Toast.makeText(getApplicationContext(), "Exiting UMass Dartmouth Campus!", Toast.LENGTH_LONG).show();
            mNotifyBuilder.setContentText("Exited UMass. Status : Not Tracking");
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(
                    mNotificationId,
                    mNotifyBuilder.build());
            if(DEBUG)Log.e(TAG, "GEOFENCE_TRANSITION_EXIT");
            stopTracking();
        }
    }

    public void stopTracking(){
        if(DEBUG)Log.e(TAG,"StopTracking");
        if(this.serviceStarted) {
            this.stopService(trackingServiceIntent);
        }
        if(DEBUG)Log.e(TAG,"StopSelf");
        this.stopSelf();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(DEBUG)Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        setupForeground();

        //monitorWifi();
        onHandleIntent(intent);

        return START_STICKY;
    }


    private NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager connManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

    public void setupForeground(){
        trackingActivityIntent = new Intent(this, MainActivity.class);
        trackingActivityPendingIntent = PendingIntent.getActivity(this, 0, trackingActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Traffic Tracker")
                .setContentText("Outside UMass: Not tracking")
                .setContentIntent(trackingActivityPendingIntent)
                .setAutoCancel(true);
        startForeground(mNotificationId,mNotifyBuilder.build());
    }

    @Override
    public void onCreate(){
        super.onCreate();
        timer.schedule(task, 0, 600000);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopForeground(true);
        stopTracking();
        timer.cancel();
        stopSelf();
        if(DEBUG)Log.e(TAG,"OnDestroy Geofence");
    }



    public void uploadFiles(){

        String[] paths;
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean wifiOnly = prefs.getBoolean("wifi_upload", true);
        boolean allowUpload = false;
        boolean wifiActive = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        String message = "No internet connection detected. Please check network connectivity";

        if(networkInfo != null && networkInfo.isConnected()){
            if ((wifiOnly && wifiActive) || (!wifiOnly))
                allowUpload = true;
            else
                message = "No wifi connection detected. Please enable wifi or change setting to upload using cell phone network";
        }

        if (!allowUpload){


        }

        if (allowUpload) {
            try {
                File root = getFilesDir();
                File outDir = new File(root.getAbsolutePath() + File.separator + "Traffic_tracker"+ File.separator + "uploads");
                paths = outDir.list();
                Log.e("UPLOAD_TASK : ",root.getAbsolutePath() + File.separator + "Traffic_tracker"+ File.separator + "uploads");
                String[] finalPaths;


                if(paths.length != 0 && paths != null) {
                    uploadData asyncTaskRunner = new uploadData();
                    asyncTaskRunner.execute(paths);
                }
                else{
                    //Toast.makeText(getApplicationContext(), "Traffic tracker : No data to upload", Toast.LENGTH_LONG).show();

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private class uploadData extends AsyncTask<String, String, String> {
        private static final String TAG = "UPLOAD_TASK";
        boolean DEBUG = true;

        @Override
        protected String doInBackground(String... params) {
            //String url_string = "http://134.88.13.215:8000/uploadToServer.php";
            String url_string = "http://134.88.13.215:8000/uploadToServer.py";
            //String url_string = "http://10.0.2.2:8000/cgi-bin/uploadToServer.py";

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            if(DEBUG)Log.e(TAG, "doInBackground ");
            String message = "Blank Message";
            for(String path:params) {
                try {
                    //path = path;
                    Log.i("uploadFile", "Uploading File : " + path);
                    //File root = Environment.getExternalStorageDirectory();
                    File root = getFilesDir();
                    File outDir = new File(root.getAbsolutePath() + File.separator + "Traffic_tracker"+ File.separator + "uploads");
                    //File outputFile = new File(outDir, "temp_gps.csv");
                    File outputFile = new File(outDir, path);
                    FileInputStream fileInputStream = new FileInputStream(outputFile);
                    message = "Upload Failed";

                    URL url = new URL(url_string);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // Allow Inputs
                    conn.setDoOutput(true); // Allow Outputs
                    conn.setUseCaches(false); // Don't use a Cached Copy
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    conn.setRequestProperty("uploaded_file", outputFile.toString());

                    dos = new DataOutputStream(conn.getOutputStream());

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + outputFile.toString() + "\"" + lineEnd);

                    dos.writeBytes(lineEnd);

                    // create a buffer of  maximum size
                    bytesAvailable = fileInputStream.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {

                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    }

                    // send multipart form data necesssary after file data...
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                    int serverResponseCode = conn.getResponseCode();
                    String serverResponseMessage = conn.getResponseMessage();

                    Log.i("uploadFile", "HTTP Response is : "
                            + serverResponseMessage + ": " + serverResponseCode);
                    fileInputStream.close();
                    dos.flush();
                    dos.close();
                    if (serverResponseCode == 200){
                        Log.i("uploadFile", "Upload successful, Deleting File : " + path);
                        outputFile.delete();
                        message = "Upload Successful";
                    }
                    else{
                        message = "Upload Unsuccessful";
                    }



                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return message;
        }

        @Override
        protected void onPostExecute(String message){
            Log.i("uploadFile", "onPostExecute");
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

        }


    }


}

