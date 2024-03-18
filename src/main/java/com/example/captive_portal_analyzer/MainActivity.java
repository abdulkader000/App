package com.example.captive_portal_analyzer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HOME";

    private boolean gpsEnabled = false;
    private boolean networkEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void proceed(View view){
        findViewById(R.id.tV_greetingText).setVisibility(View.INVISIBLE);
        findViewById(R.id.b1_proceed).setVisibility(View.INVISIBLE);

        new Thread(this::checkState).start();

        findViewById(R.id.tV_explainProcedure).setVisibility(View.VISIBLE);
        // Feature disabled because of not being usable
        // findViewById(R.id.b2_Scan_WiFi).setVisibility(View.VISIBLE);
        findViewById(R.id.b3_Analyze).setVisibility(View.VISIBLE);

    }

    public void Scan_WiFi(View view){
        if (checkState()) {
            // change to next activity (For more readability of code and usability of app)
            Intent intent_scan = new Intent(this, ListAvailableWiFi.class);
            startActivity(intent_scan);
        }


    }

    public void analyzeHotspot(View view){
        if (checkState()){
            Intent intentAnalyze = new Intent(getApplicationContext(), CaptivePortalAnalysis.class);
            startActivity(intentAnalyze);
        }
    }

    private boolean checkState(){

        final  int ACCESS_FINE_LOCATION = 100;
        final  int ACCESS_COARSE_LOCATION = 101;

        boolean returnValue = true;

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {
            Log.e(TAG, ex.toString() );
        }

        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {
            Log.e(TAG, ex.toString());
        }
        if( (!wifiManager.isWifiEnabled()) || (!gpsEnabled && !networkEnabled) ) {

            runOnUiThread(() -> Toast.makeText(this, "Location and WiFi Services must be on",Toast.LENGTH_LONG).show());
            returnValue = false;
        }

        if (!(handlePermission(Manifest.permission.ACCESS_FINE_LOCATION, ACCESS_FINE_LOCATION) || handlePermission(Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_COARSE_LOCATION))
        ){
            returnValue = false;
        }

        return returnValue;
    }

    private boolean handlePermission(String permission, int requestCode){
        boolean returnValue = true;
        if(ContextCompat.checkSelfPermission(MainActivity.this,permission)==
                PackageManager.PERMISSION_DENIED){
            returnValue = false;
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{ permission },
                    requestCode);
        }
        return returnValue;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.tV_greetingText).getVisibility() == View.INVISIBLE) {

            findViewById(R.id.tV_explainProcedure).setVisibility(View.INVISIBLE);
            // not needed as feature disabled
            // findViewById(R.id.b2_Scan_WiFi).setVisibility(View.INVISIBLE);
            findViewById(R.id.b3_Analyze).setVisibility(View.INVISIBLE);

            findViewById(R.id.tV_greetingText).setVisibility(View.VISIBLE);
            findViewById(R.id.b1_proceed).setVisibility(View.VISIBLE);

        } else {
            super.onBackPressed();
        }
    }

}