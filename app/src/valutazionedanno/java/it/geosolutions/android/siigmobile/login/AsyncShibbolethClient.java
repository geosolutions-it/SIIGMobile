package it.geosolutions.android.siigmobile.login;

import android.app.Activity;
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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.conn.ssl.SSLSocketFactory;
import cz.msebera.android.httpclient.cookie.Cookie;
import it.geosolutions.android.siigmobile.Config;
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
public class AsyncShibbolethClient {
    
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
        client.setEnableRedirects(true);
        client.setLoggingEnabled(true);
        try {
            client.setSSLSocketFactory(getCustomSSLSocketFactory());
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }

        client.get(spEndPoint, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                byte[] bytes = new byte[responseBody.length];
                for (int i=0, len=responseBody.length; i<len; i++) {
                    bytes[i] = Byte.parseByte(String.valueOf(responseBody[i]));
                }

                String str = new String(bytes);

                if(str.contains("/iamidp/Authn/X509/Login")){

                    client.post(Config.TEST_IDP_ENDPOINT, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess ( int statusCode, Header[] headers,byte[] responseBody){

                            byte[] bytes = new byte[responseBody.length];
                            for (int i = 0, len = responseBody.length; i < len; i++) {
                                bytes[i] = Byte.parseByte(String.valueOf(responseBody[i]));
                            }

                            String str = new String(bytes);

                            if (str.contains("X509")) {


                            } else {

                                callback.accessGranted();
                            }
                        }

                        @Override
                        public void onFailure ( int statusCode, Header[] headers,byte[] responseBody,
                        final Throwable error){

                            if(statusCode == 302) {
                                String s = new String();
                            }
                                // Error
                            Log.d(AsyncShibbolethClient.TAG, "Status Code: " + statusCode);
                        }
                    });

                }else {

                    callback.accessGranted();
                }
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

