package com.darryncampbell.wifi_rtt_trilateration;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class LocationRangingService extends Service {

    boolean mStarted = false;
    private static final String TAG = "Wifi-RTT";
    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiScanReceiver;
    private List<ScanResult> WifiRttAPs;
    private WifiRttManager mWifiRttManager;
    private RttRangingResultCallback mRttRangingResultCallback;
    final Handler mRangeRequestDelayHandler = new Handler();
    private int mMillisecondsDelayBeforeNewRangingRequest = 1000;
    private boolean bStop = true;
    private Configuration configuration;
    private List<AccessPoint> buildingMap;
    private ArrayList<List<RangingResult>> historicDistances;


    public LocationRangingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiScanReceiver = new WifiScanReceiver();
        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        mRttRangingResultCallback = new RttRangingResultCallback();
        configuration = new Configuration(Configuration.CONFIGURATION_TYPE.TWO_DIMENSIONAL_2);
        buildingMap = configuration.getConfiguration();
        Collections.sort(buildingMap);
        historicDistances = new ArrayList<List<RangingResult>>();
        for (int i = 0; i < buildingMap.size(); i++)
        {
            historicDistances.add(new ArrayList<RangingResult>());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent.getAction().equals(Constants.ACTION.START_LOCATION_RANGING_SERVICE))
        {
            if (mStarted)
            {

            }
            else
            {
                mStarted = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel notificationChannel = new NotificationChannel(Constants.NOTIFICATION_ID.LOCATION_UPDATE_CHANNEL_ID,
                            Constants.NOTIFICATION_ID.LOCATION_UPDATE_CHANNEL, NotificationManager.IMPORTANCE_HIGH);
                    NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.createNotificationChannel(notificationChannel);
                }
                Intent startRangingIntent = new Intent(LocationRangingService.this, LocationRangingService.class);
                startRangingIntent.setAction(Constants.ACTION.START_LOCATION_RANGING);
                Intent stopRangingIntent = new Intent(LocationRangingService.this, LocationRangingService.class);
                stopRangingIntent.setAction(Constants.ACTION.STOP_LOCATION_RANGING);
                Intent quitRangingIntent = new Intent(LocationRangingService.this, LocationRangingService.class);
                quitRangingIntent.setAction(Constants.ACTION.STOP_LOCATION_RANGING_SERVICE);

                Notification notification = new NotificationCompat.Builder(this, Constants.NOTIFICATION_ID.LOCATION_UPDATE_CHANNEL_ID)
                        .setContentTitle("WiFi RTT Location Ranging")
                        .setContentText("Location Ranging Service")
                        .setSmallIcon(R.drawable.ic_pin)
                        .setChannelId(Constants.NOTIFICATION_ID.LOCATION_UPDATE_CHANNEL_ID)
                        //.setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .addAction(R.drawable.ic_pin, "Start Ranging",
                                PendingIntent.getForegroundService(this, Constants.NOTIFICATION_ID.LOCATION_RANGING_SERVICE, startRangingIntent, 0))
                        .addAction(R.drawable.ic_pin, "Stop Ranging",
                                PendingIntent.getForegroundService(this, Constants.NOTIFICATION_ID.LOCATION_RANGING_SERVICE, stopRangingIntent, 0))
                        .addAction(R.drawable.ic_pin, "Quit",
                                PendingIntent.getForegroundService(this, Constants.NOTIFICATION_ID.LOCATION_RANGING_SERVICE, quitRangingIntent, 0))
                                        .build();
                //.addAction(R.drawable.ic_pin_drop,
                //        "Record Location", pRecordLocationIntent).build();
                startForeground(Constants.NOTIFICATION_ID.LOCATION_RANGING_SERVICE,
                        notification);
            }


        }
        else if (intent.getAction().equals(Constants.ACTION.START_LOCATION_RANGING))
        {
            registerReceiver(
                    mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

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

        }
        else if (intent.getAction().equals(Constants.ACTION.STOP_LOCATION_RANGING))
        {
            if (bStop)
                showMessage("Ranging is already stopped");
            else
                showMessage("Stopping ranging...");
            bStop = true;
        }
        else if (intent.getAction().equals(Constants.ACTION.STOP_LOCATION_RANGING_SERVICE))
        {
            mStarted = false;
            bStop = true;
            stopForeground(true);
            stopSelf();

            Intent messageIntent = new Intent(Constants.SERVICE_COMMS.FINISH);
            sendBroadcast(messageIntent);
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //  Used only for bound services
        return null;
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = mWifiManager.getScanResults();
            if (scanResults != null) {

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
                if (WifiRttAPs.size() < configuration.getConfiguration().size())
                    showMessage("Did not find enough RTT enabled APs.  Found: " + WifiRttAPs.size());
                else {
                    startRangingRequest(WifiRttAPs);
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startRangingRequest(List<ScanResult> scanResults) {
        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoints(scanResults).build();

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
            if (list.size() >= configuration.getConfiguration().size()) {
                Collections.sort(list, new Comparator<RangingResult>() {
                    @Override
                    public int compare(RangingResult o1, RangingResult o2) {
                        return o1.getMacAddress().toString().compareTo(o2.getMacAddress().toString());
                    }
                });

                List<RangingResult> rangingResultsOfInterest = new ArrayList<RangingResult>();
                rangingResultsOfInterest.clear();
                for (int i = 0; i < list.size(); i++) {
                    RangingResult rangingResult = list.get(i);
                    if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {
                        if (!configuration.getMacAddresses().contains(rangingResult.getMacAddress().toString())) {
                            //  The Mac address found is not in our configuration
                            showMessage("Unrecognised MAC address: " + rangingResult.getMacAddress().toString());
                        } else {
                            rangingResultsOfInterest.add(rangingResult);
                        }
                    } else if (rangingResult.getStatus() == RangingResult.STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC) {
                        showMessage("RangingResult failed (AP doesn't support IEEE80211 MC.");
                    } else {
                        showMessage("RangingResult failed. (" + rangingResult.getMacAddress().toString());
                    }
                }

                //  Expect us to have n APs in the rangingResults to our nearby APs
                //  Potential enhancement: could remove any APs from the building map that we couldn't range to (need at least 2)
                if (rangingResultsOfInterest.size() != configuration.getConfiguration().size())
                {
                    showMessage("Could not find all the APs defined in the configuration to range off of");
                    if (!bStop)
                        queueNextRangingRequest();
                    return;
                }

                //  Build up the position array of the APs we are ranging to
                double[][] positions = new double[buildingMap.size()][3];
                for (int i = 0; i < buildingMap.size(); i++)
                {
                    positions[i] = buildingMap.get(i).getPosition();
                }

                for (int i = 0; i < rangingResultsOfInterest.size(); i++)
                {
                    historicDistances.get(i).add(rangingResultsOfInterest.get(i));
                    if (historicDistances.get(i).size() == Constants.NUM_HISTORICAL_POINTS + 1)
                        historicDistances.get(i).remove(0);
                    showMessage("Distance to " + rangingResultsOfInterest.get(i).getMacAddress().toString() +
                            ": " + rangingResultsOfInterest.get(i).getDistanceMm() + "mm");
                    showMessage("Distance to " + rangingResultsOfInterest.get(i).getMacAddress().toString() +
                            " [Ave]: " + average(historicDistances.get(i)));
                }

                double[] distances = new double[rangingResultsOfInterest.size()];
                for (int i = 0; i < rangingResultsOfInterest.size(); i++)
                {
                    distances[i] = rangingResultsOfInterest.get(i).getDistanceMm();
                }
                double[] distancesAverage = new double[rangingResultsOfInterest.size()];
                for (int i = 0; i < rangingResultsOfInterest.size(); i++)
                {
                    distancesAverage[i] = average(historicDistances.get(i));
                }


                //TrilaterationFunction trilaterationFunction = new TrilaterationFunction(positions, distances);
                //LinearLeastSquaresSolver lSolver = new LinearLeastSquaresSolver(trilaterationFunction);
                //NonLinearLeastSquaresSolver nlSolver = new NonLinearLeastSquaresSolver(trilaterationFunction, new LevenbergMarquardtOptimizer());
                //  x is the same result as centroid.
                //RealVector x = lSolver.solve();
                //LeastSquaresOptimizer.Optimum optimum = nlSolver.solve();
                try {
                    //  Instantaneous
                    //NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                    //  Average
                    NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distancesAverage), new LevenbergMarquardtOptimizer());
                    LeastSquaresOptimizer.Optimum optimum = solver.solve();

                    double[] centroid = optimum.getPoint().toArray();
                    //RealVector standardDeviation = optimum.getSigma(0);

                    //String sCentroid = "Trilateration (centroid): ";
                    //for (int i = 0; i < centroid.length; i++)
                    //    sCentroid += "" + (int)centroid[i] + ", ";
                    //showMessage(sCentroid);
                    //Intent mapIntent = new Intent(getApplicationContext(), MapActivity.class);
                    //mapIntent.putExtra("location", centroid);
                    //mapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    //startActivity(mapIntent);

                    Intent centroidIntent = new Intent(Constants.SERVICE_COMMS.LOCATION_COORDS);
                    centroidIntent.putExtra(Constants.SERVICE_COMMS.LOCATION_COORDS, centroid);
                    sendBroadcast(centroidIntent);

                }
                catch (Exception e)
                {
                    showMessage("Error during trilateration: " + e.getMessage());
                }

            }
            else
            {
                showMessage("Could not find enough Ranging Results");
            }
            if (!bStop)
                queueNextRangingRequest();
        }
    }

    private double average(List<RangingResult> rangingResults) {
        double accumulator = 0.0;
        int n = rangingResults.size();
        for (int i = 0; i < rangingResults.size(); i++)
        {
            accumulator += rangingResults.get(i).getDistanceMm();
        }
        return accumulator / (double)n;
    }

    public void showMessage(String message) {
        Log.i(TAG, message);
        //txtDebugOutput.setText(message + '\n' + txtDebugOutput.getText());
        Intent messageIntent = new Intent(Constants.SERVICE_COMMS.MESSAGE);
        messageIntent.putExtra(Constants.SERVICE_COMMS.MESSAGE, message);
        sendBroadcast(messageIntent);
    }

}
