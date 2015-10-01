package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.ActivityUnitTestCase;
import android.util.Log;

import com.squareup.okhttp.Headers;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import it.geosolutions.android.map.utils.ProjectionUtils;
import it.geosolutions.android.map.wms.WMSLayer;
import it.geosolutions.android.map.wms.WMSLayerChunker;
import it.geosolutions.android.map.wms.WMSRequest;
import it.geosolutions.android.map.wms.WMSSource;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;
import it.geosolutions.android.siigmobile.wps.ValutazioneDannoWPSRequest;

/**
 * Created by Robert Oehler on 24.09.15.
 *
 */
public class BersagliHighlightingTest extends ActivityUnitTestCase<MainActivity> {

    public BersagliHighlightingTest() {
        super(MainActivity.class);
    }


    public void testWMSResultLayer(){

        final Context context = getInstrumentation().getTargetContext();

        assertNotNull(context);

        WMSSource destinationSource = new WMSSource(Config.DESTINATION_WMS_URL);

        // Authorization Headers
        Headers.Builder hbuilder = new Headers.Builder();
        hbuilder.add("Authorization", Config.DESTINATION_AUTHORIZATION);
        destinationSource.setHeaders(hbuilder.build());

        WMSLayer layer = new WMSLayer(destinationSource, Config.WMS_LAYERS[1]);

        layer.setVisibility(true);
        layer.setTiled(false);
        //create base parameters
        HashMap<String, String> baseParams = new HashMap<>();
        baseParams.put("format", "image/png8");
        baseParams.put("transparent", "true");

        baseParams.put("ENV", Config.WMS_ENV[1]);
        baseParams.put("RISKPANEL", Config.WMS_RISKPANEL[1]);
        baseParams.put("DEFAULTENV", Config.WMS_DEFAULTENV[1]);

        //create a cql filter and put it into the params

        final int radius = 200;
        final GeoPoint p = new GeoPoint(45.59950609926247,9.360421657562274);

        BoundingBox bb = new BoundingBox(p.latitude, p.longitude, p.latitude, p.longitude);
        final BoundingBox extended = Util.extend(bb, radius);

        final String minX = String.format(Locale.US, "%f", extended.minLongitude);
        final String minY = String.format(Locale.US, "%f", extended.minLatitude);
        final String maxX = String.format(Locale.US, "%f", extended.maxLongitude);
        final String maxY = String.format(Locale.US, "%f", extended.maxLatitude);

        final String wktPolygon = String.format(Locale.US, "POLYGON ((%s %s, %s %s, %s %s, %s %s, %s %s))",
                minX, minY,
                maxX, minY,
                maxX, maxY,
                minX, maxY,
                minX, minY);

        final String convertedPolygon = SpatialiteUtils.convertWGS84PolygonTo(context, wktPolygon, ValutazioneDannoWPSRequest.DAMAGE_AREA_EPSG);

        final String filter = String.format(Locale.US, "INTERSECTS(geometria,%s)", convertedPolygon);

        baseParams.put(WMSRequest.PARAMS.CQL_FILTER, filter);
        baseParams.put(WMSRequest.PARAMS.MIN_ZOOM, String.valueOf(Config.WMS_BERSAGLI_HIGHLIGHT_MIN_ZOOM));

        layer.setBaseParams(baseParams);

        ArrayList<WMSLayer> layers = new ArrayList<>();

        layers.add(layer);

        //simulate the handling of the wms rendering pipeline

        ArrayList<WMSRequest> requests = WMSLayerChunker.createChunkedRequests(layers);

        assertNotNull(requests);

        assertTrue(requests.size() > 0);

        final int width = 1080;
        final int height = 1347;
        final BoundingBox boundingBox = new BoundingBox(45.579638312554735,9.33988094329834,45.620053811377424,9.386229515075684);

        WMSRequest request = requests.get(0);

        assertNotNull(request);

        assertTrue(request.getMinZoom() == Config.WMS_BERSAGLI_HIGHLIGHT_MIN_ZOOM);

        URL url = request.getURL(createParameters(width, height, boundingBox));

        //connect
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
            // Add Headers
            if(request.getHeaders()!= null){
                for(String n: request.getHeaders().names()) {
                    connection.setRequestProperty(n, request.getHeaders().get(n));
                }
            }
            connection.setConnectTimeout(1000);
        } catch (IOException e2) {
            Log.e("WMS", "error opening connection");
            fail();
            return;
        }
        InputStream is;
        try {
            is = connection.getInputStream();
            Bitmap img = BitmapFactory.decodeStream(is);

            assertNotNull(img);

        } catch (IOException e1) {
            fail();
        }
    }


    private HashMap<String,String> createParameters(final int width, final int height, BoundingBox boundingBox){

        double n = boundingBox.maxLatitude;
        double w = boundingBox.minLongitude;
        double s = boundingBox.minLatitude;
        double e = boundingBox.maxLongitude;
        double nm = ProjectionUtils.toWebMercatorY(n);
        double wm = ProjectionUtils.toWebMercatorX(w);
        double sm =ProjectionUtils.toWebMercatorY(s);
        double em = ProjectionUtils.toWebMercatorX(e);
        HashMap<String,String> params = new HashMap<>();

        params.put("width",(width)+"");
        params.put("height",(height)+"");

        params.put("bbox", wm + "," + sm + "," + em + "," + nm);
        params.put("service","WMS");
        params.put("srs","EPSG:900913");
        params.put("request","GetMap");
        params.put("version","1.1.1");

        return params;

    }
}
