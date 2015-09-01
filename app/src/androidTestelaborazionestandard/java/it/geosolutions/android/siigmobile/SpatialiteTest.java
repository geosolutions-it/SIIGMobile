package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.os.Environment;
import android.test.ActivityUnitTestCase;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Geometry;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import it.geosolutions.android.map.wfs.geojson.GeometryJsonDeserializer;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonSerializer;
import it.geosolutions.android.siigmobile.spatialite.DeleteUnsavedResultsTask;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;
import jsqlite.Database;

/**
 * Created by Robert Oehler on 30.06.15.
 *
 */
public class SpatialiteTest extends ActivityUnitTestCase<MainActivity> {

    private final static String TAG = SpatialiteTest.class.getSimpleName();

    public SpatialiteTest(){
        super(MainActivity.class);
    }


    /**
     *
     * Tests the insertion of the result of a WPS Query into the local database
     *
     * And the subsequent read of the inserted data
     *
     * A dummy response is parsed
     *
     * The database is opened (created if not yet present) and the names table checked and created if necessary
     *
     * The dummy data is inserted, tested for validity, read and the read result compared to the dummy
     *
     * @throws jsqlite.Exception
     */
    public void testLocalWriteAndRead() throws jsqlite.Exception{

        final Context context = getInstrumentation().getTargetContext();

        final CRSFeatureCollection response = getDummyResponse(context);

        assertNotNull(response);

        assertNotNull(response.features);

        assertTrue(response.features.size() > 0);

        final File app_dir = new File(Environment.getExternalStorageDirectory() + "/" + SpatialiteUtils.APP_PATH);

        if(!app_dir.exists()){
            assertTrue(app_dir.mkdir());
        }

        assertTrue(app_dir.exists());

        final jsqlite.Database spatialiteDatabase = SpatialiteUtils.openSpatialiteDB(context, SpatialiteUtils.APP_PATH + "/" + SpatialiteUtils.DB_NAME);

        assertNotNull(spatialiteDatabase);

        //check names table
        if (!SpatialiteUtils.tableExists(spatialiteDatabase, SpatialiteUtils.NAMES_TABLE)) {

            assertTrue(SpatialiteUtils.createNamesTable(spatialiteDatabase));
        }

        //assert insertion success
        Pair<Boolean,String> resultPair = SpatialiteUtils.saveResult(spatialiteDatabase, response, "MULTILINESTRING", false);

        assertTrue(resultPair.first);

        assertNotNull(resultPair.second);
        //query entries

        //read the inserted result
        final CRSFeatureCollection result = SpatialiteUtils.loadResult(spatialiteDatabase,resultPair.second);

        assertNotNull(result);

        //assert feature count
        assertEquals(result.totalFeatures.intValue(), response.features.size());

        //for each feature check rischio and geometry
        for(int i = 0; i < result.totalFeatures; i++) {

            assertEquals(result.features.get(i).properties.get("rischio1"), response.features.get(i).properties.get("rischio1"));

            assertEquals(result.features.get(i).properties.get("rischio2"), response.features.get(i).properties.get("rischio2"));

            assertEquals(result.features.get(i).geometry, response.features.get(i).geometry);
        }

        spatialiteDatabase.close();

    }

    /**
     * tests that persisting and loading of a  the user selected name and description of a table works correctly
     * @throws jsqlite.Exception
     */
    public void testTableNaming() throws jsqlite.Exception {


        final Context context = getInstrumentation().getTargetContext();

        final CRSFeatureCollection response = getDummyResponse(context);

        assertNotNull(response);

        assertNotNull(response.features);

        assertTrue(response.features.size() > 0);

        final jsqlite.Database spatialiteDatabase = SpatialiteUtils.openSpatialiteDB(context, SpatialiteUtils.APP_PATH + "/" + SpatialiteUtils.DB_NAME);

        assertNotNull(spatialiteDatabase);

        //check names table
        if (!SpatialiteUtils.tableExists(spatialiteDatabase, SpatialiteUtils.NAMES_TABLE)) {

            assertTrue(SpatialiteUtils.createNamesTable(spatialiteDatabase));
        }

        //assert insertion success
        final Pair<Boolean,String> resultPair = SpatialiteUtils.saveResult(spatialiteDatabase, response, "MULTILINESTRING", false);

        assertTrue(resultPair.first);

        assertNotNull(resultPair.second);

        final String userTableName = "fooTable";
        final String userTableDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

        assertTrue(SpatialiteUtils.updateNameTableWithUserData(spatialiteDatabase, resultPair.second, userTableName,userTableDescription));

        final Pair<String,String> userEntries = SpatialiteUtils.getTableUserNameAndDescription(spatialiteDatabase, resultPair.second);

        assertNotNull(userEntries);

        assertEquals(userTableName,userEntries.first);

        assertEquals(userTableDescription,userEntries.second);

        spatialiteDatabase.close();



    }

