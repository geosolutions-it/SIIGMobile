package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import it.geosolutions.android.siigmobile.login.auth.AuthTask;
import it.geosolutions.android.siigmobile.login.auth.ShibAuthResult;
import it.geosolutions.android.siigmobile.login.fragments.LoginFragment;

/**
 * Created by Robert Oehler on 11.11.15.
 *
 */
public class ShibbolethTest extends InstrumentationTestCase {


    /**
     * tests that the authtask returns true for valid credentials
     */
    public void testLoginSuccess(){

        final Context context = getInstrumentation().getTargetContext();

        assertNotNull(context);

        assertTrue(Util.isOnline(context));

        final CountDownLatch latch = new CountDownLatch(1);

        final LoginFragment.IdentityProvider identityProvider = LoginFragment.IdentityProvider.PIEMONTE;

        final AuthTask authTask = new AuthTask(context, identityProvider, Config.SP_CONTENT_ENDPOINT) {

            @Override
            public void done(final ShibAuthResult result) {


                assertTrue(result.isSuccess());

                latch.countDown();


            }
        };
        authTask.setIdpEndPoint(Config.TEST_IDP_ENDPOINT);
        authTask.execute("myself","myself");

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
    public void testLoginFail(){

        final Context context = getInstrumentation().getTargetContext();

        assertNotNull(context);

        assertTrue(Util.isOnline(context));

        final CountDownLatch latch = new CountDownLatch(1);

        final LoginFragment.IdentityProvider identityProvider = LoginFragment.IdentityProvider.PIEMONTE;

        final AuthTask authTask = new AuthTask(context, identityProvider, Config.SP_CONTENT_ENDPOINT) {

            @Override
            public void done(final ShibAuthResult result) {


                assertFalse(result.isSuccess());

                latch.countDown();


            }
        };
        authTask.setIdpEndPoint(Config.TEST_IDP_ENDPOINT);
        authTask.execute("you","me");

        try {
            //wait for completion
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(getClass().getSimpleName(), "error awaiting latch", e);
        }

    }


}
