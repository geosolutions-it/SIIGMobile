package it.geosolutions.android.siigmobile;

import android.test.ActivityUnitTestCase;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;

import java.util.ArrayList;
import java.util.Map;

import it.geosolutions.android.map.wfs.geojson.feature.Feature;
import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;
import it.geosolutions.android.siigmobile.wps.RiskWPSRequest;
import it.geosolutions.android.siigmobile.wps.ValutazioneDannoWPSRequest;

/**
 * Created by Robert Oehler on 04.09.15.
 *
 * Tests the creation of a valutazione danno WPS request and its integrity
 *
 * And the extension of a boundingbox by a radius
 *
 */
public class ValutazioneDannoWPSTest extends ActivityUnitTestCase<ComputeFormActivity> {

    public ValutazioneDannoWPSTest(){
        super(ComputeFormActivity.class);
    }


    /**
     * Tests the creation of a wps request
     */
    public void testWPSQueryCreation() {

        assertNotNull(getInstrumentation().getTargetContext());

        final String accident  = "10";
        final String materials = "3,8,1,2,5,6,4,7,9,10,11,12,103,100,101,102,104,105,106,107,108,109,110,111,112,113,114,115,116,117,119,120,121,122,123,124,125,126,127,128,129,130,131,132,134,135,136,137,138,139";
        final String entities = "0,1";

        final int bufferWidth = 100;

        final GeoPoint p = new GeoPoint(9.054283,45.821871);

        BoundingBox bb = new BoundingBox(p.latitude, p.longitude, p.latitude, p.longitude);

        BoundingBox extended = Util.extend(bb, bufferWidth);

        Coordinate[] coords = new Coordinate[2];
        coords[0] = new Coordinate(extended.minLongitude, extended.minLatitude);
        coords[1] = new Coordinate(extended.maxLongitude, extended.maxLatitude);

        GeometryFactory fact = new GeometryFactory();
        FeatureCollection features = new FeatureCollection();
        features.features = new ArrayList<>();
        Feature bbox = new Feature();
        bbox.geometry = fact.createLineString(coords);
        features.features.add(bbox);

        final ValutazioneDannoWPSRequest request = new ValutazioneDannoWPSRequest(
                features,
                materials,
                accident,
                entities);

        final String query  = request.createWPSCallFromText(getInstrumentation().getTargetContext()); //this adds additional parameters

        assertNotNull(query);

        // Basic testing
        assertNotNull(request);

        assertNotNull(request.getFeatureCollection());

        assertNotNull(request.getParameters());


        //Ensure parameters are correctly set
        Map<String, Object> parameters = request.getParameters();

        assertNotNull(parameters);

        assertNotNull(parameters.get(RiskWPSRequest.KEY_FORMULA));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_PROCESSING));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_PRECISION));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_TARGET));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_EXTENDEDSCHEMA));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_MOBILE));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_ENTITIES));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_FP));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_MATERIALS));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_SEVERENESS));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_LEVEL));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_BATCH));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_STORE));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_SCENARIOS));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_KEMLER));


    }

    /**
     * tests the correctness of the extension of a boundingbox by a distance
     * which is used to "inflate" the area around a coordinate by a given radius
     *
     */
    public void testBoundingBoxExtension(){


        final GeoPoint p = new GeoPoint(45.821871,9.054283);

        final int extension = 100;

        final GeoPoint expectedLowerLeft  = new GeoPoint(45.820972, 9.052993);
        final GeoPoint expectedUpperRight = new GeoPoint(45.822769, 9.055572);


        final BoundingBox bb = new BoundingBox(p.latitude, p.longitude, p.latitude, p.longitude);

        final BoundingBox extended = Util.extend(bb, extension);

        assertNotNull(extended);

        final double allowedDelta = 0.000001;

        assertEquals(extended.minLatitude,  expectedLowerLeft.latitude, allowedDelta);

        assertEquals(extended.minLongitude, expectedLowerLeft.longitude, allowedDelta);

        assertEquals(extended.maxLatitude,  expectedUpperRight.latitude, allowedDelta);

        assertEquals(extended.maxLongitude,  expectedUpperRight.longitude, allowedDelta);

    }


}
