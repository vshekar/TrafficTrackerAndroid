package edu.umassd.traffictracker;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;


import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
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


public class TrackingActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, ResultCallback {
    static Intent intent;
    private Geofence mGeofence;
    private GeofencingRequest gfEnter,gfExit;
    private PendingIntent mGeofencePendingIntent;
    GeofenceTransitionsIntentService mService;
    GoogleApiClient mGoogleApiClient;
    String TAG = "TrackingActivity";
    boolean DEBUG = false;
    boolean mBound = false;
    boolean geofenceRunning = false;



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
        super.onCreate(savedInstanceState);
        //Setting layout as Tracking Activity
        setContentView(R.layout.activity_tracking);
        intent = new Intent(this, GeofenceTransitionsIntentService.class);

            //Building Geofence
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
            //Starting Geofence transition service (A background service to keep track of whether the person has entered or left the geofence

        if(!isMyServiceRunning(GeofenceTransitionsIntentService.class)) {
            //Starting service
            startService(intent);


        }
        else{
            geofenceRunning = true;
        }
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //Connecting to Google API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();


        //Binding service to the current activity (TrackingActivity can access public methods of GeofenceTransitionsIntentService.class
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

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
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, Add the geofence
        if(DEBUG)Log.e(TAG, "Connected to API");

        if(!geofenceRunning) {
            if(DEBUG)Log.e(TAG, "Service is not running adding pending intent");
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    gfEnter,
                    mGeofencePendingIntent
            ).setResultCallback(this);

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tracking, menu);
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

    public void stopTracking(View view){

        //Remove the geofence
        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                // This is the same pending intent that was used in addGeofences().
                mGeofencePendingIntent
        ).setResultCallback(this); // Result processed in onResult().

        //Checking if service is bound, If it is called the stopTracking() method
        if(mBound){
            if(DEBUG)Log.e(TAG,"Geofence service is bound. Stopping tracking service");
            mService.stopTracking();
            unbindService(mConnection);
            mBound = false;
            Toast.makeText(this, "Manually Stopped Tracking", Toast.LENGTH_SHORT).show();
        }

        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);

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

}//End tracking activity
