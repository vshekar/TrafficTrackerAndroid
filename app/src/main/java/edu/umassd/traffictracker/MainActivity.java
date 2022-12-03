package edu.umassd.traffictracker;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.model.LatLng;


/**
 * Starting point for the app. The app first connects to the GooglePlay services (Which provides access to GPS, Activity Monitor etc.)
 */
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback, OnMapReadyCallback {
    public Geofence mGeofence;
    public GeofencingRequest gfEnter;
    public PendingIntent mGeofencePendingIntent, activityHandlerPI;
    public GoogleApiClient mGoogleApiClient;

    public boolean mBound = false;
    public GeofenceTransitionsIntentService mService;
    public LocationManager locationManager;
    String TAG = "MainActivity";
    boolean DEBUG = true;
    static boolean geofenceRunning = false;
    public Intent intent, activityHandler;

    final Context c = this;
    public static Context context;

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
        if (DEBUG) Log.e(TAG, "OnCreate");
        super.onCreate(savedInstanceState);
        //Check if app is started for the first time
        boolean ftc = firstTimeCheck();
        MainActivity.context = getApplicationContext();
        //Connect to the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API).build();

        //Check if background service is already running (For cases when the app is closed and relaunched)
        if (isMyServiceRunning(GeofenceTransitionsIntentService.class) && !ftc) {
            //If the geofencetransition is running already show status
            if (DEBUG) Log.e(TAG, "geofencetransition IS running!");
            intent = new Intent(this, GeofenceTransitionsIntentService.class);

            setContentView(R.layout.activity_main);
            mGoogleApiClient.connect();
        } else if (ftc) {
            //If app is started for the first time, show settings
            showIRB();
        } else {
            if (DEBUG) Log.e(TAG, "geofencetransition is not running");
            initialize();
            //Putting google map on app

        }

        //Code to display google map of the campus instead of boring buttons
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null)
            mapFragment.getMapAsync(this);
    }

    /**
     * If background service is NOT running and the app has NOT been started for the first time,
     * This function is run to initilize the different components of the app
     */
    public void initialize() {
        //geofencetransition is not running, start it here
        setContentView(R.layout.activity_main);
        //Build the geofence
        buildGeofence();
        //start background service
        intent = new Intent(this, GeofenceTransitionsIntentService.class);
        startService(intent);
        connectGoogleApiClient(intent);
        //Bind the geofenceTransition service to access its methods
        if (!mBound)
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        //Check if GPS services are available
        checkGPS();

    }

    /**
     * THis function is triggered when the app is destroyed. Unbinds itself from the background service
     */
    @Override
    protected void onDestroy() {
        if (DEBUG) Log.e(TAG, "OnDestroy");
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onDestroy();

    }

    /**
     * This function is triggered when the app sucessfully connects to the GoogleAPI
     * @param arg0
     */
    @Override
    public void onConnected(Bundle arg0) {
        // Once connected with google api, Add the geofence
        if (DEBUG) Log.e(TAG, "Connected to API");
        if (!geofenceRunning) {
            if (DEBUG) Log.e(TAG, "Adding Geofences");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    gfEnter,
                    mGeofencePendingIntent
            ).setResultCallback(this);
            geofenceRunning = true;
        }
        if (DEBUG) Log.e(TAG, "Starting activity detection");
        /*Activity detection API*/
        activityHandler = new Intent(this, ActivityHandler.class);
        activityHandlerPI = PendingIntent.getService(this, 1, activityHandler, PendingIntent.FLAG_UPDATE_CURRENT);

        //Requests activity updates every x ms currently 5000 ms
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 5000, activityHandlerPI);

    }

    @Override
    public void onResult(Result r) {
        //Shows the result of connecting to the Google Play API (
        //TODO: Can fail, need to add notification to turn on GPS
        if (DEBUG) Log.e(TAG, "OnResult : " + r.toString());
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (DEBUG) Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }


    /**
     * Checks if GPS is enabled. If not, ask user to activate it.
     * If user does not activate GPS co-ordinates will be coarse.
     */
    public void checkGPS() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
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

    /**
     * Method that handles connection to googleAPI
     * @param intent
     */
    public void connectGoogleApiClient(Intent intent) {
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Connecting to Google API

        mGoogleApiClient.connect();
    }

    /**
     * Method to build GeoFence at the center of UMass Dartmouth with a 1000 meter radius
     * Set up to trigger when the user enters or exits the geofence
     */
    public void buildGeofence() {
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
    public void removeGeofence() {
        if (DEBUG) Log.e(TAG, "Removing geofence");
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                // This is the same pending intent that was used in addGeofences().
                mGeofencePendingIntent
        ).setResultCallback(this); // Result processed in onResult().

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, activityHandlerPI);
    }

    /**
     * Performs a first time user check. If first time user, displays IRB form otherwise start app
     * @return
     */
    public boolean firstTimeCheck() {
        boolean result, irb;
        //Check preferences for first time run
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        result = prefs.getBoolean("first_time", true);
        irb = prefs.getBoolean("irbAccepted", false);
        return result && !irb;
    }

    /**
     * Displays the IRB form on screen.
     */
    public void showIRB() {
        String title = "IRB Approval form";
        String message = this.getString(R.string.irb);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean irbAccepted = prefs.getBoolean("irbAccepted", false);

        AlertDialog.Builder builder = new AlertDialog.Builder(c)
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
                                Intent intent = new Intent(c, Settings.class);
                                startActivity(intent);


                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new Dialog.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialog, int i) {
                                MainActivity.this.finish();
                            }
                        });
        AlertDialog alertDialog = builder.create();

        // show it
        alertDialog.show();
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
        } else if (id == R.id.action_sendfeedback) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri data = Uri.parse("mailto:vshekar@umassd.edu?subject=Traffic App Feedback&body=");
            intent.setData(data);
            startActivity(intent);
        } else if (id == R.id.action_exit) {
            String title = "Exit";
            String message = "Are you sure you want to exit?\nYou will stop collecting data if you do";
            AlertDialog.Builder builder = new AlertDialog.Builder(c)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("Exit",
                            new Dialog.OnClickListener() {
                                @Override
                                public void onClick(
                                        DialogInterface dialogInterface, int i) {
                                    stopService(intent);
                                    if (mBound) {
                                        unbindService(mConnection);
                                        mBound = false;
                                    }
                                    removeGeofence();
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
     * places markers on each parking lot on the map. Not essential for the app
     * @param map
     */
    @Override
    public void onMapReady(GoogleMap map) {
        LatLng[] Lot = new LatLng[16];
        String[] lotName = new String[16];

        LatLng umassD = new LatLng(41.628931, -71.006228);
        Lot[0] = new LatLng(41.631527, -71.004978);
        lotName[0] = "Lot 1";
        Lot[1] = new LatLng(41.631263, -71.003897);
        lotName[1] = "Lot 2";
        Lot[2] = new LatLng(41.630556, -71.003077);
        lotName[2] = "Lot 3";
        Lot[3] = new LatLng(41.629794, -71.002713);
        lotName[3] = "Lot 4";
        Lot[4] = new LatLng(41.628834, -71.002498);
        lotName[4] = "Lot 5";
        Lot[5] = new LatLng(41.627973, -71.002730);
        lotName[5] = "Lot 6";
        Lot[6] = new LatLng(41.627287, -71.003496);
        lotName[6] = "Lot 7";
        Lot[7] = new LatLng(41.626492, -71.003292);
        lotName[7] = "Lot 7A";
        Lot[8] = new LatLng(41.626856, -71.004339);
        lotName[8] = "Lot 8";
        Lot[9] = new LatLng(41.626502, -71.005194);
        lotName[9] = "Lot 9";
        Lot[10] = new LatLng(41.626168, -71.006135);
        lotName[10] = "Lot 10";
        Lot[11] = new LatLng(41.628358, -71.009884);
        lotName[11] = "Lot 13";
        Lot[12] = new LatLng(41.629248, -71.010063);
        lotName[12] = "Lot 14";
        Lot[13] = new LatLng(41.630110, -71.010076);
        lotName[13] = "Lot 15";
        Lot[14] = new LatLng(41.630896, -71.009344);
        lotName[14] = "Lot 16";
        Lot[15] = new LatLng(41.631331, -71.008515);
        lotName[15] = "Lot 17";

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(umassD, 15));

        for(int i =0; i<16; i++){
            map.addMarker(new MarkerOptions()
                    .title(lotName[i])
                    .snippet("There are x parking spots here")
                    .position(Lot[i]));
        }

        map.addMarker(new MarkerOptions()
                .title("UMass Dartmouth")
                .snippet("")
                .position(umassD));

        map.getUiSettings().setAllGesturesEnabled(false);
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





}