    public void authenticateCert(final String spEndPoint, @NonNull final AuthCallback callback){

        //1. request content at SP
        client.setEnableRedirects(true);
        client.setLoggingEnabled(true);

        try {
            client.setSSLSocketFactory(getCustomSSLSocketFactory());
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }

        client.get(spEndPoint, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                String str = new String(responseBody);

                if (str.contains("/iamidp/Authn/X509/Login")) {


                    client.post(Config.TEST_IDP_ENDPOINT, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                            String str = new String(responseBody);

                            if (str.contains("SAMLResponse")) {

                                Log.d("TestShib", "SAMLResponse Found!");

                                String response = new String(responseBody);
                                Document idpDoc = Jsoup.parse(response);
                                Element relayStateElement = idpDoc.select("[name=RelayState]").get(0);
                                Element SAMLResponseElement = idpDoc.select("[name=SAMLResponse]").get(0);
                                String relayState = relayStateElement.attr("value");
                                String SAMLResponse = SAMLResponseElement.attr("value");

                                RequestParams rParams = new RequestParams();
                                rParams.put("RelayState", relayState);
                                rParams.put("SAMLResponse", SAMLResponse);

                                client.post(
                                        "https://tst-destinationpa.territorio.csi.it/territorioliv1wrupext/Shibboleth.sso/SAML2/POST",
                                        rParams,
                                        new AsyncHttpResponseHandler() {
                                            @Override
                                            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                                                String str = new String(responseBody);

                                                callback.accessGranted();
                                            }

                                            @Override
                                            public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                                                  final Throwable error) {

                                                String str = new String(responseBody);

                                                if (statusCode == 302) {
                                                    String s = new String();
                                                }
                                                // Error
                                                Log.w("TestShib", "Direct login Status Code: " + statusCode);
                                            }
                                        });


                            }  else {

                                Log.w("TestShib", "No SAMLResponse Found");
                                callback.authFailed("No SAMLResponse Found", null);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                              final Throwable error) {

                            String str = new String(responseBody);
                            // Error
                            Log.w("TestShib", "Status Code: " + statusCode);
                            Log.w("TestShib", str);
                            callback.authFailed(context.getString(R.string.login_error_unexpected_status), error);
                        }
                    });

                } else if(str.contains("SAMLResponse")){

                    String response = new String(responseBody);
                    Document idpDoc = Jsoup.parse(response);
                    Element relayStateElement = idpDoc.select("[name=RelayState]").get(0);
                    Element SAMLResponseElement = idpDoc.select("[name=SAMLResponse]").get(0);
                    String relayState = relayStateElement.attr("value");
                    String SAMLResponse = SAMLResponseElement.attr("value");

                    RequestParams rParams = new RequestParams();
                    rParams.put("RelayState", relayState);
                    rParams.put("SAMLResponse", SAMLResponse);

                    client.post(
                            "https://tst-destinationpa.territorio.csi.it/territorioliv1wrupext/Shibboleth.sso/SAML2/POST",
                            rParams,
                            new AsyncHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                                    String str = new String(responseBody);

                                    Log.d("EVVAI", str);
                                    callback.accessGranted();
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                                      final Throwable error) {

                                    if(responseBody != null && responseBody.length > 0) {
                                        String str = new String(responseBody);
                                        Log.w("TestShib", "Response: " + str);
                                    }

                                    if (statusCode == 302) {
                                        Log.d("TestShib", "Got a 302");
                                    }
                                    // Error
                                    Log.w("TestShib", "Direct login Status Code: " + statusCode);
                                }
                            });


                } else {

                    callback.accessGranted();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, final Throwable error) {
                //unexpected state
                callback.authFailed(context.getString(R.string.login_error_unexpected_status), error);
            }
        });

    }

    private SSLSocketFactory getCustomSSLSocketFactory() throws KeyStoreException, KeyManagementException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
        KeyManager[] keyManagers = null; 	 // --Defaults to No Client Authentication Certificates Provided
        // TODO: FOR TEST SERVER ONLY, DO NOT USE UnsecureTrustManager IN PRODUCTION SERVER
        TrustManager[] trustManagers = {new UnsecureTrustManager()};   // --Defaults to the built-in AndroidCAStore
        /**  ---------------- Custom Server Certificates  ---------------- **/
        /**
         * Since we are using a Custom PKI we need to add the Certificates as being trusted by the application:
         * 		using  A) a BKS file
         * 		   or  B) a PEM file
         */

        /**
         *  A) Read Trusted Certificates from BKS file
         *
         */

        //KeyStore trustStore = KeyStore.getInstance("BKS");
        //InputStream trustStoreStream = activity.openFileInput("SSL_Server_chain.bks");
        //trustStore.load(trustStoreStream, null);


        /**
         * B) Read Trusted Certificates from PEM file
         * Note: the PEM files should not contain any text (use: openssl ca -notext ...)
         */
		/*
		trustStore = KeyStore.getInstance("BKS");
		trustStore.load(null,null);
		final BufferedInputStream bis = new BufferedInputStream(
						activity.openFileInput("SSL_Server_chain.crt"));
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		while (bis.available()>0){
			Certificate cert = cf.generateCertificate(bis);
			SslCertificate sslCert = new SslCertificate((X509Certificate)cert);
			trustStore.setCertificateEntry(sslCert.getIssuedTo().getCName(), cert);
		}
		*/
        /**
         * Add TrustStore To the TrustManager:
         */

        //final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        //tmf.init(trustStore);
        //trustManagers = tmf.getTrustManagers();

        /**  ---------------- Client Certificates  ---------------- **/
        /**
         * Load Client Certificate
         */
        /**
         * When using the built-in KeyChain:
         */

        if(this.context instanceof Activity) {
            keyManagers = new KeyManager[]{new KeyChainManager((Activity) this.context)};
        }

        /**
         * When using a custom KeyStore from a file:
         */

        //KeyStore clientStore = KeyStore.getInstance("PKCS12");
        //final InputStream keyStoreLocation = activity.openFileInput("SSL_Client_B.p12");
        //clientStore.load(keyStoreLocation, "user".toCharArray());

        //final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        //kmf.init(clientStore, "user".toCharArray());

        //keyManagers = kmf.getKeyManagers();

        /**  ---------------- Insert into SSLContext ---------------- **/

        final SSLContext sslCtx = SSLContext.getInstance("TLS");
        sslCtx.init(keyManagers, trustManagers, new SecureRandom());
        return new DestinationSSLSocketFactory(sslCtx);
    }


    public interface AuthCallback
    {
        void authFailed(final String errorMessage, Throwable error);

        void accessGranted();
    }
}

