package edu.umassd.traffictracker;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Created by Shekar on 4/1/2016.
 */
public class ActivityHandler extends IntentService {
    public ActivityHandler() {
        super("ActivityHandler");

    }

    public void showToast(String message) {
        final String msg = message;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onHandleIntent(Intent intent) {
        Log.e("Activityhandler", "  Handling Intent!");
        DetectedActivity d = ActivityRecognitionResult.extractResult(intent).getMostProbableActivity();
        String activity = "";
        if (d.equals(DetectedActivity.IN_VEHICLE)) {
            activity = "In Vehicle";
        } else if (d.equals(DetectedActivity.ON_FOOT)) {
            activity = "On foot";

        } else if (d.equals(DetectedActivity.STILL)) {
            activity = "Still";
        } else {
            activity = d.toString();
        }
        String text = "Activity detected = " + activity;
        //Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        //showToast(text);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("activity", activity);
        editor.commit();
        Log.e("Activityhandler", "Activity detected = " + prefs.getString("activity", "UNKNOWN"));
    }

}


