package edu.umassd.traffictracker;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;



public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, ResultCallback {
    public Geofence mGeofence;
    public GeofencingRequest gfEnter;
    public PendingIntent mGeofencePendingIntent;
    public GoogleApiClient mGoogleApiClient;
    public boolean mBound = false;
    public GeofenceTransitionsIntentService mService;
    public LocationManager locationManager;
    String TAG = "MainActivity";
    boolean DEBUG = false;
    boolean geofenceRunning = false;
    public Intent intent;

    public ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            GeofenceTransitionsIntentService.LocalBinder binder = (GeofenceTransitionsIntentService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(DEBUG)Log.e(TAG, "OnCreate");
        super.onCreate(savedInstanceState);
        boolean ftc = firstTimeCheck();

        if (isMyServiceRunning(GeofenceTransitionsIntentService.class) && !ftc){
            //If the geofencetransition is running already show status
            if(DEBUG)Log.e(TAG, "geofencetransition IS running!");
            intent = new Intent(this, GeofenceTransitionsIntentService.class);
            setContentView(R.layout.activity_main);
        }
        else if (ftc){
            //If app is started for the first time, show settings
            //TODO: Add IRB approval form
            Intent i = new Intent(this, Settings.class);
            startActivity(i);
        }
        else {
            if(DEBUG)Log.e(TAG, "geofencetransition is not running");
            //geofencetransition is not running, start it here
            setContentView(R.layout.activity_main);
            buildGeofence();
            intent = new Intent(this, GeofenceTransitionsIntentService.class);
            startService(intent);
            connectGoogleApiClient(intent);
            //Bind the geofenceTransition service to access its methods
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            checkGPS();
        }
    }

    @Override
    protected void onDestroy(){
        if(DEBUG)Log.e(TAG, "OnDestroy");
        super.onDestroy();
        if(mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onConnected(Bundle arg0) {
        // Once connected with google api, Add the geofence
        if(DEBUG)Log.e(TAG, "Connected to API");
        if(!geofenceRunning) {
            if(DEBUG)Log.e(TAG, "Adding Geofences");
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    gfEnter,
                    mGeofencePendingIntent
            ).setResultCallback(this);
            geofenceRunning = true;
        }
    }

    @Override
    public void onResult(Result r){
        //Shows the result of connecting to the Google Play API (
        //TODO: Can fail, need to add notification to turn on GPS
        if(DEBUG)Log.e(TAG, "OnResult : " + r.toString());
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if(DEBUG)Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }



    public void checkGPS(){
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("GPS not enabled");  // GPS not found
            builder.setMessage("Would you like to enable GPS?"); // Want to enable?
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
            builder.setNegativeButton("No", null);
            builder.create().show();
            return;
        }
    }

    public void connectGoogleApiClient(Intent intent){
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //Connecting to Google API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    public void buildGeofence(){
        mGeofence = new Geofence.Builder()
                .setRequestId("UmassDartmouth")
                .setCircularRegion(41.628931, -71.006228, 1000)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        //Setting geofence enter trigger
        gfEnter = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(mGeofence)
                .build();
    }

    public void exitApp(View view){
        //Triggered when user clicks "Exit App"

        stopService(intent);
        this.finish();

    }

    public boolean firstTimeCheck(){
        boolean result = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        result = prefs.getBoolean("first_time", true);
        if (result == true){
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("first_time", false);
            editor.commit();
        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openSettings(View view) {
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }




    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }





}


