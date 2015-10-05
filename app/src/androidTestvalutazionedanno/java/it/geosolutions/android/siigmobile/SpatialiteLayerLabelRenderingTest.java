package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.os.Environment;
import android.test.ActivityUnitTestCase;
import android.util.Log;
import android.util.Pair;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.CoordinatesUtil;
import org.mapsforge.core.model.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import eu.geopaparazzi.spatialite.database.spatial.core.GeometryIterator;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import it.geosolutions.android.map.database.spatialite.SpatialiteDataSourceHandler;
import it.geosolutions.android.map.spatialite.renderer.LabelGeometryIterator;
import it.geosolutions.android.map.style.AdvancedStyle;
import it.geosolutions.android.map.style.StyleManager;
import it.geosolutions.android.map.utils.MapFilesProvider;
import it.geosolutions.android.map.wfs.geojson.feature.Feature;
import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;
import it.geosolutions.android.siigmobile.wps.SIIGRetrofitClient;
import it.geosolutions.android.siigmobile.wps.ValutazioneDannoWPSRequest;
import jsqlite.*;

/**
 * Created by Robert Oehler on 28.09.15.
 *
 */
public class SpatialiteLayerLabelRenderingTest extends ActivityUnitTestCase<ComputeFormActivity> {


    public SpatialiteLayerLabelRenderingTest() {
        super(ComputeFormActivity.class);
    }


    /**
     * tests the availability of data to render labels on spatialite layers
     *
     * this is done by
     * 1.executing a wps request
     * 2.creating a result table
     * 3.iterating its content applying the current result_ambientale.style
     * 4. reading out the column which is defined by "labelName" in the style
     * 5. checking the availibility of this data
     * and the result (test) table deleted
     */
    public void testLabelRendering(){

        final CountDownLatch latch = new CountDownLatch(1);

        final Context context = getInstrumentation().getTargetContext();

        assertNotNull(context);

        //create a wps request
        final String accident  = "10";
        final String materials = Util.commaSeparatedListForDangers(context);
        final String entities = Util.commaSeparatedListForEntities(context);

        final int bufferWidth = 500;

        final GeoPoint p = new GeoPoint(45.3134357, 9.507105);

        CoordinatesUtil.validateLatitude(p.latitude);
        CoordinatesUtil.validateLongitude(p.longitude);

        BoundingBox bb = new BoundingBox(p.latitude, p.longitude, p.latitude, p.longitude);

        BoundingBox extended = Util.extend(bb, bufferWidth);
        final BoundingBox mapBB    = Util.extend(bb, 1500);

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

        SIIGRetrofitClient.postWPS(query, Config.DESTINATION_AUTHORIZATION, new SIIGRetrofitClient.WPSRequestFeedback() {
            @Override
            public void success(CRSFeatureCollection result) {

                Log.i(getClass().getSimpleName(), " result successfully parsed");

                assertNotNull(result);
                assertTrue(result.features.size() > 0);

                //persist the result
                final Database db = SpatialiteUtils.openSpatialiteDB(context);
                final String geomType = result.features.get(0).geometry.getGeometryType().toUpperCase();
                final Pair<Boolean, String> resultPair = SpatialiteUtils.saveResult(db, result, geomType);

                assertNotNull(resultPair);
                assertTrue(resultPair.first);
                final String resultTableName = resultPair.second;

                //close the db to not interfere with spatialdatabasehandler
                try {
                    db.close();
                } catch (jsqlite.Exception e) {
                    Log.e(getClass().getSimpleName(), "error closing db", e);
                }

                try {
                    //get the style
                    MapFilesProvider.setBaseDir(Config.BASE_DIR_NAME);
                    StyleManager styleManager = StyleManager.getInstance();
                    final AdvancedStyle style = styleManager.getStyle(Config.RESULT_STYLES[0]);

                    assertNotNull(style);
                    assertTrue(style.rules.length > 0);

                    //get the spatial db
                    final String dbPath = Environment.getExternalStorageDirectory() + Config.BASE_DIR_NAME + "/" + SpatialiteUtils.DB_NAME;
                    SpatialiteDataSourceHandler spatialDatabaseHandler = new SpatialiteDataSourceHandler(dbPath);

                    //get the result table as SpatialVectorTable object
                    SpatialVectorTable spatialTable = null;
                    List<SpatialVectorTable> spatialTables = null;
                    try {
                        spatialTables = spatialDatabaseHandler.getSpatialVectorTables(false);
                    } catch (jsqlite.Exception e) {
                        Log.e(getClass().getSimpleName(), "error getting vector tables", e);
                    }
                    assertNotNull(spatialTables);
                    for (SpatialVectorTable table : spatialTables) {
                        if (table.getName().equals(resultTableName)) {
                            spatialTable = table;
                        }
                    }
                    assertNotNull(spatialTable);

                    //get the label name of the symbolizer of the first rule
                    final String labelName = style.rules[0].getSymbolizer().textfield;

                    assertNotNull(labelName);
                    //check that such a column exists
                    assertTrue(spatialDatabaseHandler.checkIfColumnExists(spatialTable,labelName));
                    //check also that some weird column not exists
                    assertFalse(spatialDatabaseHandler.checkIfColumnExists(spatialTable,"dodo"));

                    spatialDatabaseHandler.setAdditionalQueryColumn(labelName);

                    //get a geometry iterator from result table
                    GeometryIterator geometryIterator = spatialDatabaseHandler.getGeometryIteratorInBounds(
                            "4326",
                            spatialTable,
                            mapBB.maxLatitude,
                            mapBB.minLatitude,
                            mapBB.maxLongitude,
                            mapBB.minLongitude
                    );

                    // assert this is an iterator which reads label data
                    assertNotNull(geometryIterator);
                    assertTrue(geometryIterator instanceof LabelGeometryIterator);
                    assertTrue(geometryIterator.hasNext());

                    //simulate rendering --> read data
                    while (geometryIterator.hasNext()) {

                        Geometry geom = geometryIterator.next();
                        assertNotNull(geom);

                        //test that the additional column is read and available
                        Object o = geom.getUserData();
                        assertNotNull(o);

                        //in this case we expect double values
                        assertTrue(o instanceof Double);
                    }

                    Log.i(getClass().getSimpleName(), " objects successfully read");

                    //done close
                    try {
                        spatialDatabaseHandler.close();
                    } catch (jsqlite.Exception e1) {
                        Log.e(getClass().getSimpleName(), "error closing db", e1);
                    }
                } finally {

                    //delete the test table
                    Database db2 = SpatialiteUtils.openSpatialiteDB(context);
                    SpatialiteUtils.deleteResult(db2, resultTableName);

                    try {
                        db2.close();
                    } catch (jsqlite.Exception e1) {
                        Log.e(getClass().getSimpleName(), "error closing db", e1);
                    }
                }

                latch.countDown();
            }

            @Override
            public void error(String errorMessage) {

                fail("error : " + errorMessage);

            }
        });

        try {
            //wait for completion
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(getClass().getSimpleName(), "error awaiting latch", e);
        }


    }
}
