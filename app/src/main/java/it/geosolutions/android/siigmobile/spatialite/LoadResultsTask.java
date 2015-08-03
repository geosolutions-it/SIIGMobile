package it.geosolutions.android.siigmobile.spatialite;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.geosolutions.android.siigmobile.elaboration.ElaborationResult;
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


        List<ElaborationResult> results = new ArrayList<>();

        final Database db = SpatialiteUtils.openSpatialiteDB(params[0]);

        if (db != null) {

            HashMap<String,ArrayList<String[]>> namePropsMap = SpatialiteUtils.getUnifiedSavedResultTableNames(db);
            if (namePropsMap != null) {
                for(Map.Entry<String,ArrayList<String[]>> entry : namePropsMap.entrySet()){
                    ArrayList<String[]> entries = entry.getValue();
                    if(entries.size() == 1){
                        // This is added for backward compatibility
                        String[] tableRecord = entries.get(0);

                        if(tableRecord[3].equals("1")){//table 0 is pis
                            results.add(new ElaborationResult(tableRecord[1],tableRecord[2],null,tableRecord[0]));
                        }else{
                            results.add(new ElaborationResult(tableRecord[1],tableRecord[2],tableRecord[0],null));
                        }

                    }else if(entries.size() == 2){
                        String[] table0 = entries.get(0);
                        String[] table1 = entries.get(1);

                        String streetTableName;
                        String riskTableName;

                        if(table0[3].equals("1")){//table 0 is pis
                            streetTableName = table0[0];
                            riskTableName   = table1[0];
                        }else{
                            riskTableName   = table0[0];
                            streetTableName  = table1[0];
                        }

                        results.add(new ElaborationResult(table0[1],table0[2],riskTableName,streetTableName));

                    }else{
                        Log.w(LoadResultsTask.class.getSimpleName(), "unexpected arraylist size");
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
    protected void onPostExecute(List<ElaborationResult> results) {
        super.onPostExecute(results);

        done(results);
    }
}
