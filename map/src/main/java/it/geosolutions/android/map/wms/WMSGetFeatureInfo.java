package it.geosolutions.android.map.wms;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.vividsolutions.jts.geom.Geometry;

import org.mapsforge.core.model.BoundingBox;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.geosolutions.android.map.utils.ProjectionUtils;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonDeserializer;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonSerializer;
import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;
import retrofit.http.GET;
import retrofit.http.Query;
import retrofit.http.QueryMap;

/**
 * Created by Robert Oehler on 28.08.15.
 *
 * A class to make WMS requests against a GeoServer instance
 *
 * According to the GetFeatureInfo service http://docs.geoserver.org/2.7.1/user/services/wms/reference.html#wms-getfeatureinfo
 *
 */
public class WMSGetFeatureInfo {

    private final static String TAG = WMSGetFeatureInfo.class.getSimpleName();

    //final parameters which are equal for every request
    public final static String SERVICE = "WMS"; //Service name.
    public final static String REQUEST  = "GetFeatureInfo"; //Operation name.
    public final static String INFO_FORMAT = "application/json"; //Format for the feature information response -> always JSON

    //mandatory
    protected final String version;
    protected String[] layers;
    protected final String styles;
    protected final String crs;
    protected final BoundingBox boundingBox;
    protected final long[] mapSize;
    protected final String[] queryLayers;
    protected final int x;
    protected final int y;

    //optional
    private Map<String,String> additionalParameters;

    private RestAdapter restAdapter;
    private String endPoint;
    private String authToken;

    /**
     * Constructor containing the mandatory fields
     *
     * according to http://docs.geoserver.org/2.7.1/user/services/wms/reference.html#wms-getfeatureinfo
     *
     * @param version Service version. Value is one of 1.0.0, 1.1.0, 1.1.1, 1.3.
     * @param layers Layers to display on map. Value is a comma-separated list of layer names.
     * @param styles Styles in which layers are to be rendered.
     * @param crs Spatial Reference System for map output. Value is in form EPSG:nnn.
     * @param boundingBox Bounding box for map extent. Value is minx,miny,maxx,maxy in units of the SRS.
     * @param mapSize Width and height of map output, in pixels.
     * @param queryLayers list of one or more layers to query.
     * @param x X ordinate of query point on map, in pixels
     * @param y Y ordinate of query point on map, in pixels.
     * @param additionalParameters additional parameters which are optional or specific for a server instance
     * @param endPoint the endPoint of the GeoServer instance to user
     */
    public WMSGetFeatureInfo(final String version,
                             final String[] layers,
                             final String styles,
                             final String crs,
                             final BoundingBox boundingBox,
                             long[] mapSize,
                             final String[] queryLayers,
                             final int x,
                             final int y,
                             final Map<String,String> additionalParameters,
                             final String endPoint
                             ){
        this.version = version;
        this.layers = layers;
        this.styles = styles;
        this.crs = crs;
        this.boundingBox = boundingBox;
        this.mapSize = mapSize;
        this.queryLayers = queryLayers;
        this.x = x;
        this.y = y;
        this.additionalParameters = additionalParameters;
        this.endPoint = endPoint;
    }

