package edu.umassd.traffictracker;


import android.app.Service;
import android.content.Context;
import android.content.Intent;


import android.location.Location;

import android.location.LocationManager;

import android.os.Environment;
import android.os.IBinder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.google.android.gms.location.Geofence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;
import java.util.UUID;


public class TrackingActivity extends AppCompatActivity {
    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);
        intent = new Intent(this,TrackingActivity.TrackingService.class);
        this.startService(intent);
    }
    //Test comment
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
        this.stopService(intent);
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    public static class TrackingService extends Service{
        private static final String TAG = "GPS_SERVICE";
        private LocationManager mLocationManager = null;
        private static final int LOCATION_INTERVAL = 1000;
        private static final float LOCATION_DISTANCE = 0f;

        private class LocationListener implements android.location.LocationListener{
            Location mLastLocation;
            private Writer writer;
            public LocationListener(String provider){
                Log.e(TAG,"LocationListener " + provider);
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
                writeToFile("temp_gps.csv",op);
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
        public IBinder onBind(Intent arg0)
        {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId)
        {
            Log.e(TAG, "onStartCommand");
            super.onStartCommand(intent, flags, startId);
            return START_STICKY;
        }

        @Override
        public void onCreate()
        {
            Log.e(TAG, "onCreate");
            initializeLocationManager();
            if(withinUniversity()) {
                Context context = getApplicationContext();
                Toast.makeText(context, "Within University starting background process",
                        Toast.LENGTH_LONG).show();
                try {
                    mLocationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                            mLocationListeners[1]);
                } catch (java.lang.SecurityException ex) {
                    Log.i(TAG, "fail to request location update, ignore", ex);
                } catch (IllegalArgumentException ex) {
                    Log.d(TAG, "network provider does not exist, " + ex.getMessage());
                }
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
            else{
                Context context = getApplicationContext();
                Toast.makeText(context, "Outside University, Exiting process",
                        Toast.LENGTH_LONG).show();
                //Intent i = new Intent(this, MainActivity.class);
                //startActivity(i);
                stopSelf();
            }
        }

        public boolean withinUniversity(){

            boolean withinUniversity = true;
            /*
            Location loc = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            double lat = loc.getLatitude();
            double lng = loc.getLongitude();
            double unilat = 41.628482;
            double unilng = -71.006185;

            double R = 6371000;
            double dlat = Math.toRadians(lat-unilat);
            double dlng = Math.toRadians(lng-unilng);
            unilat = Math.toRadians(unilat);
            lat = Math.toRadians(lat);
            double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                    Math.sin(dlng/2) * Math.sin(dlng/2) * Math.cos(unilat) * Math.cos(lat);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            double d = R * c;

            if (d > 1000)
                withinUniversity = false;
            else
                withinUniversity = true;
            */
            return withinUniversity;
        }

        @Override
        public void onDestroy()
        {
            Log.e(TAG, "onDestroy");
            super.onDestroy();
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



}
