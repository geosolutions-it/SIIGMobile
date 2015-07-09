package it.geosolutions.android.siigmobile.geocoding;

import android.location.Address;

import java.util.List;
import java.util.Locale;

/**
 * Created by Robert Oehler on 08.07.15.
 *
 * Interface for a geocoding request
 *
 */
public interface IGeoCoder {
    /**
     * provide a list of addresses for a query string
     * @param query the location name to query
     * @param results the maximum number of results
     * @param locale the locale to provide the results in
     * @return a list of addresses or null
     */
    List<Address> getFromLocationName(final String query, int results, final Locale locale);
}
