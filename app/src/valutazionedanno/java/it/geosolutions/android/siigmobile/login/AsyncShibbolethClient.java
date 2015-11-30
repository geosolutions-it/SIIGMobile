package it.geosolutions.android.siigmobile.login;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.cookie.Cookie;
import it.geosolutions.android.siigmobile.R;

/**
 * Created by Robert Oehler on 18.11.15.
 *
 * Class which handles the authentication flow of Shibboleth
 *
 * using the android-async-http library
 * 
 * It consists in the following steps :
 *
 * 1.request the content at the SP endpoint
 *      1a. if a valid cookie was used the SP returns the content -> done
 *      1b. if not the SP returns a redirect to the IDP
 *
 * 2. Request the Auth form of the IDP
 * 3. "Fill" the form with the provided credentials and submit it
 * 4. Parse the response
 *      4a. in case of invalid credentials the IDP resends the auth form -> fail and request other credentials
 *      4b. in case of valid credentials the IDP returns "relaystate" and "SAMLResponse"
 *
 * 5. Submit relaystate and SAMLresponse to the SP to consume the assertion
 * 6. Request again the content as in step 1. The SP should now return the content and/or a cookie for further use
 *
 */

public  class AsyncShibbolethClient {
    
    private final static String TAG = "AShibClient";

    private AsyncHttpClient client;
    private Context context;

    public AsyncShibbolethClient(final Context context, final boolean useCookies) {

        this.context = context;
        client = new AsyncHttpClient();

        if(useCookies) {
            PersistentCookieStore myCookieStore = new PersistentCookieStore(context);
            for (Cookie cookie : myCookieStore.getCookies()) {
                Log.i(TAG, "cookie store contains : " + cookie.toString());
            }
            client.setCookieStore(myCookieStore);
        }
    }

    public void authenticate(final String spEndPoint, final String idpEndPoint, final String user, final String pass,@NonNull final AuthCallback callback){

        //1. request content at SP
        client.setEnableRedirects(false);
        client.get(spEndPoint, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                //TODO using AsyncHttpClient get() to the sp using valid cookies results in 200 -> check if that is true also for production
                callback.accessGranted();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, final Throwable error) {

                if(statusCode == 302) {
                    //2.we are not authorized - request auth form

                    String response = new String(responseBody);
                    Document responseDoc = Jsoup.parse(response);
                    Element link = responseDoc.select("a").first();
                    String href = link.attr("href");

                    client.setEnableRedirects(true);
                    client.post(href, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                            String response = new String(responseBody);

                            Document idpDoc = Jsoup.parse(response);
                            Element idpFormElement = idpDoc.select("form").get(0);
                            String formAction = idpFormElement.attr("action");
                            //3. having the idp action path we can perform the auth

                            RequestParams requestParams = new RequestParams();
                            requestParams.add("j_username", user);
                            requestParams.add("j_password", pass);

                            client.post(idpEndPoint + formAction, requestParams, new AsyncHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                                    String response = new String(responseBody);

                                    Document authResponseDoc = Jsoup.parse(response);
                                    Element authResponseFormElement = authResponseDoc.select("form").get(0);
                                    Element relayStateElement = authResponseDoc.select("input").get(0);
                                    Element SAMLResponseElement = authResponseDoc.select("input").get(1);

                                    String action = null, relayStateValue = null, SAMLResponseValue = null;
                                    if (authResponseFormElement != null) {
                                        action = authResponseFormElement.attr("action");
                                    }
                                    if (relayStateElement != null) {
                                        relayStateValue = relayStateElement.attr("value");
                                    }
                                    if (SAMLResponseElement != null) {
                                        SAMLResponseValue = SAMLResponseElement.attr("value");
                                    }

                                    if (relayStateValue == null || TextUtils.isEmpty(relayStateValue) ||
                                            SAMLResponseValue == null || TextUtils.isEmpty(SAMLResponseValue)) {
                                        //4a.
                                        callback.authFailed(context.getString(R.string.login_error_credentials), null);

                                    } else {

                                        //4b. we have relaystate and SAMLResponseValue -> we're logged send them to the SP
                                        RequestParams requestParams = new RequestParams();
                                        requestParams.add("RelayState", relayStateValue);
                                        requestParams.add("SAMLResponse", SAMLResponseValue);

                                        //5. consume assertion
                                        client.post(action, requestParams, new AsyncHttpResponseHandler() {
                                            @Override
                                            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                                                //no need to parse the consume response, just assert status is ok

                                                if(statusCode == 200){
                                                    
                                                    //6.now we should be able to access the content
                                                    client.setEnableRedirects(false);
                                                    client.get(spEndPoint, new AsyncHttpResponseHandler() {
                                                        @Override
                                                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                                                            if(statusCode == 200) {
                                                                //no need to parse cookie, is done by library
                                                                //we have access
                                                                callback.accessGranted();
                                                            }
                                                        }

                                                        @Override
                                                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                                                            Log.w(TAG, "error accessing content after auth" + statusCode);
                                                            callback.authFailed(context.getString(R.string.login_error_assertion), error);
                                                        }
                                                    });

                                                }else{
                                                    Log.w(TAG, "error consume assertion" + statusCode);
                                                    callback.authFailed(context.getString(R.string.login_error_assertion), error);
                                                }
                                            }

                                            @Override
                                            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                                                Log.w(TAG, "error consume assertion" + statusCode);
                                                callback.authFailed(context.getString(R.string.login_error_assertion),error);
                                            }
                                        });

                                    }
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                                    Log.w(TAG, "error authResponse" + statusCode);
                                    callback.authFailed(context.getString(R.string.login_error_assertion), error);
                                }
                            });
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                            Log.w(TAG, "error perform auth" + statusCode);
                            callback.authFailed(context.getString(R.string.login_error_assertion), error);
                        }
                    });


                }else{
                    //unexpected state
                    callback.authFailed(context.getString(R.string.login_error_unexpected_status), error);
                }
            }
        });

    }

    public interface AuthCallback
    {
        void authFailed(final String errorMessage, Throwable error);

        void accessGranted();
    }
}

