package utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import edu.umassd.traffictracker.R;
import edu.umassd.traffictracker.Settings;

/**
 * Created by Shekar on 10/31/2016.
 */
public class MiscUtils {
    public Context context;
    public LocationManager locationManager;

    public MiscUtils(Context c){
        context = c;

    }


    /**
     * Performs a first time user check. If first time user, displays IRB form otherwise start app
     * @return
     */
    public boolean firstTimeCheck(){
        boolean result,irb;
        //Check preferences for first time run
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        result = prefs.getBoolean("first_time", true);
        irb = prefs.getBoolean("irbAccepted",false);
        return result&&!irb;
    }



    /**
     * Checks if GPS is enabled. If not, ask user to activate it.
     * If user does not activate GPS co-ordinates will be coarse.
     */
    public void checkGPS(){
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("GPS not enabled");  // GPS not found
            builder.setMessage("Would you like to enable GPS?"); // Want to enable?
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
            builder.setNegativeButton("No", null);
            builder.create().show();
            return;
        }
    }


}
