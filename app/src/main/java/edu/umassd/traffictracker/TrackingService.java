package edu.umassd.traffictracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
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

/**
 * Created by Shekar on 12/18/2015.
 */
public class TrackingService extends Service implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "GPS_SERVICE";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 0f;
    private String filename;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean playService;
    private Intent thisintent;
    private Location mLastLocation;
    private Geofence mGeofence;

    private class LocationListener implements android.location.LocationListener{
        Location mLastLocation;
        private Writer writer;
        public LocationListener(String provider){
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location){
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
            double lat = mLastLocation.getLatitude();
            double lng = mLastLocation.getLongitude();
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            final String utcTime = sdf.format(new Date());
            String op = utcTime + "," + Double.toString(lat) + "," + Double.toString(lng) + "\n";
            writeToFile(filename,op);
        }

        public void writeToFile(String filename, String data){
            File root = Environment.getExternalStorageDirectory();

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
                Log.w("eztt", e.getMessage(), e);
                Context context = getApplicationContext();
                Toast.makeText(context, e.getMessage() + " Unable to write to external storage.",
                        Toast.LENGTH_LONG).show();
            }

        }

        @Override
        public void onProviderDisabled(String provider){
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };



    @Override
     public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        //displayLocation();
        Log.e(TAG, "Connected to API");
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        Log.e(TAG,"OnBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        thisintent= intent;
        playService = intent.getBooleanExtra("playService", false);
        initializeLocationManager();

        startTracking();

        return START_STICKY;
    }

    public void stopTracking(){
        Log.e(TAG,"stopTracking");
        this.stopSelf();
    }

    public void startTracking(){
        //Naming the file (Random integer 0< n < 1000 + current UTC time)
        Random rand = new Random();
        int n = rand.nextInt(1000);
        filename = Integer.toString(n);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utcTime = sdf.format(new Date());
        filename = filename +"-"+ utcTime + ".csv";

        if(playService)
        {


            //if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
            //}
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(500);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        }
        else {
            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                        mLocationListeners[0]);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "gps provider does not exist " + ex.getMessage());
            }
        }
        //Adding first line to the file
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String line = "Age:";
        line += prefs.getString("age_preference", "0");
        line += ",Race:";
        String text = "0";
        Set<String> set = new HashSet<String>(Arrays.asList(text.split(" +")));
        line += prefs.getStringSet("race_preference", set).toString();
        line += ",Gender:";
        line += prefs.getString("gender_preference", "0");
        line += ",Occupation:";
        line += prefs.getString("occupation_preference", "0");
        line += "\n";
        mLocationListeners[0].writeToFile(filename,line);
    }

    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

    }

    @Override
    public void onLocationChanged(Location location){
        Log.e(TAG, "onLocationChanged GoogleAPI: " + location);
        if (location != null) {
            mLastLocation = new Location(location);
            double lat = mLastLocation.getLatitude();
            double lng = mLastLocation.getLongitude();
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            final String utcTime = sdf.format(new Date());
            String op = utcTime + "," + Double.toString(lat) + "," + Double.toString(lng) + "\n";
            mLocationListeners[1].writeToFile(filename, op);
        }
    }


    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if(playService){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listeners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }


}
