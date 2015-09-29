package it.geosolutions.android.siigmobile;

import android.test.ActivityUnitTestCase;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Robert Oehler on 04.09.15.
 *
 * tests the creation of the "valutazione danno" form
 * and the correct activation/disactivation of the compute button
 * according to the selection of the spinners
 *
 */
public class ComputeFormActivityTest extends ActivityUnitTestCase<ComputeFormActivity> {



    public ComputeFormActivityTest() {
        super(ComputeFormActivity.class);
    }

    public ComputeFormActivityTest(Class<ComputeFormActivity> activityClass) {
        super(activityClass);
    }


    public void setUp() throws Exception {
        super.setUp();

        setActivity(launchActivity(getInstrumentation().getTargetContext().getPackageName(), ComputeFormActivity.class, null));

    }

    public void ignoretestForm(){


        assertNotNull(getActivity());

        final ComputeFormActivity.PlaceholderFragment fragment = getActivity().getFragment();

        assertNotNull(fragment);

        assertNotNull(fragment.getView());

        final View rootView = fragment.getView();

        final EditText titleEd = (EditText) rootView.findViewById(R.id.edittext_title);

        final Spinner methodSpinner = (Spinner) rootView.findViewById(R.id.spinner_method);

        final Button computeButton = (Button) rootView.findViewById(R.id.compute_button);

        final CountDownLatch latch = new CountDownLatch(1);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                assertTrue(TextUtils.isEmpty(titleEd.getText().toString()));

                assertFalse(computeButton.isEnabled());

                final String title = "nice title";
                final String bufferwidth = "100";

                titleEd.setText(title);

                assertTrue(titleEd.getText().toString().equals(title));

                methodSpinner.setSelection(1, true);

                //title set and method spinner to "substance" --> button should be enabled
                methodSpinner.getOnItemSelectedListener().onItemSelected(null, null, 1, 0);

                assertTrue(computeButton.isEnabled());

                //switch back to "buffer" selection
                methodSpinner.getOnItemSelectedListener().onItemSelected(null, null, 0, 0);

                assertFalse(computeButton.isEnabled());

                //now set the buffer width

                fragment.getBufferWidthTextWatcher().onTextChanged(bufferwidth, 11, 10, 11);

                //this should have reenabled the button

                assertTrue(computeButton.isEnabled());

                //finally remove the title

                fragment.getTitleTextWatcher().onTextChanged("",0,1,0);

                //this should have disabled the button

                assertFalse(computeButton.isEnabled());

                Log.i("FormTest","passed");

                latch.countDown();
            }
        });

        try {
            latch.await(100, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

    }
}