    /**
     * tests that a result for which a user edited name was entered is found
     * by getSavedResultTableNames
     * that a table which has not been edited is not returned by the prior
     * and that the latter is deleted by DeleteUnsavedResultsTask
     */
    public void testUnsavedResultTableDeletion(){

        final Context context = getInstrumentation().getTargetContext();

        final jsqlite.Database spatialiteDatabase = SpatialiteUtils.openSpatialiteDB(context, SpatialiteUtils.APP_PATH + "/" + SpatialiteUtils.DB_NAME);

        assertNotNull(spatialiteDatabase);

        //create two tables, one will be edited later, one not
        final CRSFeatureCollection unEditedResult = getDummyResponse(context);
        final CRSFeatureCollection editedResult = getDummyResponse(context);

        assertNotNull(unEditedResult);
        assertNotNull(editedResult);

        //assert insertion success
        final Pair<Boolean,String> uneditedWriteResultPair = SpatialiteUtils.saveResult(spatialiteDatabase, unEditedResult, "MULTILINESTRING", false);
        final Pair<Boolean,String> editedWriteResultPair = SpatialiteUtils.saveResult(spatialiteDatabase, editedResult, "MULTILINESTRING", false);

        assertTrue(uneditedWriteResultPair.first);
        assertNotNull(uneditedWriteResultPair.second);

        assertTrue(editedWriteResultPair.first);
        assertNotNull(editedWriteResultPair.second);

        final String userEditedName = "great result";
        final String userEditedDesc = "great desc";

        assertTrue(SpatialiteUtils.updateNameTableWithUserData(spatialiteDatabase, editedWriteResultPair.second, userEditedName, userEditedDesc));

        HashMap<String,ArrayList<String[]>> results = SpatialiteUtils.getUnifiedSavedResultTableNames(spatialiteDatabase);

        assertNotNull(results);

        assertTrue(results.size() > 0);

        //assert the user edited tables list contains the edited table
        boolean contains = false;
        for(Map.Entry<String,ArrayList<String[]>> entry : results.entrySet()) {
            ArrayList<String[]> entries = entry.getValue();
            for (String[] array : entries) {

                //assert the unedited table is not inside this result list
                assertTrue(array[0] != null && (!array[0].equals(uneditedWriteResultPair.second)));

                //find instead the edited table
                if (array[1] != null && array[1].equals(userEditedName) && array[2] != null && array[2].equals(userEditedDesc)) {
                    contains = true;
                }
            }
        }


        assertTrue(contains);

        final CountDownLatch latch = new CountDownLatch(1);

        new DeleteUnsavedResultsTask(){

            @Override
            public void done() {

                final Database db = SpatialiteUtils.openSpatialiteDB(getInstrumentation().getTargetContext());

                ArrayList<String> uneditedTables = SpatialiteUtils.getUnSavedResultTableNames(db);

                //this list must not be null
                assertNotNull(uneditedTables);
                //but empty
                assertTrue(uneditedTables.isEmpty());

                try {
                    db.close();
                } catch (jsqlite.Exception e) {
                    Log.e(TAG, "error closing db", e);
                }
                //release
                latch.countDown();
            }

            @Override
            public void started() {
            }
        }.execute(getInstrumentation().getTargetContext());

        try {
            spatialiteDatabase.close();
        } catch (jsqlite.Exception e) {
            Log.e(TAG, "error closing db", e);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "error awaiting execution",e);
        }




    }

    private CRSFeatureCollection getDummyResponse(final Context context){

        InputStream inputStream = context.getResources().openRawResource(R.raw.dummy_response_multilinestring);

        if (inputStream != null) {

            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            final Gson gson = new GsonBuilder()
                    .disableHtmlEscaping()
                    .registerTypeHierarchyAdapter(Geometry.class,
                            new GeometryJsonSerializer())
                    .registerTypeHierarchyAdapter(Geometry.class,
                            new GeometryJsonDeserializer()).create();

            return gson.fromJson(reader, CRSFeatureCollection.class);
        }

        return null;
    }

}
