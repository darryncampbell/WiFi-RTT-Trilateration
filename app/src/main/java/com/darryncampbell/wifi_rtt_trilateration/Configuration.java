package com.darryncampbell.wifi_rtt_trilateration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Configuration {
    //  The configuration consists of a number of Access points in 3 dimensional space (x,y,z) identified by their BSSID
    //  todo Define Access Points according to some coordinate system
    AccessPoint ap1 = new AccessPoint("3c:28:6d:ad:9e:ee", 10.0, 10.0, 0.0);

    public List<AccessPoint> getConfiguration()
    {
        ArrayList<AccessPoint> accessPoints = new ArrayList<>();

        accessPoints.add(ap1);
        Collections.sort(accessPoints);
        return accessPoints;
    }
}
