package edu.umassd.traffictracker;



import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;


import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
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
    boolean mBound = false;


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
        setContentView(R.layout.activity_tracking);
        mGeofence = new Geofence.Builder()
                .setRequestId("UmassDartmouth")
                .setCircularRegion(41.628931, -71.006228,1000)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        gfEnter = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(mGeofence)
                .build();
        intent = new Intent(this, GeofenceTransitionsIntentService.class);
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();

        //Start the GPS data collection service
        //this.startService(intent);
        //NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setContentTitle("Traffic Tracker");
    }



    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        //displayLocation();
        Log.e(TAG, "Connected to API");
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                gfEnter,
                mGeofencePendingIntent
        ).setResultCallback(this);
    }

    @Override
    public void onResult(Result r){
        Log.e(TAG, "OnResult : " + r.toString());

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
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
        //this.stopService(intent);
        //mGeofencePendingIntent.
        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                // This is the same pending intent that was used in addGeofences().
                mGeofencePendingIntent
        ).setResultCallback(this); // Result processed in onResult().

        if(mBound){
            Log.e(TAG,"Geofence service is bound. Stopping tracking service");
            mService.stopTracking();
            Toast.makeText(this, "Manually Stopped Tracking", Toast.LENGTH_SHORT).show();
        }

        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);

    }







}
