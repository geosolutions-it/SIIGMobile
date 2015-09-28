package it.geosolutions.android.siigmobile.spatialite;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

import it.geosolutions.android.siigmobile.elaboration.ElaborationResult;
import jsqlite.Database;

/**
 * Created by Robert Oehler on 09.07.15.
 *
 * An Async task which loads the results of wps calls from the database
 *
 * Literally it queries the names table
 *
 *
 */
public abstract class LoadResultsTask extends AsyncTask<Context,Void,List<ElaborationResult>>
{
    public abstract void done(List<ElaborationResult> results);
    public abstract void started();


    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        started();
    }

    @Override
    protected List<ElaborationResult> doInBackground(Context... params) {

        List<ElaborationResult> results = null;

        final Database db = SpatialiteUtils.openSpatialiteDB(params[0]);

        if (db != null) {

            results = SpatialiteUtils.getSavedResults(db);

            try {
                db.close();
            } catch (jsqlite.Exception e) {
                Log.e(LoadResultsTask.class.getSimpleName(), "exception closing db", e);
            }
        }
        return results;
    }

    @Override
    protected void onPostExecute(List<ElaborationResult> results) {
        super.onPostExecute(results);

        done(results);
    }
}
