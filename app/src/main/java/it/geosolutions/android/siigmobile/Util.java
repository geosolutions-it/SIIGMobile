package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.util.MercatorProjection;

/**
 * Created by Robert Oehler on 09.07.15.
 *
 */
public class Util {

    /**
     * Earth radius at the equator
     */
    public static final double EQUATORIAL_RADIUS = 6378137.0;

    /**
     * checks the current online state of the device
     *
     *  Requires to hold the permission
     * {@link android.Manifest.permission#ACCESS_NETWORK_STATE}.
     *
     * @param context to use
     * @return if the device is online
     */
    public static boolean isOnline(Context context) {

        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        try{
            return cm.getActiveNetworkInfo().isConnected();
        } catch (Exception e){
            Log.e(Util.class.getSimpleName(),"exception checking online state, no network available ?",e);
        }
        return false;
    }


    /**
     * Creates a BoundingBox that is a fixed meter amount larger on all sides (but does not cross date line/poles)
     *
     * src : mapsforge-core/src/main/java/org/mapsforge/core/model/BoundingBox.java (version 0.5.2)
     *
     * @param src the boundingbox to extend
     * @param meters extension (must be >= 0)
     * @return an extended BoundingBox or this (if meters == 0)
     */
    public static BoundingBox extend(final BoundingBox src, int meters) {

        if(src == null){
            throw new IllegalArgumentException("src must not be null");
        }
        if (meters == 0) {
            return src;
        } else if (meters < 0) {
            throw new IllegalArgumentException("BoundingBox extend operation does not accept negative values");
        }


        double verticalExpansion = latitudeDistance(meters);
        double horizontalExpansion = longitudeDistance(meters, Math.max(Math.abs(src.minLatitude), Math.abs(src.maxLatitude)));

        double minLat = Math.max(MercatorProjection.LATITUDE_MIN, src.minLatitude - verticalExpansion);
        double minLon = Math.max(-180, src.minLongitude - horizontalExpansion);
        double maxLat = Math.min(MercatorProjection.LATITUDE_MAX, src.maxLatitude + verticalExpansion);
        double maxLon = Math.min(180, src.maxLongitude + horizontalExpansion);

        return new BoundingBox(minLat, minLon, maxLat, maxLon);
    }

    /**
     * Calculates the amount of degrees of latitude for a given distance in meters.
     *
     * @param meters
     *            distance in meters
     * @return latitude degrees
     */
    public static double latitudeDistance(int meters) {
        return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS);
    }

    /**
     * Calculates the amount of degrees of longitude for a given distance in meters.
     *
     * @param meters
     *            distance in meters
     * @param latitude
     *            the latitude at which the calculation should be performed
     * @return longitude degrees
     */
    public static double longitudeDistance(int meters, double latitude) {
        return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS * Math.cos(Math.toRadians(latitude)));
    }

    /**
     * creates a String containing comma separated values
     * for dangers (classi di pericolo)
     *
     * Note : this counts only from index 1 to index 12 (inclusive) of the scenarios
     *
     * @param context to use to access the array
     * @return the list
     */
    public static String commaSeparatedListForDangers(final Context context){

        String[] array = context.getResources().getStringArray(R.array.scenario_ids);

        String result = "";

        //skip, the first entry, which is used to identify all
        //dangers count up to 12
        for(int i = 1; i <= 12; i++){
            result += array[i];
            if(i + 1 <= 12){
                result += ",";
            }
        }
        return result;

    }

    /**
     * creates a String containing comma separated values
     * for the entity values
     * @param context to use to access the array
     * @return the list
     */
    public static String commaSeparatedListForEntities(final Context context){

        String[] array = context.getResources().getStringArray(R.array.entity_ids);

        String result = "";

        //skip, the first entry, which is used to identify all
        for(int i = 1; i < array.length; i++){
            result += array[i];
            if(i + 1 < array.length){
                result += ",";
            }
        }
        return result;

    }
}
