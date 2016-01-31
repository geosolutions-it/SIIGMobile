package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import it.geosolutions.android.siigmobile.login.AsyncShibbolethClient;

/**
 * Created by Robert Oehler on 11.11.15.
 *
 */
public class ShibbolethTest extends InstrumentationTestCase {


    /**
     * tests that the authtask returns true for valid credentials
     */
    public void testLoginSuccess() throws Throwable {

        final Context context = getInstrumentation().getTargetContext();

        assertNotNull(context);

        assertTrue(Util.isOnline(context));

        final CountDownLatch latch = new CountDownLatch(1);

        final AsyncShibbolethClient shibboleth = new AsyncShibbolethClient(context, false);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {

                shibboleth.authenticate(Config.SP_CONTENT_ENDPOINT, Config.TEST_IDP_ENDPOINT, "myself", "myself", new AsyncShibbolethClient.AuthCallback() {
                    @Override
                    public void authFailed(final String errorMessage, Throwable error) {

                        Log.i("ShibbolethTest", "positive test failed, error");
                        fail();

                        latch.countDown();
                    }

                    @Override
                    public void accessGranted() {

                        Log.i("ShibbolethTest", "positive test passed, okay");

                        latch.countDown();
                    }
                });
            }
        });


        try {
            //wait for completion
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(getClass().getSimpleName(), "error awaiting latch", e);
        }
    }

    /**
     * tests that the authtask returns false for invalid credentials
     */
    public void testLoginFail() throws Throwable {

        final Context context = getInstrumentation().getTargetContext();

        assertNotNull(context);

        assertTrue(Util.isOnline(context));

        final CountDownLatch latch = new CountDownLatch(1);

        final AsyncShibbolethClient shibboleth = new AsyncShibbolethClient(context, false);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {

                shibboleth.authenticate(Config.SP_CONTENT_ENDPOINT, Config.TEST_IDP_ENDPOINT, "you", "me", new AsyncShibbolethClient.AuthCallback() {
                    @Override
                    public void authFailed(final String errorMessage, Throwable error) {

                        Log.i("ShibbolethTest", "negative test failed, okay");
                        latch.countDown();
                    }

                    @Override
                    public void accessGranted() {

                        Log.i("ShibbolethTest", "negative test passed, error");

                        fail();

                        latch.countDown();
                    }
                });
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
