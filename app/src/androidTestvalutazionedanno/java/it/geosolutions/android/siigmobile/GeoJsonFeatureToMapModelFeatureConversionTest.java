package it.geosolutions.android.siigmobile;

import android.test.InstrumentationTestCase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Geometry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import it.geosolutions.android.map.model.Feature;
import it.geosolutions.android.map.utils.ConversionUtilities;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonDeserializer;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonSerializer;
import it.geosolutions.android.map.wms.GetFeatureInfoConfiguration;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;

/**
 * Created by Robert Oehler on 14.09.15.
 *
 */
public class GeoJsonFeatureToMapModelFeatureConversionTest extends InstrumentationTestCase {

    /**
     * test that the conversion of
     * it.geosolutions.android.map.wfs.geojson.feature.Feature
     * to
     * it.geosolutions.android.map.model.Feature
     * generally works
     *
     * the specific content depends on the configuration
     * in R.raw.getfeature_config.js and the current locale
     * and it not tested here
     *
     */
    public void testConversion(){

        assertNotNull(getInstrumentation().getTargetContext());


        final InputStream is =  getInstrumentation().getTargetContext().getResources().openRawResource(R.raw.wfs_dummy_response);

        Gson gson = new GsonBuilder()
                      .disableHtmlEscaping()
                            .registerTypeHierarchyAdapter(Geometry.class,
                                    new GeometryJsonSerializer())
                            .registerTypeHierarchyAdapter(Geometry.class,
                                    new GeometryJsonDeserializer()).create();

        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        CRSFeatureCollection coll = gson.fromJson(reader, CRSFeatureCollection.class);

        assertNotNull(coll);
        assertNotNull(coll.features);

        final String layerName = Config.WFS_LAYERS[1];

        GetFeatureInfoConfiguration.Locale locale = Util.getLocaleConfig(getInstrumentation().getTargetContext());

        assertNotNull(locale);

        ArrayList<Feature> mapModelFeatures = ConversionUtilities.convertWFSFeatures(locale, layerName, coll.features);

        assertNotNull(mapModelFeatures);

        assertTrue(mapModelFeatures.size() > 0);

        for(Feature feature : mapModelFeatures){
            assertTrue(feature.size() > 0);
        }

        assertEquals(coll.features.size(), mapModelFeatures.size());

    }
}
