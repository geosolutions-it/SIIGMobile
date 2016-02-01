package it.geosolutions.android.siigmobile.wps;


import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.mime.TypedInput;

/**
 * Created by Robert Oehler on 01.07.15.
 *
 * Interface which defines the services between mobile and geoserver
 *
 */
public interface  SIIGWPSServices {

    /**
     * Post a wps to the server
     * @param body a wrapper containing the raw xml
     * @param cb the callback which is used to parse the response / inform about the process
     */
    @POST("/mobile/wps")
    void postWPS(@Body TypedInput body, Callback<CRSFeatureCollection> cb);


}
