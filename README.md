*Please be aware that this application / sample is provided as-is for demonstration purposes without any guarantee of support*
=========================================================

# WiFi-RTT-Trilateration
Combining Android WiFi-RTT (802.11mc) with multilateration to determine position

## Requirements:
- Access Points supporting 802.11mc standard
- Android device running Pie (9.0) or higher
- Android device with network hardware stack that supports WiFi RTT

## Testing:
This project has been tested with the following:
- Pixel 2 XL running Android Pie and Android 10
- Google WiFi access points

## Prerequisites
You need to define a configuration associating the access point mac addresses with the physical location of those access points according to some cartesian coordinate system which you have previously defined.  Personally, I used millimeters from the bottom left corner of my house (0,0,0) with x+ going right and y+ going up along the ground floor.  This app does **NOT** have the ability to place APs on a map as it is just used for testing purposes.  

(Optional) You then also need to provide a map of the area being ranged and then determine the scaling factor between that map, as displayed in the app and the physical locations of the APs on that map, according to your coordinate system.  If you do not provide a map, you can still see your estimated position as text in the MainActivity.

## Overview
This application will scan for nearby access points that support 802.11mc and determine the distances to any AP which has had its location defined in the app's configuration, according to a user-defined coordinate system.  The estimated location of a user will be determined with a 3rd party trilateration algorithm and the resulting estimate.  

Several of these estimated positions are averaged together with a weighted mean to determine a more accurate position, avoiding some jitter in the measurements.

The estimated position is then broadcast to interested parties: The MainActivity will display the positions in textual form and the MapActivity will update a pin on a map.

![Map](https://raw.githubusercontent.com/darryncampbell/WiFi-RTT-Trilateration/master/screenshots/map.png?raw=true)

## Limitations
A lot of the calculations to scale my coordinate system to display the pin on my map are hand-coded and would benefit from better commenting (MapActivity.java)

## Configuration
See the Configuration.java file for example AP layouts I used in my testing.  You can also adjust the frequency between ranging requests as well as the number of historical points to included in the moving average.

## Further work
- I did not look into the [ResponderLocation API](https://developer.android.com/reference/android/net/wifi/rtt/ResponderLocation) introduced in Android 10

## Links:
- Google docs:
  - Topic guide: https://developer.android.com/guide/topics/connectivity/wifi-rtt
  - Package reference: https://developer.android.com/reference/android/net/wifi/rtt/package-summary.html
  - Google sample app: https://github.com/android/connectivity-samples/tree/master/WifiRttScan
- Supported hardware:
  - fitlet2: https://fit-iot.com/web/products/fitlet2/
  - wild: https://fit-iot.com/web/products/wild/
  - Google Wifi: https://store.google.com/gb/product/google_wifi (does not officially claim support but seems to work)
    - Online reports indicate that other mesh based APs also work, though don't officially support WiFi RTT
- Dependant projects:
  - Trilateration: https://github.com/lemmingapex/trilateration (this seems to be the de facto implementation used by other projects)

