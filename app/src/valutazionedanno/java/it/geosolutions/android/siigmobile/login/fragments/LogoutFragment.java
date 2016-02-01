package it.geosolutions.android.siigmobile.login.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import it.geosolutions.android.siigmobile.R;
import it.geosolutions.android.siigmobile.login.LoginActivity;

/**
 * Created by Robert Oehler on 01.11.15.
 *
 * Shows the identity provider icon the user used to auth
 * and the username which was used to login
 *
 * The user can :
 *
 *      1. go "back" to return to the MainActivity (handled in LoginActivity's onbackPressed)
 *      2. Logout which results in showing the LoginFragment
 *
 */
public class LogoutFragment extends Fragment {

    private static LogoutFragment mInstance;

    public static LogoutFragment getInstance(){
        if(mInstance == null){
            mInstance = new LogoutFragment();
        }
        return mInstance;
    }

    private LoginFragment.IdentityProvider mIdentityProvider;
    private String mUsername;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null && getArguments().containsKey(LoginActivity.PREFS_IDP)){
            int idp = getArguments().getInt(LoginActivity.PREFS_IDP);
            if(idp > 0 && idp < LoginFragment.IdentityProvider.values().length) {
                mIdentityProvider = LoginFragment.IdentityProvider.values()[idp];
            }else{
                mIdentityProvider = LoginFragment.IdentityProvider.values()[LoginFragment.IdentityProvider.PIEMONTE.ordinal()];
            }
        }

        if(getArguments() != null && getArguments().containsKey(LoginActivity.PREFS_USER)){

            mUsername = getArguments().getString(LoginActivity.PREFS_USER);
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.login_fragment_logout,container,false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView idp_iv = (ImageView) view.findViewById(R.id.idp_iv);

        if(mIdentityProvider != null){

            idp_iv.setImageDrawable(((LoginActivity)getActivity()).getDrawableForIdp(mIdentityProvider));
        }

        if(mUsername != null){

            final TextView userNameTV = (TextView) view.findViewById(R.id.logout_email_tv);
            userNameTV.setText(mUsername);
        }

        final Button logoutButton = (Button) view.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog.Builder builder = new AlertDialog.Builder((getActivity()));
                builder.setIcon(R.mipmap.ic_launcher)
                        .setTitle(getString(R.string.app_name))
                        .setMessage(getString(R.string.logout_really))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();

                                ((LoginActivity) getActivity()).logout();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                final AlertDialog alert = builder.create();
                alert.show();



            }
        });
    }
}
