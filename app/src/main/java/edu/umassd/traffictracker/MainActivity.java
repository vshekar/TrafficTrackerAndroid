package edu.umassd.traffictracker;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.MapFragment;

import service.GeofenceTransitionsIntentService;
import utils.GeofenceHandler;
import utils.MapHandler;
import utils.MiscUtils;


/**
 * Starting point for the app. The app first connects to the GooglePlay services (Which provides access to GPS, Activity Monitor etc.)
 */
public class MainActivity extends AppCompatActivity  {

    public GoogleApiClient mGoogleApiClient;

    public boolean mBound = false;
    public GeofenceTransitionsIntentService mService;

    String TAG = "MainActivity";
    boolean DEBUG = true;

    public Intent intent;

    public GeofenceHandler geofenceHandler;

    public static Context c;
    public final Context context = this;

    public MiscUtils miscUtils;
    public static MapHandler mapHandler;

    /**
     * mConnection is the app's link to the background process. By using mConnection we can access the service's methods
     */
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


    /**
     * This is the first function that triggers when the app starts
     * The function first checks whether the app has been started for the first time
     * and then attempts to connect to the GoogleApiClient
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(DEBUG)Log.e(TAG, "OnCreate");
        super.onCreate(savedInstanceState);
        //Check if app is started for the first time

        MainActivity.c = getApplicationContext();
        geofenceHandler = new GeofenceHandler(this);
        miscUtils = new MiscUtils(this);

        boolean ftc = miscUtils.firstTimeCheck();

        //Check if background service is already running (For cases when the app is closed and relaunched)
        if (isMyServiceRunning(GeofenceTransitionsIntentService.class) && !ftc){
            //If the geofencetransition is running already show status
            if(DEBUG)Log.e(TAG, "geofencetransition IS running!");
            intent = new Intent(this, GeofenceTransitionsIntentService.class);

            setContentView(R.layout.activity_main);
            geofenceHandler.mGoogleApiClient.connect();
            geofenceHandler.updateContext(this);
        }
        else if (ftc){
            //If app is started for the first time, show settings
            //miscUtils.showIRB();
            showIRB();

        }
        else {
            if(DEBUG)Log.e(TAG, "geofencetransition is not running");
            initialize();
            //Putting google map on app

        }

        mapHandler = new MapHandler((MapFragment) this.getFragmentManager().findFragmentById(R.id.map));
    }

    /**
     * If background service is NOT running and the app has NOT been started for the first time,
     * This function is run to initilize the different components of the app
     */
    public void initialize(){
        //geofencetransition is not running, start it here
        setContentView(R.layout.activity_main);
        //Build the geofence
        geofenceHandler.buildGeofence();
        //start background service
        intent = new Intent(this, GeofenceTransitionsIntentService.class);
        startService(intent);
        geofenceHandler.connectGoogleApiClient(intent);
        //Bind the geofenceTransition service to access its methods
        if (!mBound)
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        //Check if GPS services are available
        miscUtils.checkGPS();



    }

    /**
     * THis function is triggered when the app is destroyed. Unbinds itself from the background service
     */
    @Override
    protected void onDestroy(){
        if(DEBUG)Log.e(TAG, "OnDestroy");
        if(mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onDestroy();

    }


    /**
     * Inflates options menu
     * @param menu
     * @return
     */
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
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
            return true;
        }
        else if(id==R.id.action_sendfeedback){
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri data = Uri.parse("mailto:vshekar@umassd.edu?subject=Traffic App Feedback&body=");
            intent.setData(data);
            startActivity(intent);
        }
        else if(id==R.id.action_exit){
            String title = "Exit";
            String message = "Are you sure you want to exit?\nYou will stop collecting data if you do";
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("Exit",
                            new Dialog.OnClickListener(){
                                @Override
                                public void onClick(
                                        DialogInterface dialogInterface, int i){
                                    geofenceHandler.removeGeofence(intent);
                                    stopService(intent);
                                    if(mBound) {
                                        unbindService(mConnection);
                                        mBound = false;
                                    }
                                    finishAffinity();
                                    return;
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new Dialog.OnClickListener() {
                                @Override
                                public void onClick(
                                        DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                }
                            });
            AlertDialog alertDialog = builder.create();

            // show it
            alertDialog.show();
        }

        return super.onOptionsItemSelected(item);
    }




    /**
     * Checks if the background service is running
     * @param serviceClass
     * @return
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Displays the IRB form on screen.
     */
    public void showIRB() {
        String title = "IRB Approval form";
        String message = context.getString(R.string.irb);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean irbAccepted = prefs.getBoolean("irbAccepted", false);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Accept",
                        new Dialog.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialogInterface, int i) {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("irbAccepted", true);
                                editor.putBoolean("first_time", false);
                                editor.commit();
                                dialogInterface.dismiss();
                                initialize();
                                Intent intent = new Intent(context, Settings.class);
                                startActivity(intent);



                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new Dialog.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialog, int i) {

                                //context.finish();
                                    finish();
                            }
                        });
        AlertDialog alertDialog = builder.create();

        // show it
        alertDialog.show();

    }





}


