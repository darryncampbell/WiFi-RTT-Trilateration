package com.darryncampbell.wifi_rtt_trilateration;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class MapActivity extends AppCompatActivity {

    private LocationRangingServiceReceiver locationRangingServiceReceiver = null;
    private Configuration configuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationRangingServiceReceiver == null)
            locationRangingServiceReceiver = new LocationRangingServiceReceiver();
        //  Listen for messages from the Location Ranging Service
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.SERVICE_COMMS.LOCATION_COORDS);
        intentFilter.addAction(Constants.SERVICE_COMMS.FINISH);
        registerReceiver(locationRangingServiceReceiver, intentFilter);
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

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        double[] currentLocation = intent.getDoubleArrayExtra("location");
        movePinTestMap(currentLocation[0], currentLocation[1], 0.0);
    }

    private void movePinMap3Points(double x, double y, double z) {
        //  FOR THE PRESENTATION_1 SETUP
        //  todo this is currently hard coded for 2D setups

        final ImageView ImageView_BitmapView = findViewById(R.id.map);
        final ImageView ImageView_Pin = findViewById(R.id.pin);
        ImageView_Pin.setVisibility(View.VISIBLE);

        //  0,0 point is 51.0204% down the image and 50.195% from the left of the image
        float x_image_offset = (ImageView_BitmapView.getWidth() * 0.50195f);
        float y_image_offset = (ImageView_BitmapView.getHeight() * 0.510204f);

        x_image_offset = ImageView_BitmapView.getX() + x_image_offset;
        y_image_offset = ImageView_BitmapView.getY() + y_image_offset;

        float pin_width = ImageView_Pin.getWidth();
        float pin_height = ImageView_Pin.getHeight();
        float x_pin_offset = (pin_width / 2.0f);
        float y_pin_offset = (pin_height) - (5.0f / 72.0f * pin_height); //  There are a few pixels at the bottom of the pin
        //  Account for the fact that the Pin is pointing to the lower middle of the image view
        float pinOriginX = x_image_offset - x_pin_offset;
        float pinOriginY = y_image_offset - y_pin_offset;
        //ImageView_Pin.setX(pinOriginX);
        //ImageView_Pin.setY(pinOriginY);

        float floorWidth = ImageView_BitmapView.getWidth() / 1024.0f;
        float floorHeight = ImageView_BitmapView.getHeight() / 539.0f;

        float scaledX = (float) (x * (322.0f / 2000.0f)) * floorWidth;
        float scaledY = (float) (y * (322.0f / 2000.0f)) * floorHeight;
        ImageView_Pin.setX(pinOriginX + scaledX);
        ImageView_Pin.setY(pinOriginY - scaledY);
    }

    private void movePinTestMap(double x, double y, double z) {
        //  todo this is currently hard coded for 2D setups

        final ImageView ImageView_BitmapView = findViewById(R.id.map);
        final ImageView ImageView_Pin = findViewById(R.id.pin);
        ImageView_Pin.setVisibility(View.VISIBLE);

        //  0,0 point is 82.4427% down the image and 2.336% from the left of the image
        float x_image_offset = (ImageView_BitmapView.getWidth() * 0.02336f);
        float y_image_offset = (ImageView_BitmapView.getHeight() * 0.824427f);

        x_image_offset = ImageView_BitmapView.getX() + x_image_offset;
        y_image_offset = ImageView_BitmapView.getY() + y_image_offset;

        float pin_width = ImageView_Pin.getWidth();
        float pin_height = ImageView_Pin.getHeight();
        float x_pin_offset = (pin_width / 2.0f);
        float y_pin_offset = (pin_height) - (5.0f / 72.0f * pin_height); //  There are a few pixels at the bottom of the pin
        //  Account for the fact that the Pin is pointing to the lower middle of the image view
        float pinOriginX = x_image_offset - x_pin_offset;
        float pinOriginY = y_image_offset - y_pin_offset;
        //ImageView_Pin.setX(pinOriginX);
        //ImageView_Pin.setY(pinOriginY);

        float floorWidth = ImageView_BitmapView.getWidth() * (1772.0f / 3982.0f);
        float floorHeight = ImageView_BitmapView.getHeight() * (1488.0f / 2096.0f);

        float scaledX = (float) (x / 8370.0f * floorWidth);
        float scaledY = (float) (y / 7000.0f * floorHeight);
        ImageView_Pin.setX(pinOriginX + scaledX);
        ImageView_Pin.setY(pinOriginY - scaledY);
    }

    private class LocationRangingServiceReceiver extends BroadcastReceiver
    {
        //  Receives messages from the Location Ranging Service

        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(Constants.SERVICE_COMMS.LOCATION_COORDS))
            {
                configuration = (Configuration)intent.getParcelableExtra(Constants.SERVICE_COMMS.CONFIG);
                ImageView imageView = findViewById(R.id.map);
                imageView.setImageResource(configuration.map_resource);

                //  double[] of current coordinates
                double[] centroid = intent.getDoubleArrayExtra(Constants.SERVICE_COMMS.LOCATION_COORDS);
                //  todo currently hardcoded for 2D
                if (configuration.map_resource == R.drawable.map_test)
                    movePinTestMap(centroid[0], centroid[1], 0.0);
                else if (configuration.map_resource == R.drawable.map_3_points)
                    movePinMap3Points(centroid[0], centroid[1], 0.0);
            }
            else if (intent.getAction().equals(Constants.SERVICE_COMMS.FINISH))
            {
                finishAffinity();
            }
        }
    }
}


//  NOTES:
//  Coordinate system starts at 0,0 at the origin (bottom left)
//  floorWidth === 8370 units
//  floorHeight === 7000 units
//  First floor Z is 2550 units

//  Move pin to bottom right of ground floor (1865 - 93) = 1772px
//  ImageView_Pin.setX(pinOriginX + floorWidth);
//  ImageView_Pin.setY(pinOriginY);
//
//  //  Move pin to the top left of the ground floor (1730 - 242) = 1488px
//  ImageView_Pin.setX(pinOriginX);
//  ImageView_Pin.setY(pinOriginY - floorHeight);
//
//  //  Move pin to the top right of the ground floor
//  ImageView_Pin.setX(pinOriginX + floorWidth);
//  ImageView_Pin.setY(pinOriginY - floorHeight);
//
//  //  Move pin to the origin on the first floor (2113 - 94) = 2019px
//  ImageView_Pin.setX(pinOriginX + firstFloorXDelta);
//  ImageView_Pin.setY(pinOriginY);
//
//  //  Move the pin to the bottom right of the first floor
//  ImageView_Pin.setX(pinOriginX + firstFloorXDelta + floorWidth);
//  ImageView_Pin.setY(pinOriginY);
//
//  //  Move the pin to the top left of the first floor
//  ImageView_Pin.setX(pinOriginX + firstFloorXDelta);
//  ImageView_Pin.setY(pinOriginY - floorHeight);
//
//  //  Move the pin to the top right of the first floor
//  ImageView_Pin.setX(pinOriginX + floorWidth + firstFloorXDelta);
//  ImageView_Pin.setY(pinOriginY - floorHeight);