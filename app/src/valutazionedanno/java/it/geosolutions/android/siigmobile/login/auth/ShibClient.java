package it.geosolutions.android.siigmobile.login.auth;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;

import it.geosolutions.android.siigmobile.Config;
import it.geosolutions.android.siigmobile.R;

/**
 * Created by Robert Oehler on 10.11.15.
 *
 * Class which handles the authentication flow of Shibboleth
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
@SuppressWarnings("deprecation")
public  class ShibClient {

    private DefaultHttpClient httpClient;

    private final static String SAML_REQUEST = "SAMLRequest";
    private final static String RELAY_STATE = "RelayState";

    private Context context;

    private String cookie;

    public ShibClient(final Context context) throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {

        this.context = context;
        this.httpClient = new DefaultHttpClient();
    }

    public void authenticate(final String spEndPoint, final String idpEndPoint, final String user, final String pass,@NonNull final AuthCallback callback) throws IOException{

        //1. request content at SP
        final ShibClient.ContentRequestResponse contentRequest = requestContent(spEndPoint, true);

        if(contentRequest instanceof ShibClient.AccessViaCookieResponse){
            //we have access using the cookie
            callback.accessViaCookie();
            return;
        }

        if(contentRequest instanceof ShibClient.ErrorResponse || contentRequest.getStatus() != 302){
            //unexpected state
            callback.authFailed(context.getString(R.string.login_error_unexpected_status)
                    + contentRequest.getStatus());
            return;
        }
        if(!(contentRequest instanceof ShibClient.NeedsAuthResponse) || ((ShibClient.NeedsAuthResponse)contentRequest).getRedirectUrl() == null){
            //cannot request auth without redirect url
            callback.authFailed(context.getString(R.string.login_error_noredirect));
            return;
        }

        //2.we are not authorized - request auth form
        final String idpAuthPath = requestAuthForm(((ShibClient.NeedsAuthResponse) contentRequest).getRedirectUrl());

        //3. having the idp action path we can perform the auth
        final String authResponse = performAuth(idpEndPoint + idpAuthPath, user, pass);

        //4. parse the response
        Document authResponseDoc = Jsoup.parse(authResponse);
        Element authResponseFormElement = authResponseDoc.select("form").get(0);
        Element relayStateElement = authResponseDoc.select("input").get(0);
        Element SAMLResponseElement = authResponseDoc.select("input").get(1);

        String action = null, relayStateValue = null, SAMLResponseValue = null;
        if(authResponseFormElement != null){
            action = authResponseFormElement.attr("action");
        }
        if(relayStateElement != null){
            relayStateValue = relayStateElement.attr("value");
        }
        if(SAMLResponseElement != null){
            SAMLResponseValue = SAMLResponseElement.attr("value");
        }

        if(relayStateValue == null || TextUtils.isEmpty(relayStateValue) ||
                SAMLResponseValue == null || TextUtils.isEmpty(SAMLResponseValue)){
            //4a.
            callback.authFailed(context.getString(R.string.login_error_credentials));

        }else{

            //4b. we have relaystate and SAMLResponseValue -> we're logged send them to the SP
            //5. consume assertion
            consumeAssertion(action, relayStateValue, SAMLResponseValue);

            //6.now we should be able to access the content
            final ShibClient.ContentRequestResponse contentRequest2 = requestContent(spEndPoint, false);

            if(contentRequest2 instanceof ShibClient.AccessViaCredentialsResponse){
                //now we have access, grant it
                callback.accessViaCredentials(((ShibClient.AccessViaCredentialsResponse) contentRequest2).getCookie());
            }else{
                //still no access
                callback.authFailed(context.getString(R.string.login_error_assertion));
            }
        }

    }

    /**
     * requests the content and parses the response
     * according to the Http respnce code a ContentRequestResponse is returned
     * 200 -> auth via crendentials
     * 302 -> redirect needs auth
     * 500 -> auth via cookie this is likely specific for testshib, test on prduction server
     * all others are unexpected and ErrorRespone is returned
     *
     * @param spEndPoint the endpoint to request the content
     * @param useCookieIfAvailable to use the cookie if it is avaialble,
     * @return an instance of ContentRequestResponse
     * @throws IOException
     */
    private ContentRequestResponse requestContent(final String spEndPoint, final boolean useCookieIfAvailable)throws IOException{

        HttpGet httpget = new HttpGet(spEndPoint);
        //don't allow redirecting
        BasicHttpParams params = new BasicHttpParams();
        params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        httpget.setParams(params);

        if(useCookieIfAvailable && cookie != null){
            httpget.setHeader(Config.SHIBB_COOKIE_NAME,cookie);
        }

        HttpResponse response = httpClient.execute(httpget, new BasicHttpContext());
        String responseString = readResponse(response.getEntity().getContent()).toString();
        Document responseDoc = Jsoup.parse(responseString);
        int statusCode = response.getStatusLine().getStatusCode();

        if(statusCode == 302) {
            //we are not authorized -> wants to redirect us to idp, parse
            Element link = responseDoc.select("a").first();
            String href = link.attr("href");

            Uri uri = Uri.parse(href);
            String relay = uri.getQueryParameter(RELAY_STATE);
            String saml_request  = uri.getQueryParameter(SAML_REQUEST);

            return new NeedsAuthResponse(statusCode,href,relay,saml_request);

        }else if(statusCode == 200){

            //TODO this cookie parsing is specific for the testshib server, review for production server
            Elements ul = responseDoc.select("ul");
            Elements lis = ul.first().select("li");

            String _cookie = null;
            for (Element element : lis) {
                if(element.text().startsWith(Config.SHIBB_COOKIE_NAME)){
                    String wrapped = element.html();
                    String tail = wrapped.substring(wrapped.indexOf("<b>"));
                    _cookie = tail.replace("<b>","").replace("</b>","");
                    break;
                }
            }

            return new AccessViaCredentialsResponse(statusCode,_cookie);
        }else if(statusCode ==  500){
            //TODO check production server what status is returned for cookie requests
            //access via cookie
            return new AccessViaCookieResponse(statusCode);

        }else{
            //unexpected status
            return new ErrorResponse(statusCode);
        }

    }

    private String requestAuthForm(final String authFormUrl)  throws IOException{

        HttpPost httpPost = new HttpPost(authFormUrl);
        HttpContext context = new BasicHttpContext();
        HttpResponse response = httpClient.execute(httpPost, context);
        String responseString = readResponse(response.getEntity().getContent()).toString();

        Document idpDoc = Jsoup.parse(responseString);
        Element idpFormElement = idpDoc.select("form").get(0);
        return idpFormElement.attr("action");

    }

    private String performAuth(final String actionPath, final String user, final String pass) throws IOException{

        HttpPost httpPost = new HttpPost(actionPath);
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("j_username", user));
        nameValuePairs.add(new BasicNameValuePair("j_password", pass));
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
        HttpResponse response2 = httpClient.execute(httpPost);
        return readResponse(response2.getEntity().getContent()).toString();
    }

    private String consumeAssertion(final String actionUrl,final String relayState,final String samlresponse) throws IOException{

        HttpPost httpPost = new HttpPost(actionUrl);
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("RelayState", relayState));
        nameValuePairs.add(new BasicNameValuePair("SAMLResponse", samlresponse));
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
        HttpResponse response = httpClient.execute(httpPost);
        return readResponse(response.getEntity().getContent()).toString();
    }


    private StringBuilder readResponse(InputStream is) throws IOException {
        String line;
        StringBuilder total = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        while ((line = reader.readLine()) != null) {
            total.append(line);
        }
        return total;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }


    public class ContentRequestResponse
    {
        private int status;

        public ContentRequestResponse(final int pStatus){
            this.status = pStatus;
        }

        public int getStatus() {
            return status;
        }

    }

    public class ErrorResponse extends  ContentRequestResponse
    {
        public ErrorResponse(int pStatus){
            super(pStatus);
        }
    }

    public class AccessViaCookieResponse extends ContentRequestResponse{

        public AccessViaCookieResponse(int pStatus){
            super(pStatus);
        }
    }

    public class AccessViaCredentialsResponse extends ContentRequestResponse{

        private String cookie;

        public AccessViaCredentialsResponse(final int pStatus, final String pCookie){
            super(pStatus);
            this.cookie = pCookie;
        }

        public String getCookie() {
            return cookie;
        }
    }

    public class NeedsAuthResponse extends  ContentRequestResponse{

        private String redirectUrl;
        private String relayState;
        private String responseValue;

        public NeedsAuthResponse(final int pStatus,final String pUrl,final String pRelayState, final String pResponseValue){
            super(pStatus);
            this.redirectUrl = pUrl;
            this.relayState = pRelayState;
            this.responseValue = pResponseValue;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public String getRelayState() {
            return relayState;
        }

        public String getResponseValue() {
            return responseValue;
        }
    }

    public interface AuthCallback
    {
        void authFailed(final String errorMessage);

        void accessViaCookie();

        void accessViaCredentials(final String cookie);
    }
}
