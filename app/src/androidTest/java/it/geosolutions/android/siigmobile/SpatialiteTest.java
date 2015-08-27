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
import jsqlite.Stmt;

/**
 * Created by Robert Oehler on 30.06.15.
 *
 */
public class SpatialiteTest extends ActivityUnitTestCase<MainActivity> {

    private final static String TAG = SpatialiteTest.class.getSimpleName();

    private final static String EXTERNAL_TEST_FILE = "grafo_5.sqlite";


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


    /**
     * Compares the spatialite version numbers
     * of the "internal" database which is created when the application is installed
     * and an "external" one, which is downloaded at first install
     *
     * asserts that the internal version is even or newer than the external version
     */
    public void disabledtestInternalAndExternalVersions(){

        assertTrue(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED));

        final File sdcardDir = Environment.getExternalStorageDirectory();

        assertTrue(sdcardDir.canRead());
        //open internal db
        Database internalDatabase  = SpatialiteUtils.openSpatialiteDB(getInstrumentation().getTargetContext(), SpatialiteUtils.APP_PATH + "/" + SpatialiteUtils.DB_NAME);

        assertNotNull(internalDatabase);
        //check that external file exists
        assertTrue(new File(sdcardDir,SpatialiteUtils.APP_PATH + "/" + EXTERNAL_TEST_FILE).exists());
        //open external db
        Database externalDatabase  = SpatialiteUtils.openSpatialiteDB(getInstrumentation().getTargetContext(), SpatialiteUtils.APP_PATH + "/" + EXTERNAL_TEST_FILE);

        assertNotNull(externalDatabase);

        String internalVersion = null, externalVersion = null;

        try {
            //extract spatialite versions
            Stmt intStmt = internalDatabase.prepare("SELECT ver_splite FROM spatialite_history ORDER by timestamp desc;");
            if (intStmt.step()) {
                internalVersion = intStmt.column_string(0);
            }

            Stmt extStmt = externalDatabase.prepare("SELECT ver_splite FROM spatialite_history ORDER by timestamp desc;");
            if (extStmt.step()) {
                externalVersion = extStmt.column_string(0);
            }

            assertNotNull(internalVersion);

            assertNotNull(externalVersion);

            if(internalVersion.contains("-stable")){
                internalVersion = internalVersion.substring(0,internalVersion.lastIndexOf("-stable"));
            }
            if(externalVersion.contains("-stable")){
                externalVersion = externalVersion.substring(0,externalVersion.lastIndexOf("-stable"));
            }

            //compare
            assertTrue(versionCompare(internalVersion,externalVersion) >= 0);

        }catch(jsqlite.Exception e){
            Log.e(TAG,"exception reading version numbers");
        } finally {
            try {
                internalDatabase.close();
                externalDatabase.close();
            } catch (jsqlite.Exception e) {
                Log.e(TAG, "exception closing databases");
            }
        }


    }
    /**
     * Compares two version strings.
     *
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     *
     * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less than str2.
     *         The result is a positive integer if str1 is _numerically_ greater than str2.
     *         The result is zero if the strings are _numerically_ equal.
     *
     * src : http://stackoverflow.com/questions/6701948/efficient-way-to-compare-version-strings-in-java
     */
    private Integer versionCompare(String str1, String str2) {

        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i]))
        {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length)
        {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else
        {
            return Integer.signum(vals1.length - vals2.length);
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