    /**
     * converts the boundingbox of the entire screen
     * to the size of one pixel
     * and requests this pixel
     * @return the featurecollection containing the information of the request or null in case of error
     */
    public FeatureCollection requestGetFeatureInfo(){

        if(boundingBox == null){
            Log.e(TAG,"boundingbox null, cannot continue");
            return null;
        }

        double n = boundingBox.maxLatitude;
        double w = boundingBox.minLongitude;
        double s = boundingBox.minLatitude;
        double e = boundingBox.maxLongitude;

        double nm = ProjectionUtils.toWebMercatorY(n);
        double wm = ProjectionUtils.toWebMercatorX(w);
        double sm = ProjectionUtils.toWebMercatorY(s);
        double em = ProjectionUtils.toWebMercatorX(e);

        double width  = Math.abs(em - wm);
        double height = Math.abs(nm - sm);

        if(width == 0 || height == 0){
           Log.e(TAG,"bounding box width or height 0, cannot continue");
            return null;
        }

        if(mapSize[0] == 0 || mapSize[1] == 0){
            Log.e(TAG, "invalid map width or height, cannot continue");
            return null;
        }

        double pixel_width  = width / mapSize[0];
        double pixel_height = height / mapSize[1];

        final double pixel_w = wm + x * pixel_width;
        final double pixel_e = pixel_w + pixel_width;
        final double pixel_n = nm - y * pixel_height;
        final double pixel_s = pixel_n - pixel_height;

        final String pixel_bb = String.format(Locale.US, "%f,%f,%f,%f", pixel_w, pixel_s, pixel_e, pixel_n);

        //for tests : get two features
        //pixel_bb = "852645.061290,5565904.856364,852645.210581,5565905.005545";

        //convert query layers array to a comma-separated list of query layer names.
        String queryLayersString = "";
        for(int i = 0; i < queryLayers.length; i++){
            if(i > 0){
                queryLayersString+=",";
            }
            queryLayersString += queryLayers[i];
        }
        //convert layers array to a comma-separated list of layer names.
        String layersString = "";
        for(int i = 0; i < layers.length; i++){
            if(i > 0){
                layersString+=",";
            }
            layersString += layers[i];
        }

        //the bounding box is now of one pixel size
        //hence request pixel (0,0) of an image with size (1,1)
        return getWMSInfo(pixel_bb,1,1,layersString, queryLayersString,0, 0,additionalParameters);

    }


    /**
     * performs the actual synchronous request against the server using Retrofit
     *
     * @param bb the boundingbox as string
     * @param width width of the image
     * @param height height of the image
     * @param layersString  comma-separated list of layer names
     * @param queryLayersString Comma-separated list of one or more layers to query.
     * @param x X ordinate of query point on map, in pixels. 0 is left side.
     * @param y Y ordinate of query point on map, in pixels. 0 is the top.
     * @param additionalParameters map containing additional parameters
     * @return the featurecollection containing the information of the request or null in case of error
     */
    private FeatureCollection getWMSInfo(
            final String bb,
            final int width,
            final int height,
            final String layersString,
            final String queryLayersString,
            final int x,
            final int y,
            final Map<String,String> additionalParameters){



        WMSGetFeatureInfoService wmsGetFeatureInfoService = getRestAdapter().create(WMSGetFeatureInfoService.class);

        return wmsGetFeatureInfoService.getWMSFeatureInfo(
                layersString,
                queryLayersString,
                styles,
                SERVICE,
                version,
                REQUEST,
                bb,
                width,
                height,
                INFO_FORMAT,
                crs,
                x,
                y,
                additionalParameters);
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public RestAdapter getRestAdapter(){

        if(restAdapter == null){

            Gson gson = new GsonBuilder()
                    .disableHtmlEscaping()
                    .registerTypeHierarchyAdapter(Geometry.class,
                            new GeometryJsonSerializer())
                    .registerTypeHierarchyAdapter(Geometry.class,
                            new GeometryJsonDeserializer()).create();

            final OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setReadTimeout(1, TimeUnit.MINUTES);
            okHttpClient.setConnectTimeout(1, TimeUnit.MINUTES);


            RestAdapter.Builder builder = new RestAdapter.Builder();
            builder.setEndpoint(endPoint);
            //builder.setLogLevel(RestAdapter.LogLevel.FULL);
            builder.setConverter(new GsonConverter(gson));
            if(authToken != null){
                builder.setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Authorization", authToken);
                    }
                });
            }
            builder.setClient(new OkClient(okHttpClient));

            restAdapter = builder.build();
        }

        return restAdapter;
    }

    /**
     * service definition for the getFeatureInfo service
     */
    public interface WMSGetFeatureInfoService {

        @GET("/")
        FeatureCollection getWMSFeatureInfo(
                                @Query("LAYERS")        String layers,
                                @Query("QUERY_LAYERS")  String queryLayers,
                                @Query("STYLES")        String styles,
                                @Query("SERVICE")       String service,
                                @Query("VERSION")       String version,
                                @Query("REQUEST")       String request,
                                @Query("BBOX")          String boundingBox,
                                @Query("WIDTH")         int width,
                                @Query("HEIGHT")        int height,
                                @Query("INFO_FORMAT")   String info_format,
                                @Query("SRS")           String epsg,
                                @Query("X")             int x,
                                @Query("Y")             int y,
                                @QueryMap               Map<String,String> options


        );
    }

}
