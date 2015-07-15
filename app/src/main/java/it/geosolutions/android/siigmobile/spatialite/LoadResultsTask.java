package it.geosolutions.android.siigmobile.spatialite;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;
import jsqlite.Database;

/**
 * Created by Robert Oehler on 09.07.15.
 *
 * An Async task which loads the results of wps calls from the database
 *
 * Literally it queries the names table
 *
 * If this task is not for deleting unsaved result and the contains a user edited name
 * it is loaded containg its geometry and added to a list of CRSFeatureCollection

 *
 */
public abstract class LoadResultsTask extends AsyncTask<Context,Void,List<CRSFeatureCollection>>
{
    public abstract void done(List<CRSFeatureCollection> results);
    public abstract void started();


    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        started();
    }

    @Override
    protected List<CRSFeatureCollection> doInBackground(Context... params) {

        List<CRSFeatureCollection> results = new ArrayList<>();

        final Database db = SpatialiteUtils.openSpatialiteDB(params[0]);

        if (db != null) {

            ArrayList<String[]> names = SpatialiteUtils.getSavedResultTableNames(db);
            if (names != null) {
                for (String[] array : names) {

                    //if internal and user edited name available, load detailed result
                    if (array[0] != null && array[1] != null) {
                        CRSFeatureCollection result = SpatialiteUtils.loadResult(db, array[0]);
                        if (result != null) {
                            result.userEditedName = array[1];
                            result.userEditedDescription = array[2];
                            result.isPIS = ("1".equals(array[3]));
                            results.add(result);
                        }
                    }
                }
            }
            try {
                db.close();
            } catch (jsqlite.Exception e) {
                Log.e(LoadResultsTask.class.getSimpleName(), "exception closing db", e);
            }
        }
        return results;
    }

    @Override
    protected void onPostExecute(List<CRSFeatureCollection> results) {
        super.onPostExecute(results);

        done(results);
    }
}
