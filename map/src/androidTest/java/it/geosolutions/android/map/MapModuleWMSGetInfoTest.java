package it.geosolutions.android.map;

import android.os.Bundle;
import android.os.Parcel;
import android.test.InstrumentationTestCase;

import org.mapsforge.core.model.BoundingBox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.geosolutions.android.map.model.query.WMSGetFeatureInfoQuery;
import it.geosolutions.android.map.wms.GetFeatureInfoConfiguration;

/**
 * Created by Robert Oehler on 08.09.15.
 *
 */
public class MapModuleWMSGetInfoTest extends InstrumentationTestCase {

    public final static String VERSION = "1.1.1";
    public final static String FORMAT = "image/png8";
    public final static int EPSG = 3857;

    public final static String PARAMETER_FEATURE_COUNT = "FEATURE_COUNT";
    public final static String PARAMETER_FORMAT = "FORMAT";

    private final static int PIXEL_X = 323;
    private final static int PIXEL_Y = 490;

    private final static String WFS_LAYER_NAME = "wfs_layer";
    private final static String WMS_LAYER_NAME = "wms_layer";

    private final static long[] MAP_SIZE = new long[]{1080,1347};

    private final static BoundingBox BB = new BoundingBox(45.64698713082342,10.048183441162099,45.667174257783245,10.07135772705077);

    public static final int WMS_INFO_FEATURE_COUNT = 10;

    private final static String CRS = String.format(Locale.US, "EPSG:%d", EPSG);

    public static final String[] SUPPORTED_LANGUAGES = new String[]{"en","it","fr","de"};

    /**
     * tests the correct serialization of GetFeatureInfoConfiguration objects
     *
     * and the parceling of WMSGetFeatureInfoQuery objects
     */
    public void testSerializationAndParceling(){

        final String local_code = Locale.getDefault().getLanguage();
        boolean supported = false;
        for (String locale : SUPPORTED_LANGUAGES) {
            if (local_code.toLowerCase().equals(locale)) {
                supported = true;
                break;
            }
        }
        final String env = String.format(Locale.US, "locale:%s", supported ? local_code : SUPPORTED_LANGUAGES[0]);

        final HashMap<String, String> additionalParameters = new HashMap<>();

        additionalParameters.put(PARAMETER_FEATURE_COUNT, String.valueOf(WMS_INFO_FEATURE_COUNT));
        additionalParameters.put(PARAMETER_FORMAT, FORMAT);

        WMSGetFeatureInfoQuery query = new WMSGetFeatureInfoQuery();
        query.setLat(BB.minLatitude);
        query.setLon(BB.minLongitude);
        query.setPixel_X(PIXEL_X);
        query.setPixel_Y(PIXEL_Y);
        query.setZoomLevel((byte) 10);
        query.setMapSize(MAP_SIZE);
        query.setBbox(BB);
        query.setLayers(new String[]{WMS_LAYER_NAME});
        query.setQueryLayers(new String[]{WFS_LAYER_NAME});
        query.setVersion(VERSION);
        query.setCrs(CRS);
        query.setLocale(env);
        query.setStyles("");
        query.setAdditionalParams(additionalParameters);



        GetFeatureInfoConfiguration getFeatureInfoConfiguration = new GetFeatureInfoConfiguration();

        //create a config from code
        GetFeatureInfoConfiguration.Locale locale = new GetFeatureInfoConfiguration.Locale();
        locale.setLocale(SUPPORTED_LANGUAGES[0]);

        GetFeatureInfoConfiguration.Layer layer = new GetFeatureInfoConfiguration.Layer();
        layer.setName("layer");

        Map<String,String> map = new HashMap<>();
        map.put("key","value");
        map.put("nice", "one");

        List<GetFeatureInfoConfiguration.Layer> layers = new ArrayList<>();
        layers.add(layer);
        layer.setProperties(map);
        locale.setLayers(layers);
        List<GetFeatureInfoConfiguration.Locale> locales = new ArrayList<>();
        locales.add(locale);

        getFeatureInfoConfiguration.setLocales(locales);

        //test serialization generally
        try {
            new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(getFeatureInfoConfiguration);
        }catch (IOException e){
            fail("error serializing GetFeatureInfoConfiguration");
        }
        query.setLocaleConfig(getFeatureInfoConfiguration.getLocaleForLanguageCode(local_code));

        //put data in bundle
        Bundle b = new Bundle();
        b.putParcelable("query", query);
        b.putSerializable("config", getFeatureInfoConfiguration);

        Parcel parcel = Parcel.obtain();
        b.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);
        Bundle b2 = parcel.readBundle();
        b2.setClassLoader(WMSGetFeatureInfoQuery.class.getClassLoader());

