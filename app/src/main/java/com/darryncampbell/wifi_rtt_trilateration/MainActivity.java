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
    private static final String TAG = "Wifi-RTT";
    TextView txtDebugOutput;
    Button btnFindAccessPointsAndRange;
    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiScanReceiver;
    private List<ScanResult> WifiRttAPs;
    private WifiRttManager mWifiRttManager;
    private RttRangingResultCallback mRttRangingResultCallback;
    final Handler mRangeRequestDelayHandler = new Handler();
    private int mMillisecondsDelayBeforeNewRangingRequest = 1000;
    private boolean bStop = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtDebugOutput = findViewById(R.id.txtDebugOutput);
        btnFindAccessPointsAndRange = findViewById(R.id.btnFindAccessPoints);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiScanReceiver = new WifiScanReceiver();
        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        mRttRangingResultCallback = new RttRangingResultCallback();
        //  todo handle screen rotation

    }

    @Override
    protected void onResume() {
        super.onResume();
        mPermissions =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
        if (!mPermissions) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            showMessage("Location permissions granted");
        }

        registerReceiver(
                mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public void onClickFindAccessPointsAndRange(View view) {
        if (mPermissions) {
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT))
            {
                showMessage("This device does not support WIFI RTT");
            }
            else
            {
                //  startScan() is marked as deprecated but no alternative API is available (yet)
                bStop = false;
                showMessage("Searching for access points");
                mWifiManager.startScan();
            }
        } else
            showMessage("Location permissions not granted");
    }

    public void onClickStopRanging(View view)
    {
        showMessage("Stopping ranging...");
        bStop = true;
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
                showMessage("Location permissions granted");
            }
        }
    }

    public void showMessage(String message) {
        Log.i(TAG, message);
        txtDebugOutput.setText(message + '\n' + txtDebugOutput.getText());
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = mWifiManager.getScanResults();
            if (scanResults != null) {

                if (mPermissions) {
                    WifiRttAPs = new ArrayList<>();
                    for (ScanResult scanResult : scanResults) {
                        if (scanResult.is80211mcResponder())
                            WifiRttAPs.add(scanResult);
                        if (WifiRttAPs.size() >= RangingRequest.getMaxPeers()) {
                            break;
                        }
                    }

                    showMessage(scanResults.size()
                            + " APs discovered, "
                            + WifiRttAPs.size()
                            + " RTT capable.");
                    for (ScanResult wifiRttAP : WifiRttAPs) {
                        showMessage("AP Supporting RTT: " + wifiRttAP.SSID + " (" + wifiRttAP.BSSID + ")");
                    }
                    //  Start ranging
                    //  todo should range for 4 APs and if don't have this number, display an error
                    if (WifiRttAPs.size() != 1)
                        showMessage("Did not find enough RTT enabled APs.  Found: " + WifiRttAPs.size());
                    else {
                        startRangingRequest(WifiRttAPs);
                    }
                } else {
                    showMessage("Permissions not allowed.");
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startRangingRequest(List<ScanResult> scanResults) {
        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoint(scanResults.get(0)).build();
        //  todo add additional access points

        mWifiRttManager.startRanging(
                rangingRequest, getApplication().getMainExecutor(), mRttRangingResultCallback);
    }

    // Class that handles callbacks for all RangingRequests and issues new RangingRequests.
    private class RttRangingResultCallback extends RangingResultCallback {

        private void queueNextRangingRequest() {
            mRangeRequestDelayHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            startRangingRequest(WifiRttAPs);
                        }
                    },
                    mMillisecondsDelayBeforeNewRangingRequest);
        }

        @Override
        public void onRangingFailure(int code) {
            Log.d(TAG, "onRangingFailure() code: " + code);
            queueNextRangingRequest();
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            Log.d(TAG, "onRangingResults(): " + list);

            // Because we are only requesting RangingResult for one access point (not multiple
            // access points), this will only ever be one. (Use loops when requesting RangingResults
            // for multiple access points.)
            if (list.size() == 1) {
                //  todo check the list is sorted correctly
                Collections.sort(list, new Comparator<RangingResult>() {
                    @Override
                    public int compare(RangingResult o1, RangingResult o2) {
                        return o1.getMacAddress().toString().compareTo(o2.getMacAddress().toString());
                    }
                });
                RangingResult rangingResult = list.get(0);


                //  todo check that the received ranging results match the expected mac addresses
                //  todo calculate the distances to the APs
                if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {
                    showMessage("Distance to " + rangingResult.getMacAddress().toString() + ": " + rangingResult.getDistanceMm() + "mm");

                    //  todo get this from the configurations
                    double[][] positions = new double[][] { {0.0, 10.0, 0.0}, {10.0, 0.0, 0.0} };

                    //  todo extend this to 4 APs
                    double[] distances = new double[2];
                    distances[0] = rangingResult.getDistanceMm();
                    distances[1] = rangingResult.getDistanceMm();

                    //  todo catch exception if there is only one position
                    TrilaterationFunction trilaterationFunction = new TrilaterationFunction(positions, distances);
                    LinearLeastSquaresSolver lSolver = new LinearLeastSquaresSolver(trilaterationFunction);
                    NonLinearLeastSquaresSolver nlSolver = new NonLinearLeastSquaresSolver(trilaterationFunction, new LevenbergMarquardtOptimizer());
                    RealVector x = lSolver.solve();
                    LeastSquaresOptimizer.Optimum optimum = nlSolver.solve();
                    double[] centroid = optimum.getPoint().toArray();
                    //  todo confirm which is the actual output
                    //  todo - x and centroid are the same
                    showMessage("Trilateration (x): " + x);


/*
                        mNumberOfSuccessfulRangeRequests++;

                        mRangeTextView.setText((rangingResult.getDistanceMm() / 1000f) + "");
                        addDistanceToHistory(rangingResult.getDistanceMm());
                        mRangeMeanTextView.setText((getDistanceMean() / 1000f) + "");

                        mRangeSDTextView.setText(
                                (rangingResult.getDistanceStdDevMm() / 1000f) + "");
                        addStandardDeviationOfDistanceToHistory(
                                rangingResult.getDistanceStdDevMm());
                        mRangeSDMeanTextView.setText(
                                (getStandardDeviationOfDistanceMean() / 1000f) + "");

                        mRssiTextView.setText(rangingResult.getRssi() + "");
                        mSuccessesInBurstTextView.setText(
                                rangingResult.getNumSuccessfulMeasurements()
                                        + "/"
                                        + rangingResult.getNumAttemptedMeasurements());

                        float successRatio =
                                ((float) mNumberOfSuccessfulRangeRequests
                                        / (float) mNumberOfRangeRequests)
                                        * 100;
                        mSuccessRatioTextView.setText(successRatio + "%");

                        mNumberOfRequestsTextView.setText(mNumberOfRangeRequests + "");
*/
                    } else if (rangingResult.getStatus()
                            == RangingResult.STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC) {
                        showMessage("RangingResult failed (AP doesn't support IEEE80211 MC.");

                    } else {
                        showMessage("RangingResult failed.");
                    }
            }
            if (!bStop)
                queueNextRangingRequest();
        }
    }
}
