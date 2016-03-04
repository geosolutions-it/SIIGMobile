package it.geosolutions.android.siigmobile;

import android.support.v4.util.Pair;
import android.test.ActivityUnitTestCase;

import org.mapsforge.core.model.BoundingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.geosolutions.android.map.utils.ProjectionUtils;
import it.geosolutions.android.map.wfs.geojson.feature.Feature;
import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;
import it.geosolutions.android.map.wms.WMSGetFeatureInfo;

/**
 * Created by Robert Oehler on 30.08.15.
 *
 */
public class WMSGetInfoTest extends ActivityUnitTestCase<MainActivity> {

    private final static int PIXEL_X = 323;
    private final static int PIXEL_Y = 490;

    private final static String WFS_LAYER_NAME = "destination:popolazione_residente_all";
    private final static String WMS_LAYER_NAME = Config.WMS_LAYERS[1];

    private final static long[] MAP_SIZE = new long[]{1080,1347};

    private final static BoundingBox BB = new BoundingBox(45.64698713082342,10.048183441162099,45.667174257783245,10.07135772705077);

    private final static String CRS = String.format(Locale.US,"EPSG:%d", Config.WMS_GETINFO_EPSG);
    private final static String ENV = "locale:" + Locale.getDefault().getLanguage();

    public WMSGetInfoTest() {
        super(MainActivity.class);
    }


    /**
     * test the basic functionality of the WMSGetFeatureInfo service implementation
     * and the correctness of the concentration on the selected pixel
     *
     * the current implementation is used which calculates a bounding box around the selected pixel
     * and its result is compared against a query with the complete bounding box and the pixel selection
     * (323,490)
     *
     * this is done using the destination endpoint and authentification
     *
     */
    public void testWMSGetInfo(){


        final HashMap<String,String> additionalParameters = new HashMap<>();

        additionalParameters.put(Config.WMS_GETINFO_PARAMETER_FEATURE_COUNT, String.valueOf(Config.WMS_INFO_FEATURE_COUNT));
        additionalParameters.put(Config.WMS_GETINFO_PARAMETER_FORMAT, Config.WMS_GETINFO_FORMAT);
        additionalParameters.put("ENV", ENV);

        final WMSGetFeatureInfo getFeatureInfo = new WMSGetFeatureInfo(
                Config.WMS_GETINFO_VERSION,
                new String[]{WMS_LAYER_NAME},
                Config.WMS_GETINFO_STYLES,
                CRS,
                BB,
                MAP_SIZE,
                new String[]{WFS_LAYER_NAME},
                (int) Math.rint(PIXEL_X),
                (int) Math.rint(PIXEL_Y),
                additionalParameters,
                Config.DESTINATION_WMS_URL);

        List<Pair<String,String>> headers = new ArrayList<Pair<String, String>>();
        headers.add(new Pair<String, String>("Authorization", Config.DESTINATION_AUTHORIZATION));

        getFeatureInfo.setHeaders(headers);

        FeatureCollection result = getFeatureInfo.requestGetFeatureInfo();

        assertNotNull(result);
        assertNotNull(result.features);
        assertTrue(result.features.size() > 0);

        //having the result for the focused request, now do a manual one

        final String pixel_bb = String.format(Locale.US, "%f,%f,%f,%f",
                ProjectionUtils.toWebMercatorX(BB.minLongitude),
                ProjectionUtils.toWebMercatorY(BB.minLatitude),
                ProjectionUtils.toWebMercatorX(BB.maxLongitude),
                ProjectionUtils.toWebMercatorY(BB.maxLatitude));


        WMSGetFeatureInfo.WMSGetFeatureInfoService wmsGetFeatureInfoService = getFeatureInfo.getRestAdapter().create(WMSGetFeatureInfo.WMSGetFeatureInfoService.class);

        FeatureCollection featureCollection = wmsGetFeatureInfoService.getWMSFeatureInfo(
                WMS_LAYER_NAME,
                WFS_LAYER_NAME,
                Config.WMS_GETINFO_STYLES,
                WMSGetFeatureInfo.SERVICE,
                Config.WMS_GETINFO_VERSION,
                WMSGetFeatureInfo.REQUEST,
                pixel_bb,
                (int) MAP_SIZE[0],
                (int) MAP_SIZE[1],
                WMSGetFeatureInfo.INFO_FORMAT,
                CRS,
                PIXEL_X,
                PIXEL_Y,
                additionalParameters);

        assertNotNull(featureCollection);
        assertNotNull(featureCollection.features);
        assertTrue(featureCollection.features.size() > 0);

        Feature fullFeature = featureCollection.features.get(0);
        Feature focusFeature = result.features.get(0);

        assertEquals(result.features.size(),featureCollection.features.size());

        assertEquals(fullFeature.geometry.compareTo(focusFeature.geometry), 0);

        for (Map.Entry<String,Object> entry : fullFeature.properties.entrySet()) {
            assertTrue(focusFeature.properties.containsKey(entry.getKey()));
            if(entry.getValue() != null){
                assertTrue(focusFeature.properties.containsValue(entry.getValue()));
            }
            assertEquals(fullFeature.properties.get(entry.getKey()),focusFeature.properties.get(entry.getKey()));
        }
    }

}
