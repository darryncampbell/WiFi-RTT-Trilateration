package com.darryncampbell.wifi_rtt_trilateration;

import java.util.Comparator;

public class AccessPoint implements Comparable{
    private double x;
    private double y;
    private double height;
    private String bssid;
    private String friendlyName;

    public AccessPoint(String bssid, double x, double y, double height, String friendlyName)
    {
        this.bssid = bssid;
        this.x = x;
        this.y = y;
        this.height = height;
        this.friendlyName = friendlyName;
    }

    public double[] getPosition()
    {
        return new double [] {this.x, this.y, this.height};
    }

    public String getBssid() {
        return bssid;
    }

    @Override
    public int compareTo(Object o) {
        AccessPoint a1 = (AccessPoint)o;
        return this.getBssid().compareTo(a1.getBssid());
    }
}
