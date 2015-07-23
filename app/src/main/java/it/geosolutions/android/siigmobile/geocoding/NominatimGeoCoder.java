package it.geosolutions.android.siigmobile.geocoding;

import android.location.Address;

import org.mapsforge.core.model.BoundingBox;

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
    public List<Address> getFromLocationName(final String query,final BoundingBox bb, final int results, final Locale locale) {

        final String viewbox = String.format(Locale.US,"%f,%f,%f,%f",bb.minLongitude,bb.maxLatitude,bb.maxLongitude,bb.minLatitude);

        final List<NominatimPlace> places = nominationService.search(
                query,
                "json",
                results,
                viewbox,
                1,
                locale.getLanguage());

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
        List<NominatimPlace> search(@Query("q") String query,//<query> Query string to search for.
                                    @Query("format") String format,//[html|xml|json|jsonv2]
                                    @Query("limit") int limit, //Limit the number of returned results.
                                    @Query("viewbox") String viewBox, //viewbox=<left>,<top>,<right>,<bottom>
                                    @Query("bounded") int bounded, //bounded=[0|1] Restrict the results to only items contained with the bounding box.
                                    @Query("accept-language") String languageCode); //=<browser language string>
    }
}
