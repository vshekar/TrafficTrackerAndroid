package edu.umassd.traffictracker;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
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
    boolean serviceStarted = false;
    public static Intent trackingServiceIntent;
    private final IBinder mBinder = new LocalBinder();
    public GeofenceTransitionsIntentService(){
        super("GeofenceTransitionsIntentService");
    }

    //Create a notification. This will tell the user whether they are being tracked or not
    private NotificationCompat.Builder mNotifyBuilder =
            new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Traffic Tracker")
                    .setContentText("Outside UMass Dartmouth Campus");
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
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        trackingServiceIntent = new Intent(this,TrackingService.class);

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ) {
            Log.e(TAG, "GEOFENCE_TRANSITION_ENTER");
            Toast.makeText(getApplicationContext(), "Entered UMass Dartmouth Campus!", Toast.LENGTH_LONG).show();
            mNotifyBuilder.setContentText("Entering UMass. Status : Tracking");
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(
                    mNotificationId,
                    mNotifyBuilder.build());
            if(checkPlayServices()){
                trackingServiceIntent.putExtra("playService", true);
            }
            else{
                trackingServiceIntent.putExtra("playService",false);
                //Toast.makeText(getApplicationContext(), "Did not find Google Play service", Toast.LENGTH_LONG).show();
            }
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
            Log.e(TAG, "GEOFENCE_TRANSITION_EXIT");
            stopTracking();
        }
    }

    public void stopTracking(){
        Log.e(TAG,"StopTracking");
        if(this.serviceStarted) {
            this.stopService(trackingServiceIntent);
        }
        Log.e(TAG,"StopSelf");
        this.stopSelf();
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
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mNotifyBuilder.build());
    }

    @Override
    public void onDestroy(){
        //this.stopService(trackingServiceIntent);
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancelAll();
        Log.e(TAG,"OnDestroy Geofence");
    }

}

