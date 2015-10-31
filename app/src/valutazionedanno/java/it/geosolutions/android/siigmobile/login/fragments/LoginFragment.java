package it.geosolutions.android.siigmobile.login.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import it.geosolutions.android.siigmobile.Config;
import it.geosolutions.android.siigmobile.MainActivity;
import it.geosolutions.android.siigmobile.R;
import it.geosolutions.android.siigmobile.Util;
import it.geosolutions.android.siigmobile.login.LoginActivity;
import it.geosolutions.android.siigmobile.login.auth.AuthTask;
import it.geosolutions.android.siigmobile.login.auth.ShibAuthResult;

/**
 * Created by Robert Oehler on 31.10.15.
 *
 * Fragment to handle login requests with inserted credentials
 *
 * Having selected an IDP the user is asked to enter credentials
 * These are checked before a login request starts
 * If valid and network is available the auth process starts resulting in
 *
 *  1. Failure - an error message is reported via Toast
 *  2. Success - the credentials, idp and cookie are persisted to preferences
 *     the LoginActivity is finished and Main started
 *
 */
public class LoginFragment extends Fragment {

    private static LoginFragment mInstance;

    public static LoginFragment getInstance(){
        if(mInstance == null){
            mInstance = new LoginFragment();
        }
        return mInstance;
    }


    public enum IdentityProvider
    {
        AOSTA,
        BOLZANO,
        LOMBARDIA,
        PIEMONTE
    }

    private IdentityProvider mIdentityProvider;

    private EditText mEmailView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;

    private AuthTask mAuthTask;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null && getArguments().containsKey(LoginActivity.PREFS_IDP)){
            int idp = getArguments().getInt(LoginActivity.PREFS_IDP);
            mIdentityProvider = IdentityProvider.values()[idp];
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.login_fragment_login,container,false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        mLoginFormView = view.findViewById(R.id.login_form);
        mLoginStatusView = view.findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) view.findViewById(R.id.login_status_message);


        ImageView idp_iv = (ImageView) view.findViewById(R.id.region_image);

        if(mIdentityProvider != null){

            idp_iv.setImageDrawable(((LoginActivity)getActivity()).getDrawableForIdp(mIdentityProvider));

        }

        final Button cancelButton = (Button) view.findViewById(R.id.login_cancel);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((LoginActivity) getActivity()).backToIDPSelection();
            }
        });
        //TODO remove
        if(((LoginActivity)getActivity()).autoselectPiemonteAsIDP){
            cancelButton.setVisibility(View.GONE);
        }

        final Button loginButton  = (Button) view.findViewById(R.id.login_confirm);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //hide keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService( Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);
                attemptLogin();
            }
        });

        Log.i(getClass().getSimpleName(), "onViewCreated " + mIdentityProvider.toString());

        mEmailView = (EditText) view.findViewById(R.id.email);
        //TODO remove
        mEmailView.setText("myself");

        mPasswordView = (EditText) view.findViewById(R.id.password);
        //TODO remove
        mPasswordView.setText("myself");
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String user = mEmailView.getText().toString();
        final String pass = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(pass)) {
            mPasswordView.setError(getString(R.string.login_error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (pass.length() < 4) {
            mPasswordView.setError(getString(R.string.login_error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(user)) {
            mEmailView.setError(getString(R.string.login_error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        //check if online
        if(!Util.isOnline(getActivity().getBaseContext())){

            Toast.makeText(getActivity().getBaseContext(), getString(R.string.login_not_online), Toast.LENGTH_LONG).show();
            cancel = true;
        }


        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            if(focusView != null){
                focusView.requestFocus();
            }
        } else {
            // Show progress spinner
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);

            //kick off a background task to perform the user login attempt.
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mAuthTask = new AuthTask(getActivity().getBaseContext(), mIdentityProvider, Config.SP_CONTENT_ENDPOINT){

                @Override
                public void done(final ShibAuthResult result) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mAuthTask = null;

                            showProgress(false);

                            if (result.isSuccess()) {

                                Toast.makeText(getActivity().getBaseContext(), getString(R.string.login_success), Toast.LENGTH_LONG).show();

                                final SharedPreferences.Editor ed = prefs.edit();

                                ed.putString(LoginActivity.PREFS_USER, user);
                                ed.putString(LoginActivity.PREFS_PASS, pass);
                                if (result.hasCookie()) {
                                    ed.putString(Config.PREFS_SHIBB_COOKIE, result.getCookie());
                                }
                                ed.putInt(LoginActivity.PREFS_IDP, mIdentityProvider.ordinal());

                                ed.apply();

                                getActivity().finish();
                                startActivity(new Intent(getActivity(), MainActivity.class));

                            } else {
                                //error
                                if (!result.hasError()) {
                                    //no exception, most likely wrong password
                                    mPasswordView.setError(getString(R.string.login_error_incorrect_password));
                                    mPasswordView.requestFocus();
                                } else {

                                    //error message available, show
                                    Toast.makeText(getActivity().getBaseContext(), getString(R.string.login_error_generic) + " : " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
                                }
                            }

                        }
                    });


                }
            };
            //TODO remove on production and use the endpoint according to the selected idp ->authtask.getEndPointAccordingToIdentityProvider()
            mAuthTask.setIdpEndPoint(Config.TEST_IDP_ENDPOINT);
            mAuthTask.setCookie(prefs.getString(Config.PREFS_SHIBB_COOKIE, null));
            mAuthTask.execute(user, pass);
        }
    }


    /**
     * Shows the progress UI and hides the login form.
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
