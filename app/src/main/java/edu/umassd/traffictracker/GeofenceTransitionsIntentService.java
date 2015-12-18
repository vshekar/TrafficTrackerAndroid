package edu.umassd.traffictracker;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * Created by Shekar on 12/18/2015.
 */
public class GeofenceTransitionsIntentService extends IntentService{
    String TAG = "Geofence Transition Service";
    public static Intent trackingServiceIntent;
    private final IBinder mBinder = new LocalBinder();
    public GeofenceTransitionsIntentService(){
        super("GeofenceTransitionsIntentService");
    }

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
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        trackingServiceIntent = new Intent(this,TrackingService.class);

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ) {
            Log.e(TAG, "GEOFENCE_TRANSITION_ENTER");
            Toast.makeText(getApplicationContext(), "Entering UMass Dartmouth Campus!", Toast.LENGTH_LONG).show();
            if(checkPlayServices()){
                trackingServiceIntent.putExtra("playService", true);


            }
            else{
                trackingServiceIntent.putExtra("playService",false);
                //Toast.makeText(getApplicationContext(), "Did not find Google Play service", Toast.LENGTH_LONG).show();
            }
            this.startService(trackingServiceIntent);

        }
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            Toast.makeText(getApplicationContext(), "Exiting UMass Dartmouth Campus!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "GEOFENCE_TRANSITION_EXIT");
            stopTracking();
        }
    }

    public void stopTracking(){
        Log.e(TAG,"StopTracking");
        this.stopService(trackingServiceIntent);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                //GooglePlayServicesUtil.getErrorDialog(resultCode, this, 1000).show();
                Log.e(TAG, "Error in google play services");
            } else {
                Log.e(TAG, "Device is not supported");

            }
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy(){
        //this.stopService(trackingServiceIntent);
        Log.e(TAG,"OnDestroy");
    }

}
