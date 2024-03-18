package com.example.captive_portal_analyzer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ListAvailableWiFi extends AppCompatActivity {

    private final String TAG = "listAndConnectWifi";

    private WifiManager wifiManager;
    private Context context;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> availableWiFiNetworks = new ArrayList<>();
    private String selectedWiFi;

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            new Thread(() -> scanSuccess()).start();
        }
    };

    private final BroadcastReceiver postConnectionBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(
                    WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                return;
            }

            findViewById(R.id.pBar).setVisibility(View.GONE);
            findViewById(R.id.tVAcceptWiFiConnecting).setVisibility(View.GONE);

            Intent intent_1 = new Intent(getApplicationContext(), CaptivePortalAnalysis.class);
            startActivity(intent_1);


        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_available_wi_fi);

        new Thread(() -> {
            context = getApplicationContext();
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            ListView listView = findViewById(R.id.lv_available_wifi_networks);
            adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, availableWiFiNetworks);

            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                selectedWiFi = (String) parent.getItemAtPosition(position);
                runOnUiThread(() -> Toast.makeText(context, "connecting with " + selectedWiFi, Toast.LENGTH_SHORT).show());
                connect_with_wifi(selectedWiFi);
            });



            wifiManager.startScan();
        }).start();
    }

    private void scanSuccess() {
        List<ScanResult> results = wifiManager.getScanResults();

        // clear ListView for results of next scan
        runOnUiThread(() -> {
            adapter.clear();
            adapter.notifyDataSetChanged();
            for (ScanResult scanResult : results) {
                if (!(TextUtils.isEmpty(scanResult.SSID)) && !(availableWiFiNetworks.contains(scanResult.SSID))) {
                    availableWiFiNetworks.add(scanResult.SSID);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void connect_with_wifi(String SSID) {
        findViewById(R.id.lv_available_wifi_networks).setVisibility(View.GONE);
        findViewById(R.id.pBar).setVisibility(View.VISIBLE);
        findViewById(R.id.tVAcceptWiFiConnecting).setVisibility(View.VISIBLE);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        final WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
                .setSsid(SSID)
                .setIsAppInteractionRequired(true)
                .setIsUserInteractionRequired(true)
                .build();

        final List<WifiNetworkSuggestion> suggestionsList =
                new ArrayList<WifiNetworkSuggestion>() {{
                    add(suggestion);

                }};

        wifiManager.addNetworkSuggestions(suggestionsList);
    }

    @Override
    public void onResume(){
        super.onResume();

        final IntentFilter intentFilterWifi = new IntentFilter();
        intentFilterWifi.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifiScanReceiver, intentFilterWifi);

        final IntentFilter intentFilterPostCon =
                new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);
        context.registerReceiver(postConnectionBroadcastReceiver, intentFilterPostCon);

        Log.i(TAG, "registered BroadcastReceivers");
    }

    @Override
    public void onStop(){
        super.onStop();

        context.unregisterReceiver(wifiScanReceiver);
        context.unregisterReceiver(postConnectionBroadcastReceiver);

        Log.i(TAG, "unregistered BroadcastReceivers");
    }

    @Override
    public void onBackPressed() {
        if (! (findViewById(R.id.pBar).getVisibility() == View.VISIBLE) ) {
            super.onBackPressed();
        }
    }

}