package it.geosolutions.android.siigmobile.wfs;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Geometry;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;

import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.cookie.Cookie;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonDeserializer;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonSerializer;
import it.geosolutions.android.siigmobile.BuildConfig;
import it.geosolutions.android.siigmobile.Config;
import it.geosolutions.android.siigmobile.Util;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Created by Robert Oehler on 11.09.15.
 *
 */
public class WFSRequest {

    public final static String VALUE_SERVICE = "WFS";
    public final static String VALUE_VERSION = "1.0.0";
    public final static String VALUE_REQUEST = "GetFeature";
    public final static String VALUE_OUTPUTFORMAT = "application/json";
    public final static int VALUE_CRS = 4326;


    public static final String ENDPOINT = "http://destination.geo-solutions.it/geoserver_destination_plus/destination";


    /**
     * Sends the WPS Request to GeoServer using RetroFit
     * The response, if successfull, is parsed by RetroFit, provinging a CRSCollectionFeature in the feedback
     * This already runs in background / gives feedback in the main thread, but may last an notable amount of time
     *
     * @param layerName the layer to query
     * @param center the position of the center of the query
     * @param radius the radius of meter of the query
     * @param cookies a list of cookies to identify
     * @param auth the auth token to use
     * @param feedback the feedback to receive the result or a error message
     */
    public static void getWFS(final String layerName,final GeoPoint center, final int radius, final List<Cookie> cookies, final String auth, final WFSRequestFeedback feedback)
    {

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeHierarchyAdapter(Geometry.class, new GeometryJsonSerializer())
                .registerTypeHierarchyAdapter(Geometry.class, new GeometryJsonDeserializer()).create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(ENDPOINT)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Authorization", auth);
                        if(cookies != null && cookies.size() > 0) {
                            for(Cookie cookie : cookies) {
                                request.addHeader(cookie.getName(), cookie.getValue());
                            }
                        }
                    }
                })
                .setConverter(new GsonConverter(gson))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        BoundingBox bb = new BoundingBox(center.latitude, center.longitude, center.latitude, center.longitude);
        final BoundingBox extended = Util.extend(bb, radius);

        final String bb_string = String.format(Locale.US, "%f,%f,%f,%f,EPSG:%d",
                extended.minLongitude,
                extended.minLatitude,
                extended.maxLongitude,
                extended.maxLatitude,
                VALUE_CRS);

        final String layer = String.format(Locale.getDefault(),"destination:%s",layerName);

        SIIGWFSServices services = restAdapter.create(SIIGWFSServices.class);
        services.getFeatureWFS(
                VALUE_SERVICE,
                VALUE_VERSION,
                VALUE_REQUEST,
                layer,
                bb_string,
                Config.WFS_MAX_RESULTS,
                VALUE_OUTPUTFORMAT,
                new Callback<CRSFeatureCollection>() {
                    @Override
                    public void success(CRSFeatureCollection featureResponse, Response response) {
                        if (BuildConfig.DEBUG) {
                            Log.i("WPSCall", "success " + featureResponse.toString() + "\n" + response.toString());
                        }
                        feedback.success(featureResponse);
                    }

                    @Override
                    public void failure(RetrofitError error) {

                        Log.w("WPSCall", "failure " + error.getMessage());

                        feedback.error(error.getMessage());
                    }
                });
    }

    public interface WFSRequestFeedback
    {
        void success(CRSFeatureCollection result);

        void error(String errorMessage);
    }


    /**
     * Created by Robert Oehler on 01.07.15.
     *
     * Interface which defines the services between mobile and geoserver
     *
     */
    public interface  SIIGWFSServices {

        /**
         * Post a wps to the server
         * @param service the service id
         *                @param version the version to use
         *                               @param request the name of the request,
         *
         * @param cb the callback which is used to parse the response / inform about the process
         */
        @GET("/ows")
        void getFeatureWFS(@Query("service") String service,
                           @Query("version") String version,
                           @Query("request") String request,
                           @Query("typeName") String typeName,
                           @Query("bbox")  String boundingBox,
                           @Query("maxFeatures") int maxFeatures,
                           @Query("outputFormat") String outputFormat,
                           Callback<CRSFeatureCollection> cb);


    }

}
