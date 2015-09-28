package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.test.ActivityUnitTestCase;

import org.mapsforge.core.model.BoundingBox;

import it.geosolutions.android.siigmobile.elaboration.ElaborationResult;
import it.geosolutions.android.siigmobile.elaboration.Elaborator;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;

/**
 * Created by Robert Oehler on 31.07.15.
 *
 */
public class ElaboratorTest extends ActivityUnitTestCase<MainActivity> {

    public ElaboratorTest(){
        super(MainActivity.class);
    }

    public void testSequentialFormulaElaboration(){

        final Context context = getInstrumentation().getTargetContext();

        final BoundingBox bb = new BoundingBox(Config.LATITUDE_MIN,Config.LONGITUDE_MIN,Config.LATITUDE_MAX,Config.LONGITUDE_MAX);

        final Elaborator elaborator = new Elaborator(context) {

            @Override
            public void showError(int resource) {

                fail(getInstrumentation().getTargetContext().getString(resource));

            }

            @Override
            public void showMessage(String message) {
                //nothing
            }

            @Override
            public void done(ElaborationResult result) {

                assertNotNull(result);

                assertEquals(result.getUserEditedName(),"test");

                final jsqlite.Database db = SpatialiteUtils.openSpatialiteDB(context, SpatialiteUtils.APP_PATH + "/" + SpatialiteUtils.DB_NAME);

                assertNotNull(db);
                //get tables from

                CRSFeatureCollection risk = SpatialiteUtils.loadResult(db, result.getRiskTableName());

                assertNotNull(risk);

                //assert feature count
                assertEquals(risk.totalFeatures.intValue(), risk.features.size());

                assertFalse(risk.isPIS);

                CRSFeatureCollection street = SpatialiteUtils.loadResult(db, result.getStreetTableName());

                assertNotNull(street);

                //assert feature count
                assertEquals(street.totalFeatures.intValue(), street.features.size());

                assertTrue(street.isPIS);

                try {
                    db.close();
                } catch (jsqlite.Exception e) {
                    //ignore
                }

            }
        };

        elaborator.startCalc(bb, true, "test","description");

    }
}
