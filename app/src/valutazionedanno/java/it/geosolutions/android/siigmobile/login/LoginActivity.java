package it.geosolutions.android.siigmobile.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import it.geosolutions.android.siigmobile.Config;
import it.geosolutions.android.siigmobile.MainActivity;
import it.geosolutions.android.siigmobile.R;
import it.geosolutions.android.siigmobile.login.fragments.AutoLoginFragment;
import it.geosolutions.android.siigmobile.login.fragments.IdentityProviderFragment;
import it.geosolutions.android.siigmobile.login.fragments.LoginFragment;
import it.geosolutions.android.siigmobile.login.fragments.LogoutFragment;

/**
 * Created by Robert Oehler on 31.10.15.
 *
 * The LoginActivity manages the different states of Login
 *
 * These are
 *
 * IDP Selection - select an idp provider
 * LOGIN - enter credentials and start a login request
 * AUTO_LOGIN - use saved credentials/cookie to login automatically
 * LOGOUT - show username which is used for login and give the possibility to logout
 *
 * These are handled in according fragments
 *
 */
public class LoginActivity extends AppCompatActivity {

    public final static String IDP_FRAGMENT_TAG = "IDP_FRAGMENT_TAG";
    public final static String LOGIN_FRAGMENT_TAG = "LOGIN_FRAGMENT_TAG";
    public final static String LOGOUT_FRAGMENT_TAG = "LOGOUT_FRAGMENT_TAG";
    public final static String AUTO_LOGIN_FRAGMENT_TAG = "AUTO_LOGIN_FRAGMENT_TAG";

    public final static String PARAM_LOGOUT    = "PARAM_LOGOUT";

    public final static String PREFS_USER   = "LOGIN_PREFS_USER";
    public final static String PREFS_PASS   = "LOGIN_PREFS_PASS";
    public final static String PREFS_IDP    = "LOGIN_PREFS_IDP";

    private CurrentState mCurrentState;

    //fragments
    private IdentityProviderFragment identityProviderFragment;
    private LoginFragment loginFragment;
    private LogoutFragment logoutFragment;
    private AutoLoginFragment autoLoginFragment;

    private boolean didLogout = false;

    private enum CurrentState
    {
        IDP_SELECTION,
        LOGIN,
        LOGOUT,
        AUTO_LOGIN
    }

    public boolean autoselectPiemonteAsIDP = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        final boolean hasCredentials = prefs.getString(PREFS_USER, null) != null && prefs.getString(PREFS_PASS, null) != null;

        //this activity may be launched from MainActivity to show the login state - in this case the intent contains this flag
        boolean showLogout = false;
        if(getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey(PARAM_LOGOUT)){
            showLogout = getIntent().getExtras().getBoolean(PARAM_LOGOUT);
        }

