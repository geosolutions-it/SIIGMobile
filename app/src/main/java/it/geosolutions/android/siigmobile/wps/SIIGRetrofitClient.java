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
     * @param authToken token to identidy
     * @param feedback the feedback to receive the result or a error message
     */
    public static void postWPS(final String wps, final String authToken, final WPSRequestFeedback feedback)
    {

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeHierarchyAdapter(Geometry.class, new GeometryJsonSerializer())
                .registerTypeHierarchyAdapter(Geometry.class, new GeometryJsonDeserializer())
                .create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setConverter(new GsonConverter(gson))
                .setEndpoint(ENDPOINT)
                .setRequestInterceptor(new LoginRequestInterceptor(authToken))
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

        private String mUser;
        private String mPass;
        private String mAuth;

        /**
         * constructor to use with user/pass
         * @param pUser
         * @param pPass
         */
        public LoginRequestInterceptor(final String pUser,final String pPass){
            this.mUser = pUser;
            this.mPass = pPass;
        }

        /**
         * constructor to use with auth token
         * @param authToken
         */
        public LoginRequestInterceptor(final String authToken){
            this.mAuth = authToken;
        }

        @Override
        public void intercept(RequestFacade requestFacade) {

            requestFacade.addHeader("Accept", "application/json;");

            if (mUser != null && mPass != null) {
                final String authorizationValue = getB64Auth(mUser, mPass);
                requestFacade.addHeader("Authorization", authorizationValue);
            }else if(mAuth != null){
                requestFacade.addHeader("Authorization", mAuth);
            }else{
                throw new IllegalArgumentException("no password or user available to intercept");
            }
        }

        public static String getB64Auth( String login, String pass ) {

            String source = login + ":" + pass;

            return "Basic " + Base64.encodeToString(source.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);

        }

    }

}
