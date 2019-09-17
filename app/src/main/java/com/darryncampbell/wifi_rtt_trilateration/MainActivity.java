package com.darryncampbell.wifi_rtt_trilateration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lemmingapex.trilateration.LinearLeastSquaresSolver;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    boolean mPermissions = false;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private LocationRangingServiceReceiver locationRangingServiceReceiver = null;

    private static final String TAG = "Wifi-RTT";
    TextView txtDebugOutput;
    Button btnFindAccessPointsAndRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtDebugOutput = findViewById(R.id.txtDebugOutput);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (locationRangingServiceReceiver == null)
            locationRangingServiceReceiver = new LocationRangingServiceReceiver();
        //  Listen for messages from the Location Ranging Service
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.SERVICE_COMMS.LOCATION_COORDS);
        intentFilter.addAction(Constants.SERVICE_COMMS.MESSAGE);
        intentFilter.addAction(Constants.SERVICE_COMMS.FINISH);
        registerReceiver(locationRangingServiceReceiver, intentFilter);

        mPermissions =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
        if (!mPermissions) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            showMessage("Location permissions granted", true);
            Intent startIntent = new Intent(MainActivity.this, LocationRangingService.class);
            startIntent.setAction(Constants.ACTION.START_LOCATION_RANGING_SERVICE);
            startForegroundService(startIntent);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (locationRangingServiceReceiver != null)
        {
            unregisterReceiver(locationRangingServiceReceiver);
            locationRangingServiceReceiver = null;
        }
    }

    public void btnClearClick(View view)
    {
        txtDebugOutput.setText("");
    }

    public void btnShowMapClick(View view)
    {
        Intent mapIntent = new Intent(getApplicationContext(), MapActivity.class);
        //mapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mapIntent);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissions denied");
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                showMessage("Location permissions granted", true);
            }
        }
    }

    public void showMessage(String message, boolean shouldLog) {
        if (shouldLog)
            Log.i(TAG, message);
        txtDebugOutput.setText(message + '\n' + txtDebugOutput.getText());
    }


    private class LocationRangingServiceReceiver extends BroadcastReceiver
    {
        //  Receives messages from the Location Ranging Service

        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(Constants.SERVICE_COMMS.MESSAGE))
            {
                //  Debug / info message
                showMessage(intent.getStringExtra(Constants.SERVICE_COMMS.MESSAGE), false);
            }
            else if (intent.getAction().equals(Constants.SERVICE_COMMS.LOCATION_COORDS))
            {
                //  double[] of current coordinates
                double[] centroid = intent.getDoubleArrayExtra(Constants.SERVICE_COMMS.LOCATION_COORDS);
                String sCentroid = "Trilateration (centroid): ";
                for (int i = 0; i < centroid.length; i++)
                    sCentroid += "" + (int)centroid[i] + ", ";
                showMessage(sCentroid, true);
            }
            else if (intent.getAction().equals(Constants.SERVICE_COMMS.FINISH))
            {
                finishAffinity();
            }
        }
    }
}
