package it.geosolutions.android.siigmobile.login.auth;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.client.ClientProtocolException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import it.geosolutions.android.siigmobile.R;
import it.geosolutions.android.siigmobile.login.fragments.LoginFragment;

/**
 * Created by Robert Oehler on 01.11.15.
 *
 * Task that handles the Shibboleth login process
 *
 * Its result is reported using the abstract method done(boolean, String)
 *
 * Results can be
 *  1. Login failed - returning false and an error message
 *  2. Login successful using the cookie - returning true and null
 *  3. Login successful using credentials - returning true and the cookie
 *
 *  If you have a cookie set it via setCookie(String cookie)
 *  If you have a specific idp endPoint set it via setIdpEndPoint()
 *  otherwise the endpoint is determined via the identity provider
 *
 */
public abstract class AuthTask extends AsyncTask<String, Void, Void> {

    public abstract void done(final ShibAuthResult result);

    private Context mContext;
    private LoginFragment.IdentityProvider mProvider;
    private String mCookie;
    private String mSpEndPoint;
    private String mIdpEndPoint;

    public AuthTask(final Context context, final LoginFragment.IdentityProvider provider, final String spEndPoint){

        this.mContext = context;
        this.mProvider = provider;
        this.mSpEndPoint = spEndPoint;
    }

    public void setCookie(String cookie) {
        this.mCookie = cookie;
    }

    public void setIdpEndPoint(String idpEndPoint) {
        this.mIdpEndPoint = idpEndPoint;
    }

    @Override
    protected Void doInBackground(String... params) {

        if(params == null || params.length < 2){
            done(ShibAuthResult.failure(mContext.getString(R.string.snackbar_noparam_text)));
            return null;
        }

        final String user = params[0];
        final String pass = params[1];

        //if no specific endpoint is set, get it according to the id provider
        if(mIdpEndPoint == null){
            mIdpEndPoint = getEndPointAccordingToIdentityProvider(mProvider);
        }

        try {

            final ShibClient shibClient = new ShibClient(mContext);

            //if we have a cookie set it
            if(mCookie != null) {
                shibClient.setCookie(mCookie);
            }
            //start auth flow and return response to caller
            shibClient.authenticate(mSpEndPoint, mIdpEndPoint, user, pass, new ShibClient.AuthCallback() {
                @Override
                public void authFailed(final String errorMessage) {

                    done(ShibAuthResult.failure(errorMessage));
                }
                @Override
                public void accessViaCookie(){

                    done(ShibAuthResult.successViaCookie());
                }

                @Override
                public void accessViaCredentials(final String cookie) {

                    done(ShibAuthResult.successViaCredentials(cookie));
                }
            });


        } catch (KeyManagementException e) {
            Log.e("AuthTask","error auth",e);
            done(ShibAuthResult.failure("KeyManagementException"));
        } catch (NoSuchAlgorithmException e) {
            Log.e("AuthTask", "error auth", e);
            done(ShibAuthResult.failure("NoSuchAlgorithmException"));
        } catch (KeyStoreException e) {
            Log.e("AuthTask","error auth",e);
            done(ShibAuthResult.failure("KeyStoreException"));
        } catch (UnrecoverableKeyException e) {
            Log.e("AuthTask","error auth",e);
            done(ShibAuthResult.failure("UnrecoverableKeyException"));
        } catch (ClientProtocolException e) {
            Log.e("AuthTask", "error auth", e);
            done(ShibAuthResult.failure("ClientProtocolException"));
        } catch (IOException e) {
            Log.e("AuthTask","error auth",e);
            done(ShibAuthResult.failure("IOException"));
        }

        return null;
    }


    /**
     * returns the endpoint according to the identity provider
     * @param provider
     * @return
     */
    public String getEndPointAccordingToIdentityProvider(final LoginFragment.IdentityProvider provider){


        switch (provider){

            case AOSTA:
                return "https://idpcittadini.partout.it/idp/shibboleth";
            case BOLZANO:
                return "https://idp.prov.bz/idp/shibboleth";
            case LOMBARDIA:
                //TODO
                break;
            case PIEMONTE:
                //TODO
                break;
        }

        return null;

    }
}