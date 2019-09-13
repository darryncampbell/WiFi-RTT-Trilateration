package com.darryncampbell.wifi_rtt_trilateration;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        final ImageView ImageView_BitmapView = findViewById(R.id.map);
        final ImageView ImageView_Pin = findViewById(R.id.pin);


        ImageView_BitmapView.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View view, MotionEvent event) {

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
                ImageView_Pin.setX(pinOriginX);
                ImageView_Pin.setY(pinOriginY);

                float floorWidth = ImageView_BitmapView.getWidth() * (1772.0f / 3982.0f);
                float floorHeight = ImageView_BitmapView.getHeight() * (1488.0f / 2096.0f);
                float firstFloorXDelta = ImageView_BitmapView.getWidth() * (2019.0f / 3982.0f);

                //  Coordinate system starts at 0,0 at the origin (bottom left)
                //  floorWidth === 8370 units
                //  floorHeight === 7000 units
                //  First floor Z is 2550 units

                //  Move pin to bottom right of ground floor (1865 - 93) = 1772px
                ImageView_Pin.setX(pinOriginX + floorWidth);
                ImageView_Pin.setY(pinOriginY);

                //  Move pin to the top left of the ground floor (1730 - 242) = 1488px
                ImageView_Pin.setX(pinOriginX);
                ImageView_Pin.setY(pinOriginY - floorHeight);

                //  Move pin to the top right of the ground floor
                ImageView_Pin.setX(pinOriginX + floorWidth);
                ImageView_Pin.setY(pinOriginY - floorHeight);

                //  Move pin to the origin on the first floor (2113 - 94) = 2019px
                ImageView_Pin.setX(pinOriginX + firstFloorXDelta);
                ImageView_Pin.setY(pinOriginY);

                //  Move the pin to the bottom right of the first floor
                ImageView_Pin.setX(pinOriginX + firstFloorXDelta + floorWidth);
                ImageView_Pin.setY(pinOriginY);

                //  Move the pin to the top left of the first floor
                ImageView_Pin.setX(pinOriginX + firstFloorXDelta);
                ImageView_Pin.setY(pinOriginY - floorHeight);

                //  Move the pin to the top right of the first floor
                ImageView_Pin.setX(pinOriginX + floorWidth + firstFloorXDelta);
                ImageView_Pin.setY(pinOriginY - floorHeight);

                return true;
            }
        });



/*
        Resources res=getResources();
        Bitmap mBitmap = BitmapFactory.decodeResource(res, R.drawable.map_test);
        BitmapDrawable bDrawable = new BitmapDrawable(res, mBitmap);

        final int bitmapWidth = bDrawable.getIntrinsicWidth();
        final int bitmapHeight = bDrawable.getIntrinsicHeight();
        int screenWidth = this.getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = this.getWindowManager().getDefaultDisplay().getHeight();


        // set maximum scroll amount (based on center of image)
        final int maxX = (int)((bitmapWidth / 2) - (screenWidth / 2));
        final int maxY = (int)((bitmapHeight / 2) - (screenHeight / 2));

        // set scroll limits
        final int maxLeft = (maxX * -1);
        final int maxRight = maxX;
        final int maxTop = (maxY * -1);
        final int maxBottom = maxY;

        // set touchlistener
        ImageView_BitmapView.setOnTouchListener(new View.OnTouchListener()
        {
            float downX, downY;
            int totalX, totalY;
            int scrollByX, scrollByY;
            public boolean onTouch(View view, MotionEvent event)
            {
                float currentX, currentY;
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        currentX = event.getX();
                        currentY = event.getY();
                        scrollByX = (int)(downX - currentX);
                        scrollByY = (int)(downY - currentY);

                        // scrolling to left side of image (pic moving to the right)
                        if (currentX > downX)
                        {
                            if (totalX == maxLeft)
                            {
                                scrollByX = 0;
                            }
                            if (totalX > maxLeft)
                            {
                                totalX = totalX + scrollByX;
                            }
                            if (totalX < maxLeft)
                            {
                                scrollByX = maxLeft - (totalX - scrollByX);
                                totalX = maxLeft;
                            }
                        }

                        // scrolling to right side of image (pic moving to the left)
                        if (currentX < downX)
                        {
                            if (totalX == maxRight)
                            {
                                scrollByX = 0;
                            }
                            if (totalX < maxRight)
                            {
                                totalX = totalX + scrollByX;
                            }
                            if (totalX > maxRight)
                            {
                                scrollByX = maxRight - (totalX - scrollByX);
                                totalX = maxRight;
                            }
                        }

                        // scrolling to top of image (pic moving to the bottom)
                        if (currentY > downY)
                        {
                            if (totalY == maxTop)
                            {
                                scrollByY = 0;
                            }
                            if (totalY > maxTop)
                            {
                                totalY = totalY + scrollByY;
                            }
                            if (totalY < maxTop)
                            {
                                scrollByY = maxTop - (totalY - scrollByY);
                                totalY = maxTop;
                            }
                        }

                        // scrolling to bottom of image (pic moving to the top)
                        if (currentY < downY)
                        {
                            if (totalY == maxBottom)
                            {
                                scrollByY = 0;
                            }
                            if (totalY < maxBottom)
                            {
                                totalY = totalY + scrollByY;
                            }
                            if (totalY > maxBottom)
                            {
                                scrollByY = maxBottom - (totalY - scrollByY);
                                totalY = maxBottom;
                            }
                        }

                        ImageView_BitmapView.scrollBy(scrollByX, 0);  //  was scrollByY
                        downX = currentX;
                        downY = currentY;
                        ImageView_Pin.setX(ImageView_BitmapView.getX());
                        ImageView_Pin.setY(ImageView_BitmapView.getY() + ImageView_BitmapView.getHeight() - (Math.abs(maxY)));
                        break;

                }

                return true;
            }
        });
        */

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        double[] currentLocation = intent.getDoubleArrayExtra("location");
        movePin(currentLocation[0], currentLocation[1]);
    }

    private void movePin(double x, double y) {
        //  todo this is currently hard coded for 2D setups

        final ImageView ImageView_BitmapView = findViewById(R.id.map);
        final ImageView ImageView_Pin = findViewById(R.id.pin);

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
}
