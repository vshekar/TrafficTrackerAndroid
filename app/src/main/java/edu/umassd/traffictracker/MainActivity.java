package edu.umassd.traffictracker;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openSettings(View view) {
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }

    public void startTracking(View view){
        Intent intent = new Intent(this, TrackingActivity.class);
        startActivity(intent);
    }

    public void uploadFiles(View view){

        String[] paths;

        try{
            File root = Environment.getExternalStorageDirectory();
            File outDir = new File(root.getAbsolutePath() + File.separator + "Traffic_tracker");
            paths = outDir.list();


            AsyncTaskRunner asyncTaskRunner = new AsyncTaskRunner();
            asyncTaskRunner.execute(paths);

        }catch(Exception e){
            e.printStackTrace();
        }


    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String> {
        private static final String TAG = "UPLOAD_TASK";

        @Override
        protected String doInBackground(String... params) {
            String url_string = "http://134.88.13.215:8000/uploadToServer.php";
            //String url_string = "http://10.0.2.2:8000/cgi-bin/uploadToServer.py";

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            Log.e(TAG, "doInBackground ");

            for(String path:params) {
                try {
                    //path = path;
                    Log.i("uploadFile", "Uploading File : " + path);
                    File root = Environment.getExternalStorageDirectory();

                    File outDir = new File(root.getAbsolutePath() + File.separator + "Traffic_tracker");
                    //File outputFile = new File(outDir, "temp_gps.csv");
                    File outputFile = new File(outDir, path);
                    FileInputStream fileInputStream = new FileInputStream(outputFile);

                    URL url = new URL(url_string);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // Allow Inputs
                    conn.setDoOutput(true); // Allow Outputs
                    conn.setUseCaches(false); // Don't use a Cached Copy
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    conn.setRequestProperty("uploaded_file", outputFile.toString());

                    dos = new DataOutputStream(conn.getOutputStream());

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + outputFile.toString() + "\"" + lineEnd);

                    dos.writeBytes(lineEnd);

                    // create a buffer of  maximum size
                    bytesAvailable = fileInputStream.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {

                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    }

                    // send multipart form data necesssary after file data...
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                    int serverResponseCode = conn.getResponseCode();
                    String serverResponseMessage = conn.getResponseMessage();

                    Log.i("uploadFile", "HTTP Response is : "
                            + serverResponseMessage + ": " + serverResponseCode);
                    fileInputStream.close();
                    dos.flush();
                    dos.close();
                    if (serverResponseCode == 200){
                        Log.i("uploadFile", "Upload successful, Deleting File : " + path);
                        outputFile.delete();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

}


