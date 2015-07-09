package it.geosolutions.android.siigmobile.geocoding;

import android.location.Address;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Created by Robert Oehler on 08.07.15.
 *
 * Implementation of a GeoCoding request using the OpenStreetMap Nominatim service
 *
 * A synchronous Retrofit request is created and executed
 *
 * Its result is converted to Address objects which are returned
 *
 * for now only displayname, latitude and longitude are used *
 *
 * http://wiki.openstreetmap.org/wiki/Nominatim
 *
 */
public class NominatimGeoCoder implements IGeoCoder {

    private final String ENDPOINT = "http://nominatim.openstreetmap.org/";

    private NominationService nominationService;

    public NominatimGeoCoder(){

       final  RestAdapter restAdapter = new RestAdapter.Builder()
       .setEndpoint(ENDPOINT)
       .build();

       nominationService = restAdapter.create(NominationService.class);
    }

    @Override
    public List<Address> getFromLocationName(final String query, final int results, final Locale locale) {

        final List<NominatimPlace> places = nominationService.search(query, "json", results, locale.getLanguage());

        if(places != null && places.size() > 0) {
            ArrayList<Address> addresses = new ArrayList<>();

            for(NominatimPlace place : places){
                //a place without name is useless, skip
                if(place.getDisplayName() == null || place.getDisplayName().equals("")){
                    continue;
                }
                Address a = new Address(Locale.getDefault());

                a.setLatitude(Double.parseDouble(place.getLat()));
                a.setLongitude(Double.parseDouble(place.getLon()));
                a.setFeatureName(place.getDisplayName());

                addresses.add(a);
            }
            return  addresses;
        }

        return null;
    }

    public interface NominationService
    {
        @GET("/search")
        List<NominatimPlace> search(@Query("q") String query, @Query("format") String format, @Query("limit") int limit, @Query("accept-language") String languageCode);
    }
}