        //inflate the main frame
        setContentView(R.layout.login_layout);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.m_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mCurrentState == CurrentState.LOGIN) {

                    //back to idp mode
                    backToIDPSelection();
                }
            }
        });

        if(showLogout){
            mCurrentState = CurrentState.LOGOUT;

            Bundle bundle = new Bundle();
            bundle.putInt(PREFS_IDP, prefs.getInt(PREFS_IDP, LoginFragment.IdentityProvider.PIEMONTE.ordinal()));
            bundle.putString(PREFS_USER, prefs.getString(PREFS_USER, null));
            getLogoutFragment().setArguments(bundle);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.login_fragment_container, getLogoutFragment(), LOGOUT_FRAGMENT_TAG)
                    .commit();

        }else { //login, select a fragment to show

            if (hasCredentials) { // try auto login

                mCurrentState = CurrentState.AUTO_LOGIN;

                //show auto login fragment
                Bundle bundle = new Bundle();
                bundle.putInt(PREFS_IDP,       prefs.getInt(PREFS_IDP, LoginFragment.IdentityProvider.PIEMONTE.ordinal()));
                bundle.putString(PREFS_USER,   prefs.getString(PREFS_USER, null));
                bundle.putString(PREFS_PASS,   prefs.getString(PREFS_PASS, null));
                bundle.putString(Config.PREFS_SHIBB_COOKIE, prefs.getString(Config.PREFS_SHIBB_COOKIE, null));
                getAutoLoginFragment().setArguments(bundle);

                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.login_fragment_container, getAutoLoginFragment(), AUTO_LOGIN_FRAGMENT_TAG)
                        .commit();


            } else { //manual login

                //initial login state -> idp selection
                mCurrentState = CurrentState.IDP_SELECTION;

                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.login_fragment_container, getIdentityProviderFragment(), IDP_FRAGMENT_TAG)
                        .commit();



                //TODO remove when idp selection is active
                if(autoselectPiemonteAsIDP) {

                    autoSelectPiemonteAsIDP();
                }
            }
        }

    }
    //TODO remove when idp selection is active
    public void autoSelectPiemonteAsIDP(){

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if(getLoginFragment().getArguments() != null){
                            getLoginFragment().getArguments().putInt(PREFS_IDP, LoginFragment.IdentityProvider.PIEMONTE.ordinal());
                        }else {
                            Bundle bundle = new Bundle();
                            bundle.putInt(PREFS_IDP, LoginFragment.IdentityProvider.PIEMONTE.ordinal());
                            getLoginFragment().setArguments(bundle);
                        }

                        getSupportFragmentManager()
                                .beginTransaction()
                                        // .setCustomAnimations(R.anim.in_from_right, R.anim.out_to_left)
                                .replace(R.id.login_fragment_container, getLoginFragment(), LOGIN_FRAGMENT_TAG)
                                .commit();

                        mCurrentState = CurrentState.LOGIN;
                    }
                });
            }
        }, 30);

    }

    /**
     * the user selected an identity provider
     * this is connected via the onclick property of the layouts xml
     * @param view the clicked view
     */
    public void idpSelected(final View view) {

        LoginFragment.IdentityProvider provider;

        switch (view.getId()) {
            case R.id.bolzano_image:
                provider = LoginFragment.IdentityProvider.BOLZANO;
                break;
            case R.id.piemonte_image:
                provider = LoginFragment.IdentityProvider.PIEMONTE;
                break;
            case R.id.lombardia_image:
                provider = LoginFragment.IdentityProvider.LOMBARDIA;
                break;
            case R.id.val_aosta_image:
                provider = LoginFragment.IdentityProvider.AOSTA;
                break;
            default:
                provider = LoginFragment.IdentityProvider.BOLZANO;
                break;
        }


        Bundle bundle = new Bundle();
        bundle.putInt(PREFS_IDP, provider.ordinal());
        getLoginFragment().setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.in_from_right,R.anim.out_to_left)
                .replace(R.id.login_fragment_container, getLoginFragment(), LOGIN_FRAGMENT_TAG)
                .commit();

        mCurrentState = CurrentState.LOGIN;

        toggleBackButton(true);
    }

    /**
     * switches the mode of this activity back to identity provider selection
     */
    public void backToIDPSelection(){

        //TODO  remove when idp selection is active
        if (autoselectPiemonteAsIDP) {
            finish();
        } else {

            mCurrentState = CurrentState.IDP_SELECTION;

            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.in_from_left, R.anim.out_to_right)
                    .replace(R.id.login_fragment_container, getIdentityProviderFragment(), IDP_FRAGMENT_TAG)
                    .commit();

            toggleBackButton(false);
        }
    }

    /**
     * shows / hides the back button in the toolbar
     * @param show true to show, false to hide
     */
    public void toggleBackButton(final boolean show){
        if(getSupportActionBar() != null) {

            getSupportActionBar().setDisplayHomeAsUpEnabled(show);
            getSupportActionBar().setDisplayShowHomeEnabled(show);
        }
    }

    public void logout(){

        Toast.makeText(getBaseContext(),getString(R.string.logout_logged_out),Toast.LENGTH_SHORT).show();

        //clear credentials
        final SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();

        ed.putString(PREFS_USER, null);
        ed.putString(PREFS_PASS, null);
        ed.putString(Config.PREFS_SHIBB_COOKIE, null);
        ed.putInt(PREFS_IDP, -1);

        ed.apply();

        didLogout = true;

        //TODO remove
        if(autoselectPiemonteAsIDP){
            autoSelectPiemonteAsIDP();
        }else{
            backToIDPSelection();
        }

    }

    @Override
    public void onBackPressed() {

        if(mCurrentState == CurrentState.LOGIN){

            backToIDPSelection();

        } else {

            if(mCurrentState == CurrentState.LOGOUT && !didLogout){
                //user was in logout screen but did not log out
                //we cleared the back stack to be able to logout
                //hence now go "back"
                finish();
                startActivity(new Intent(this, MainActivity.class));
            }else {

                super.onBackPressed();
            }
        }
    }

    public Drawable getDrawableForIdp(final LoginFragment.IdentityProvider idp){

        if(idp != null){

            switch (idp){
                case AOSTA:
                    return ContextCompat.getDrawable(getBaseContext(), R.drawable.vallee_d_aoste_header);
                case BOLZANO:
                    return ContextCompat.getDrawable(getBaseContext(), R.drawable.bolzano_header);
                case LOMBARDIA:
                    return ContextCompat.getDrawable(getBaseContext(), R.drawable.lombardia_header);
                case PIEMONTE:
                    return ContextCompat.getDrawable(getBaseContext(), R.drawable.piemonte_header);
            }

        }
        return null;
    }


    public IdentityProviderFragment getIdentityProviderFragment() {

        if(identityProviderFragment == null){
            identityProviderFragment = IdentityProviderFragment.getInstance();
        }
        return identityProviderFragment;
    }

    public LoginFragment getLoginFragment() {

        if(loginFragment == null){
            loginFragment = LoginFragment.getInstance();
        }
        return loginFragment;
    }

    public LogoutFragment getLogoutFragment() {

        if(logoutFragment == null){
            logoutFragment = LogoutFragment.getInstance();
        }
        return logoutFragment;
    }

    public AutoLoginFragment getAutoLoginFragment() {

        if(autoLoginFragment == null){
            autoLoginFragment = AutoLoginFragment.getInstance();
        }
        return autoLoginFragment;
    }
}
