package utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;


import edu.umassd.traffictracker.ActivityHandler;

/**
 * Created by Shekar on 10/31/2016.
 */
public class GeofenceHandler implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, ResultCallback {
    public Geofence mGeofence;
    public GeofencingRequest gfEnter;
    public PendingIntent mGeofencePendingIntent, activityHandlerPI;
    public GoogleApiClient mGoogleApiClient;
    public Intent intent,activityHandler;
    public Context context;
    public static boolean geofenceRunning = false;


    String TAG = "GeofenceHandler";
    boolean DEBUG = true;

    public GeofenceHandler(Context c){
        context = c;
        //Connect to the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API).build();

    }

    public void updateContext(Context c){
        this.context = c;
    }


    /**
     * Method to build GeoFence at the center of UMass Dartmouth with a 1000 meter radius
     * Set up to trigger when the user enters or exits the geofence
     */
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

    /**
     * Remove geofence when exiting the app. The geofence has to be explicitly removed or it will
     * still trigeer when the user is not running the app (or the background process is not running)
     */
    public void removeGeofence(Intent intent){
        if(DEBUG) Log.e(TAG, "Removing geofence");
        mGeofencePendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                // This is the same pending intent that was used in addGeofences().
                mGeofencePendingIntent
        ).setResultCallback(this); // Result processed in onResult().

        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, activityHandlerPI);
    }

    @Override
    public void onResult(Result r){
        //Shows the result of connecting to the Google Play API (
        //TODO: Can fail, need to add notification to turn on GPS
        if(DEBUG)Log.e(TAG, "OnResult : " + r.toString());
    }

    /**
     * This function is triggered when the app sucessfully connects to the GoogleAPI
     * @param arg0
     */
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
        if(DEBUG)Log.e(TAG, "Starting activity detection");
        /*Activity detection API*/
        activityHandler = new Intent(context, ActivityHandler.class);
        activityHandlerPI = PendingIntent.getService(context, 1, activityHandler, PendingIntent.FLAG_UPDATE_CURRENT);

        //Requests activity updates every x ms currently 5000 ms
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient,5000,activityHandlerPI);

    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if(DEBUG)Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    /**
     * Method that handles connection to googleAPI
     * @param intent
     */
    public void connectGoogleApiClient(Intent intent){
        mGeofencePendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Connecting to Google API

        mGoogleApiClient.connect();
    }

}
