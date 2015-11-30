package it.geosolutions.android.siigmobile.login.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import it.geosolutions.android.siigmobile.Config;
import it.geosolutions.android.siigmobile.MainActivity;
import it.geosolutions.android.siigmobile.R;
import it.geosolutions.android.siigmobile.Util;
import it.geosolutions.android.siigmobile.login.LoginActivity;
import it.geosolutions.android.siigmobile.login.AsyncShibbolethClient;

/**
 * Created by Robert Oehler on 11.11.15.
 *
 * Fragment for auto login
 *
 * This assumes valid credentials and an optional cookie and starts a login request
 *
 * In case of success it finishes LoginActivity and starts MainActivity
 * In case of failure it hides the progress view
 * and gives the user the options
 *      1. to retry for the case that he was temporarily offline o other network problems
 *      2. to logout and restart the auth inserting credentials
 *
 */
public class AutoLoginFragment extends Fragment {

    private static AutoLoginFragment mInstance;

    public static AutoLoginFragment getInstance(){
        if(mInstance == null){
            mInstance = new AutoLoginFragment();
        }
        return mInstance;
    }

    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView errorTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.login_fragment_auto,container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        mLoginFormView = view.findViewById(R.id.login_form);
        mLoginStatusView = view.findViewById(R.id.login_status);

        errorTextView = (TextView) view.findViewById(R.id.auto_login_failed_reason);

        if(getArguments() != null && getArguments().containsKey(LoginActivity.PREFS_IDP)) {
            int idp = getArguments().getInt(LoginActivity.PREFS_IDP);
            final LoginFragment.IdentityProvider identityProvider = LoginFragment.IdentityProvider.values()[idp];
            ImageView idp_iv = (ImageView) view.findViewById(R.id.idp_iv);

            if (identityProvider != null) {

                idp_iv.setImageDrawable(((LoginActivity) getActivity()).getDrawableForIdp(identityProvider));
            }
        }

        final Button retryButton = (Button) view.findViewById(R.id.try_again_button);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showProgress(true);
                autoLogin();
            }
        });

        final Button logoutButton = (Button) view.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((LoginActivity) getActivity()).logout();

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        autoLogin();

    }

    public void autoLogin(){

        //check if online
        if(!Util.isOnline(getActivity().getBaseContext())){
            showProgress(false);
            errorTextView.setText(getString(R.string.login_not_online));

        }else { //go

            String user = getArguments().getString(LoginActivity.PREFS_USER);
            String pass = getArguments().getString(LoginActivity.PREFS_PASS);

            if (user == null || pass == null) {
                Log.w("AutoLogin", "user/pass not available, cannot autologin");
                showProgress(false);
                return;
            }


            //TODO use production endpoints
//            int idp = getArguments().getInt(LoginActivity.PREFS_IDP);
//            LoginFragment.IdentityProvider identityProvider = LoginFragment.IdentityProvider.values()[idp];
//            String productionIDP =  getEndPointAccordingToIdentityProvider(identityProvider);

            final AsyncShibbolethClient shibbolethClient = new AsyncShibbolethClient(getActivity(), true);
            shibbolethClient.authenticate(Config.SP_CONTENT_ENDPOINT, Config.TEST_IDP_ENDPOINT, user, pass, new AsyncShibbolethClient.AuthCallback() {
                @Override
                public void authFailed(final String errorMessage, Throwable error) {

                    showProgress(false);

                    Toast.makeText(getActivity().getBaseContext(), getString(R.string.login_error_generic) + " : " + errorMessage, Toast.LENGTH_LONG).show();


                }
                @Override
                public void accessGranted(){


                    Toast.makeText(getActivity().getBaseContext(), getString(R.string.login_success), Toast.LENGTH_LONG).show();

                    getActivity().finish();
                    startActivity(new Intent(getActivity(), MainActivity.class));

                }


            });

        }
    }

    /**
     * Shows /hides the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
