package it.geosolutions.android.siigmobile.login.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import it.geosolutions.android.siigmobile.R;

/**
 * Created by Robert Oehler on 31.10.15.
 *
 * Shows the different identity providers icons to let the user select one
 *
 * The selection callback is in LoginActivity.idpSelected()
 *
 */
public class IdentityProviderFragment extends Fragment {

    private static IdentityProviderFragment mInstance;

    public static IdentityProviderFragment getInstance(){
        if(mInstance == null){
            mInstance = new IdentityProviderFragment();
        }
        return mInstance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.login_fragment_idp,container, false);

    }

}
