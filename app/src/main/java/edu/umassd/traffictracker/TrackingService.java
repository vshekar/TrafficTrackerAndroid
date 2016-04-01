package edu.umassd.traffictracker;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;

import android.os.Bundle;

import android.os.IBinder;
import android.preference.PreferenceManager;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedWriter;

import java.io.File;

import java.io.FileWriter;
import java.io.IOException;

import java.io.Writer;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import android.os.Handler;



/**
 * Created by Shekar on 12/18/2015.
 */
public class TrackingService extends Service implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "GPS_SERVICE";
    boolean DEBUG = true;
    private LocationManager mLocationManager = null;
    private String filename;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean playService = true;
    private Location mLastLocation;
    private Writer writer;
    final Handler handler = new Handler();
    private int TOTAL_POINTS = 1000;
    private int current_points = 0;




    public void createNewFile(){
        //Naming the file (Random integer 0< n < 1000 + current UTC time)
        if(DEBUG)Log.e(TAG, "Creating New file");
        File root = getFilesDir();
        File inDir = new File(root.getAbsolutePath() + File.separator + "Traffic_tracker");
        File outDir = new File(root.getAbsolutePath() + File.separator + "Traffic_tracker"+ File.separator + "uploads");
        if (!outDir.exists()){
            outDir.mkdirs();
        }
        String[] paths;
        paths = inDir.list();
        for(int i=1;i<paths.length;i++){
            if(DEBUG)Log.e(TAG,"Moving file paths: "+ paths[i]);
            File from = new File(inDir, paths[i]);
            File to = new File(outDir, paths[i]);
            from.renameTo(to);
        }

        Random rand = new Random();
        int n = rand.nextInt(1000);
        filename = Integer.toString(n);
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy-HH-mm-ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utcTime = sdf.format(new Date());
        filename = filename +"-"+ utcTime + ".csv";

        //Adding first line to the file
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String line = "\"Age\":";
        line += "\"" + prefs.getString("age_preference", "0") + "\"";
        line += ",\"Race\":";
        String text = "0";
        Set<String> set = new HashSet<String>(Arrays.asList(text.split(" +")));
        line += "\"" + prefs.getStringSet("race_preference", set).toString() + "\"";
        line += ",\"Gender\":";
        line += "\""+ prefs.getString("gender_preference", "0") + "\"";
        line += ",\"Occupation\":";
        line += "\""+prefs.getString("occupation_preference", "0")+ "\"";

        writeToFile(filename,line+"\n");
    }

    public void writeToFile(String filename, String data){
        if(DEBUG)Log.e(TAG, "Writing to file");
        //File root = Environment.getExternalStorageDirectory();
        File root = getFilesDir();
        File outDir = new File(root.getAbsolutePath() + File.separator + "Traffic_tracker");

        if (!outDir.exists()){
            outDir.mkdirs();
        }
        try {
            if (!outDir.isDirectory()) {
                throw new IOException(
                        "Unable to create directory EZ_time_tracker. Maybe the SD card is mounted?");
            }
            File outputFile = new File(outDir, filename);
            writer = new BufferedWriter(new FileWriter(outputFile, true));
            writer.write(data);

            writer.close();
        } catch (IOException e) {
            if(DEBUG)Log.w("eztt", e.getMessage(), e);
            Context context = getApplicationContext();
            Toast.makeText(context, e.getMessage() + " Unable to write to external storage.",
                    Toast.LENGTH_LONG).show();
        }

    }


    @Override
     public void onConnectionFailed(ConnectionResult result) {
        if (DEBUG) Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        //displayLocation();
        if(DEBUG)Log.e(TAG, "Connected to API");
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        if(DEBUG)Log.e(TAG, "OnBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(DEBUG)Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        initializeLocationManager();
        startTracking();
        //timer.schedule(task,0,600000);



        return START_STICKY;
    }

    public void stopTracking(){
        if(DEBUG)Log.e(TAG, "stopTracking");
        this.stopSelf();
    }

    public void startTracking(){

        createNewFile();
        mGoogleApiClient.connect();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(2);

    }

    @Override
    public void onCreate()
    {
        if(DEBUG)Log.e(TAG, "onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    @Override
    public void onLocationChanged(Location location){
        if(DEBUG)Log.e(TAG, "onLocationChanged GoogleAPI: " + Double.toString(location.getSpeed()));
        //If the accuracy of the location reading is less than 4.0 meters
        if (location != null) {
            mLastLocation = new Location(location);
            //Get latitude and longitude
            double lat = mLastLocation.getLatitude();
            double lng = mLastLocation.getLongitude();
            //Get current time stamp
            //final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            //final String utcTime = sdf.format(new Date());
            long utcTime = mLastLocation.getTime();

            String op = Long.toString(utcTime) + "," + Double.toString(lat) + "," + Double.toString(lng)+ "," + Double.toString(mLastLocation.getAccuracy()) + "\n" ;
            //op = encryptString(op);
            writeToFile(filename, op);
            if(current_points < TOTAL_POINTS){
                if(DEBUG)Log.e(TAG, "Current Points = " + Integer.toString(current_points));
                current_points++;
            }
            else{
                if(DEBUG)Log.e(TAG, "Reached max points creating new file and uploading ");
                current_points = 0;
                //new uploadData().execute();

                createNewFile();
            }

        }
    }



    @Override
    public void onDestroy() {
        if(DEBUG)Log.e(TAG, "onDestroy");
        super.onDestroy();
        if(playService){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }


    }

    @Override
    public void onTaskRemoved(Intent rootIntent){
        if(DEBUG)Log.e(TAG, "OnTaskRemoved");

        super.onTaskRemoved(rootIntent);
        //this.stopSelf();
    }

    private void initializeLocationManager() {
        if(DEBUG)Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }




}
