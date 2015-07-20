package it.geosolutions.android.siigmobile.spatialite;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

import jsqlite.Database;

/**
 * Created by Robert Oehler on 09.07.15.
 *
 * Deletes all currently unsafed results in the database
 *
 * "unsaved" means the entry in the names table does not contain a user edited name
 *
 */
public abstract class DeleteUnsafedResultsTask extends AsyncTask<Context,Void,Void> {

    private final static String TAG = DeleteUnsafedResultsTask.class.getSimpleName();

    public abstract void done();
    public abstract void started();

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        started();
    }

    @Override
    protected Void doInBackground(Context... params) {

        final Database db = SpatialiteUtils.openSpatialiteDB(params[0]);

        final ArrayList<String> names = SpatialiteUtils.getUnSavedResultTableNames(db);

        if (names != null) {
            for (String name : names) {

                if (!SpatialiteUtils.deleteResult(db, name)) {
                    Log.w(TAG, "error deleting result " + name);
                }

                if (!SpatialiteUtils.deleteResultFromNamesTable(db, name)) {
                    Log.w(TAG, "error deleting result from names table " + name);
                }
            }
        }
        try {
            db.close();
        } catch (jsqlite.Exception e) {
            Log.e(TAG, "exception closing db", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        done();
    }
}
