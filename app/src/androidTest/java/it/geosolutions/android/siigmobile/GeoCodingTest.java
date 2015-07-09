package it.geosolutions.android.siigmobile;

import android.database.Cursor;
import android.test.ActivityUnitTestCase;
import android.util.Log;

import java.util.concurrent.CountDownLatch;

import it.geosolutions.android.siigmobile.geocoding.AndroidGeoCoder;
import it.geosolutions.android.siigmobile.geocoding.GeoCodingTask;
import it.geosolutions.android.siigmobile.geocoding.IGeoCoder;
import it.geosolutions.android.siigmobile.geocoding.NominatimGeoCoder;

/**
 * Created by Robert Oehler on 09.07.15.
 *
 * Test the currently two IGeoCoder Implementations
 *
 * AndroidGeoCoder
 * NominatimGeoCoder
 *
 */
public class GeoCodingTest extends ActivityUnitTestCase<MainActivity> {

    private static final String TAG = GeoCodingTest.class.getSimpleName();

    public GeoCodingTest(){
        super(MainActivity.class);
    }


    public void testAndroidGeoCoder(){

        assertTrue(Util.isOnline(getInstrumentation().getTargetContext()));

        doTestGeoCoder(new AndroidGeoCoder(getInstrumentation().getTargetContext()));

    }

    public void testNominatimGeoCoder(){

        assertTrue(Util.isOnline(getInstrumentation().getTargetContext()));

        doTestGeoCoder(new NominatimGeoCoder());

    }
    private void doTestGeoCoder(final IGeoCoder geoCoder){

        final CountDownLatch latch = new CountDownLatch(1);

        final String query = "Pisa";

        final GeoCodingTask geoCodingTask = new GeoCodingTask(geoCoder,query) {
            @Override
            public void done(Cursor cursor) {

                assertNotNull(cursor);

                assertTrue(cursor.moveToFirst());

                final String title = cursor.getString(cursor.getColumnIndex("title"));
                final String lat = cursor.getString(cursor.getColumnIndex("lat"));
                final String lon = cursor.getString(cursor.getColumnIndex("lon"));

                assertTrue(title.toLowerCase().contains(query.toLowerCase()));

                assertTrue(lat.startsWith("43.7"));

                assertTrue(lon.startsWith("10.4"));

                //release
                latch.countDown();
            }
        };
        geoCodingTask.execute();

        try {
            //wait for completion
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "error awaiting latch", e);
        }
    }

}
