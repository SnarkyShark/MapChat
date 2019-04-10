package edu.temple.mapchat;

import android.location.Location;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

public class Friend implements Comparable {

    private String username;
    private LatLng position;
    private float distance;

    public Friend(String name, double lat, double lng) {
        this.username = name;
        this.position = new LatLng(lat, lng);
    }

    @Override
    @NonNull
    public String toString() {
        return this.username;
    }

    public LatLng getPosition() {
        return position;
    }

    // distance
    public void calculateDist(double lat, double lng) {
        this.distance = Math.abs(distance((float) position.latitude, (float) position.longitude,
                (float) lat, (float) lng));
    }

    private float distance (float lat_a, float lng_a, float lat_b, float lng_b )
    {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b-lat_a);
        double lngDiff = Math.toRadians(lng_b-lng_a);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return new Float(distance * meterConversion).floatValue();
    }

    public float getDistance() {
        return distance;
    }

    @Override
    public int compareTo(Object p) {
        Friend o = (Friend) p;
        int retval = 0;

        double signNum = this.getDistance() - o.getDistance();

        if(signNum < 0)
            retval = -1;
        else if (signNum > 0)
            retval = 1;
        //else, retval = 0
        return retval;
    }
}