        //recreate
        WMSGetFeatureInfoQuery parcelQuery = b2.getParcelable("query");
        GetFeatureInfoConfiguration bundleConfig = (GetFeatureInfoConfiguration) b2.getSerializable("config");

        assertNotNull(bundleConfig);
        assertNotNull(bundleConfig.getLocales());

        //test serialization in detail
        for(int i = 0; i < getFeatureInfoConfiguration.getLocales().size();i++){

            assertNotNull(bundleConfig.getLocales().get(i));
            assertTrue(compareLocales(getFeatureInfoConfiguration.getLocales().get(i), bundleConfig.getLocales().get(i)));
        }

        //test parceling in detail
        assertNotNull(parcelQuery);

        assertEquals(query.getLat(), parcelQuery.getLat());
        assertEquals(query.getLon(), parcelQuery.getLon());
        assertEquals(query.getRadius(), parcelQuery.getRadius());
        assertEquals(Arrays.toString(query.getLayers()), Arrays.toString(parcelQuery.getLayers()));
        assertEquals(Arrays.toString(query.getQueryLayers()), Arrays.toString(parcelQuery.getQueryLayers()));
        assertEquals(Arrays.toString(query.getMapSize()), Arrays.toString(parcelQuery.getMapSize()));
        assertEquals(query.getBbox().toString(), parcelQuery.getBbox().toString());
        assertEquals(query.getVersion(), parcelQuery.getVersion());
        assertEquals(query.getCrs(), parcelQuery.getCrs());
        assertEquals(query.getLocale(),parcelQuery.getLocale());
        assertEquals(query.getHeaders(),parcelQuery.getHeaders());
        assertEquals(query.getStyles(),parcelQuery.getStyles());
        assertEquals(query.getEndPoint(),parcelQuery.getEndPoint());
        assertEquals(query.getPixel_X(),parcelQuery.getPixel_X());
        assertEquals(query.getPixel_Y(),parcelQuery.getPixel_Y());
        assertTrue(compareLocales(query.getLocaleConfig(), parcelQuery.getLocaleConfig()));
        assertEquals(query.getAdditionalParams(),parcelQuery.getAdditionalParams());


    }

    /**
     * compares two locales of a config
     * @param locale to compare
     * @param other locale to compare
     * @return true if their content is equal or failing during test
     */
    public boolean compareLocales(GetFeatureInfoConfiguration.Locale locale, GetFeatureInfoConfiguration.Locale other) {


        assertEquals(locale.getLocale(), other.getLocale());

        for (int j = 0; j < locale.getLayers().size(); j++) {

            GetFeatureInfoConfiguration.Layer layer = locale.getLayers().get(j);

            assertNotNull(other.getLayers().get(j));
            assertEquals(layer.getName(), other.getLayers().get(j).getName());

            for (Map.Entry<String,String> entry : layer.getProperties().entrySet()) {

                assertTrue(other.getLayers().get(j).getProperties().containsKey(entry.getKey()));
                assertTrue(other.getLayers().get(j).getProperties().containsValue(entry.getValue()));
            }

        }

        return true;
    }

}
