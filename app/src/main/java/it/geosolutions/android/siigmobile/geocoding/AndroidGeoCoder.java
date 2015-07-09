package it.geosolutions.android.siigmobile.geocoding;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Robert Oehler on 08.07.15.
 *
 * AndroidGeoCoder uses the GeoCoder implementation
 *
 * provided in android.location
 *
 * to achieve a list of addresses for a query string
 *
 */
public class AndroidGeoCoder implements IGeoCoder {

    private Geocoder mGeoCoder;

    public AndroidGeoCoder(Context context) {

        this.mGeoCoder = new Geocoder(context);
    }

    @Override
    public List<Address> getFromLocationName(String query, int results, final Locale locale) {

        try {
            return this.mGeoCoder.getFromLocationName(query, results);
        }catch(IOException e){
            Log.e(AndroidGeoCoder.class.getSimpleName(), "exception getFromLocationName",e);
            return null;
        }
    }
}
