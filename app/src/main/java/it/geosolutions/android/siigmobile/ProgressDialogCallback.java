package it.geosolutions.android.siigmobile;

import android.app.ProgressDialog;

import retrofit.Callback;

/**
 * Created by Lorenzo on 20/07/2015.
 */
public abstract class ProgressDialogCallback<T> implements Callback<T> {

    public ProgressDialog pd;
}
