package it.geosolutions.android.siigmobile.login.auth;

/**
 * Created by Robert Oehler on 12.11.15.
 *
 */
public class ShibAuthResult {

    private boolean success;
    private String cookie;
    private String errorMessage;

    public static ShibAuthResult successViaCookie(){

        return new ShibAuthResult(true,null,null);

    }
    public static ShibAuthResult successViaCredentials(final String cookie){

        return new ShibAuthResult(true, cookie, null);

    }
    public static ShibAuthResult failure(final String errorMessage){

        return new ShibAuthResult(false, null, errorMessage);

    }

    public ShibAuthResult(final boolean pSuccess, final String pCookie, final String pError){

        this.success = pSuccess;
        this.cookie  = pCookie;
        this.errorMessage = pError;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean hasCookie(){
        return this.cookie != null;
    }

    public boolean hasError(){
        return this.errorMessage != null;
    }

    public String getCookie() {
        return cookie;
    }

    public String getErrorMessage() {
        return errorMessage;
    }


}
