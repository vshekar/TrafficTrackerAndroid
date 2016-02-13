package edu.umassd.traffictracker;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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



public class GeofenceTransitionsIntentService extends Service {
    String TAG = "Geofence Transition Service";
    boolean DEBUG = true;
    boolean serviceStarted = false;
    public static Intent trackingServiceIntent;
    private final IBinder mBinder = new LocalBinder();
    public GeofenceTransitionsIntentService(){
        super();
    }
    private Intent trackingActivityIntent;
    private PendingIntent trackingActivityPendingIntent;
    //Create a notification. This will tell the user whether they are being tracked or not
    private NotificationCompat.Builder mNotifyBuilder;


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


    protected void handleIntent(Intent intent) {
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

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                //GooglePlayServicesUtil.getErrorDialog(resultCode, this, 1000).show();
                if(DEBUG)Log.e(TAG, "Error in google play services");
            } else {
                if(DEBUG)Log.e(TAG, "Device is not supported");

            }
            return false;
        }
        return true;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(DEBUG)Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        setupForeground();
        handleIntent(intent);

        return START_STICKY;
    }

    public void setupForeground(){
        trackingActivityIntent = new Intent(this, TrackingActivity.class);
        trackingActivityPendingIntent = PendingIntent.getActivity(this, 0, trackingActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Traffic Tracker")
                .setContentText("Outside UMass Dartmouth Campus")
                .setContentIntent(trackingActivityPendingIntent)
                .setAutoCancel(true);
        startForeground(mNotificationId,mNotifyBuilder.build());
    }
    @Override
    public void onCreate(){
        super.onCreate();
        //NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        //mNotifyMgr.notify(mNotificationId, mNotifyBuilder.build());
    }

    @Override
    public void onDestroy(){
        //this.stopService(trackingServiceIntent);
        //NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //mNotifyMgr.cancelAll();
        super.onDestroy();
        stopForeground(true);
        stopSelf();
        if(DEBUG)Log.e(TAG,"OnDestroy Geofence");
    }

}

