package it.geosolutions.android.siigmobile;

import android.test.ActivityUnitTestCase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Geometry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import it.geosolutions.android.map.wfs.geojson.GeometryJsonDeserializer;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonSerializer;
import it.geosolutions.android.map.wfs.geojson.feature.Feature;

/**
 * Created by robertoehler on 06.07.15.
 */
public class GeometryDeserializingTest  extends ActivityUnitTestCase<MainActivity> {

    public GeometryDeserializingTest(){
        super(MainActivity.class);
    }

    public void testMultiLineDeserialization(){

        InputStream inputStream = getInstrumentation().getTargetContext().getResources().openRawResource(R.raw.dummy_response_multipolygon );

        assertNotNull(inputStream);

        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        final Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeHierarchyAdapter(Geometry.class,
                        new GeometryJsonSerializer())
                .registerTypeHierarchyAdapter(Geometry.class,
                        new GeometryJsonDeserializer()).create();

        final CRSFeatureCollection result = gson.fromJson(reader, CRSFeatureCollection.class);

        assertNotNull(result);

        assertNotNull(result.features);

        assertEquals(result.features.get(0).geometry.getGeometryType().toUpperCase(), "MULTIPOLYGON");

        assertEquals(result.features.size(),2);

        for (Feature feature : result.features) {

            assertNotNull(feature.geometry);
        }

    }
}
