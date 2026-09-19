package edu.northeastern.numad26sp_team4_sena.utils;

import com.google.android.gms.maps.model.LatLng;


public class DistanceCalculator {

    private static final int EARTH_RADIUS_MILES = 3959;


    public static double calculateDistanceMiles(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_MILES * c;
    }




    public static String formatDistance(double miles) {
        if (miles < 0.1) {
            return "< 0.1 mi";
        }
        return String.format("%.1f mi", miles);
    }
}
