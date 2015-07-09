package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * Created by Robert Oehler on 09.07.15.
 *
 */
public class Util {

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
}
