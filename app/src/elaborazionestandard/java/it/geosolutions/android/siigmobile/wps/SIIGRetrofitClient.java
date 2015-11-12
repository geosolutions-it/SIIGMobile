package it.geosolutions.android.siigmobile.wps;

import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Geometry;

import it.geosolutions.android.map.wfs.geojson.GeometryJsonDeserializer;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonSerializer;
import it.geosolutions.android.siigmobile.BuildConfig;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedString;

/**
 * Created by Robert Oehler on 29.06.15.
 *
 */
public class SIIGRetrofitClient {

    public final static String ENDPOINT = "http://destination.geo-solutions.it/geoserver_destination_plus";

    /**
     * Sends the WPS Request to GeoServer using RetroFit
     * The response, if successful, is parsed by RetroFit, providing a CRSCollectionFeature as feedback
     * This already runs in background / gives feedback in the main thread, but may last an notable amount of time
     *
     * @param wps the WPS request as String
     * @param shibCookie shibCookie to identify
     * @param authToken token to identify
     * @param feedback the feedback to receive the result or a error message
     */
    public static void postWPS(final String wps, final String shibCookie, final String authToken, final WPSRequestFeedback feedback)
    {

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeHierarchyAdapter(Geometry.class, new GeometryJsonSerializer())
                .registerTypeHierarchyAdapter(Geometry.class, new GeometryJsonDeserializer())
                .create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setConverter(new GsonConverter(gson))
                .setEndpoint(ENDPOINT)
                .setRequestInterceptor(new LoginRequestInterceptor(authToken, shibCookie))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        //wrap the xml as "TypedString"
        //src : http://stackoverflow.com/questions/21398598/how-to-post-raw-whole-json-in-the-body-of-a-retrofit-request
        TypedString string = new TypedString(wps){
            @Override public String mimeType() {
                return "application/xml";
            }
        };

        SIIGWPSServices siigService = restAdapter.create(SIIGWPSServices.class);
        siigService.postWPS(string, new Callback<CRSFeatureCollection>() {
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


    public interface WPSRequestFeedback
    {
        void success(CRSFeatureCollection result);

        void error(String errorMessage);
    }

    public static class LoginRequestInterceptor implements RequestInterceptor {

        private String mAuth;
        private String mShibbCookie;


        /**
         * constructor to use with auth token and shibCookie (can be null)
         * @param authToken
         * @param shibbCookie
         */
        public LoginRequestInterceptor(final String authToken, final String shibbCookie){
            this.mAuth = authToken;
            this.mShibbCookie = shibbCookie;
        }

        @Override
        public void intercept(RequestFacade requestFacade) {

            requestFacade.addHeader("Accept", "application/json;");

            if(mAuth != null){
                requestFacade.addHeader("Authorization", mAuth);
            }
        }

        public void setAuthFromCredentials( String login, String pass ) {

            String source = login + ":" + pass;

            mAuth = "Basic " + Base64.encodeToString(source.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);

        }

    }

}
