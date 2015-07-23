package it.geosolutions.android.siigmobile.geocoding;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Address;
import android.os.AsyncTask;
import android.provider.BaseColumns;

import org.mapsforge.core.model.BoundingBox;

import java.util.List;
import java.util.Locale;

import it.geosolutions.android.siigmobile.Config;

/**
 * Created by Robert Oehler on 09.07.15.
 *
 *
 * This async task may often be cancelled as other Geocoding requests start
 * when the user is typing in the search view
 *
 * According to http://stackoverflow.com/questions/2735102/ideal-way-to-cancel-an-executing-asynctask
 * Asyntasks isCancelled() method is only invoked after doInBackground on the UI thread
 * and hence rather senseless
 *
 * Instead a local flag is used
 *
 */
public abstract class GeoCodingTask extends AsyncTask<Void,Void,Cursor> {

    private final static String TAG = GeoCodingTask.class.getSimpleName();

    public abstract void done(Cursor cursor);

    private volatile boolean cancelled = false;
    private IGeoCoder mGeoCoder;
    private String mQuery;
    private BoundingBox mBB;

    public GeoCodingTask(final IGeoCoder geoCoder, final String query, final BoundingBox bb){

        this.mGeoCoder = geoCoder;
        this.mQuery    = query;
        this.mBB       = bb;
    }

    @Override
    protected Cursor doInBackground(Void... params) {


        final List<Address> addresses = mGeoCoder.getFromLocationName(mQuery, mBB, Config.SUGGESTIONS_THRESHOLD, Locale.getDefault());

        if (!cancelled && addresses != null && addresses.size() > 0) {
            //Searchview supports only CursorAdapter
            //hence for each request a new cursor needs to be set up
            final MatrixCursor c = new MatrixCursor(new String[]{BaseColumns._ID, Config.CURSOR_TITLE, Config.CURSOR_LAT, Config.CURSOR_LON});
            final int count = addresses.size();

            for (int i = 0; i < count; i++) {
                if (addresses.get(i) != null) {
                    Address address = addresses.get(i);

                    if (address != null && address.getFeatureName() != null) {

                        String temp = address.getFeatureName();
                        if (address.getAdminArea() != null && (!address.getAdminArea().equals("null"))) {

                            temp += " " + address.getAdminArea();
                        }
                        String lat = Double.toString(address.getLatitude());
                        String lon = Double.toString(address.getLongitude());
                        c.addRow(new Object[]{i, temp, lat, lon});
                        //Log.i(TAG, "adding to cursor " + temp);
                    }
                }
            }
            return c;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Cursor cursor) {
        super.onPostExecute(cursor);

        if (!cancelled && cursor != null) {

            done(cursor);

        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        cancelled = true;
    }

    public String getQuery() {

        return mQuery;
    }
}
