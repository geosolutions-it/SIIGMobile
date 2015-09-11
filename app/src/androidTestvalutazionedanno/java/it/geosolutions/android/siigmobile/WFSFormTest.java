package it.geosolutions.android.siigmobile;

import android.os.Handler;
import android.test.ActivityUnitTestCase;
import android.util.Log;
import android.widget.Spinner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import it.geosolutions.android.map.adapters.FeatureInfoAttributesAdapter;
import it.geosolutions.android.siigmobile.wfs.WFSBersagliDataActivity;

/**
 * Created by Robert Oehler on 14.09.15.
 *
 */
public class WFSFormTest extends ActivityUnitTestCase<WFSBersagliDataActivity> {


    public WFSFormTest() {
        super(WFSBersagliDataActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        setActivity(launchActivity(getInstrumentation().getTargetContext().getPackageName(), WFSBersagliDataActivity.class, null));

    }

    /**
     * test that the adapter of the form fragment is filled with the result of a wfs request
     */
    public void testWFSForrm(){

        assertNotNull(getActivity());

        assertTrue(Util.isOnline(getInstrumentation().getTargetContext()));

        final WFSBersagliDataActivity.WFSResultFragment fragment = getActivity().getFragment();

        assertNotNull(fragment);

        assertNotNull(fragment.getView());

        final Spinner spinner = (Spinner) getActivity().findViewById(R.id.layer_spinner);

        assertNotNull(spinner.getOnItemSelectedListener());

        final CountDownLatch latch = new CountDownLatch(1);

        final FeatureInfoAttributesAdapter adapter = fragment.getAdapter();

        assertTrue(adapter.getCount() == 0); //empty on create

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                spinner.setSelection(1, true);

                //triggers a request
                spinner.getOnItemSelectedListener().onItemSelected(null, null, 1, 0);


                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        assertNotNull(fragment.getAdapter());

                        assertTrue(adapter.getCount() > 0); // has been filled

                        Log.i("FormTest", "passed");

                        latch.countDown();
                    }
                }, 10000);

            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("time out exceeded");
        }

    }

}
