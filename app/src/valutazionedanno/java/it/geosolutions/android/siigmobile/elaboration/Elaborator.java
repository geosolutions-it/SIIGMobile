package it.geosolutions.android.siigmobile.elaboration;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;

import java.util.ArrayList;
import java.util.Random;

import it.geosolutions.android.map.BuildConfig;
import it.geosolutions.android.map.database.SpatialDataSourceManager;
import it.geosolutions.android.map.utils.MapFilesProvider;
import it.geosolutions.android.map.wfs.geojson.feature.Feature;
import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;
import it.geosolutions.android.siigmobile.Config;
import it.geosolutions.android.siigmobile.R;
import it.geosolutions.android.siigmobile.Util;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;
import it.geosolutions.android.siigmobile.wps.SIIGRetrofitClient;
import it.geosolutions.android.siigmobile.wps.ValutazioneDannoWPSRequest;
import jsqlite.Database;

/**
 * Created by Robert Oehler on 27.07.15.
 *
 * Elaborator for a "valutazione danno" elaboration
 * It does one calculation, requesting the data from the server
 * and if the responce contained data persists it
 *
 */
public abstract class Elaborator {

    private final static String TAG = Elaborator.class.getSimpleName();

    public abstract void showError(String errorMessage);

    public abstract void done(ElaborationResult result);

    private Context mContext;
    private boolean cancelled = false;

    public Elaborator(final Context pContext){
        this.mContext = pContext;
    }


    /**
     * starts a calculation based on a BoundingBox
     * @param p the center of the calculation
     * @param title user edited title of the processing
     * @param description user edited description of the processing (may be null)
     */
    public void startCalc(final GeoPoint p,
                          final String title,
                          final String description,
                          final int bufferWidth,
                          final int accidentItem,
                          final int scenarioItem,
                          final int entityItem) {

        if (!Util.isOnline(mContext)) {
            showError(mContext.getString(R.string.snackbar_offline_text));
            done(null);
            return;
        }

        if (p == null) {
            showError(mContext.getString(R.string.snackbar_noparam_text));
            done(null);
            return;
        }

        String accident = mContext.getResources().getStringArray(R.array.accident_ids)[accidentItem];
        String scenario = mContext.getResources().getStringArray(R.array.scenario_ids)[scenarioItem];
        String entity   = mContext.getResources().getStringArray(R.array.entity_ids)[entityItem];

        if (scenario.equals(mContext.getResources().getStringArray(R.array.scenario_ids)[0])) {
            scenario = Util.commaSeparatedListForDangers(mContext);
        }

        if (entity.equals(mContext.getResources().getStringArray(R.array.entity_ids)[0])) {
            entity = Util.commaSeparatedListForEntities(mContext);
        }

        BoundingBox bb = new BoundingBox(p.latitude, p.longitude, p.latitude, p.longitude);
        final BoundingBox extended = Util.extend(bb, bufferWidth);

        Coordinate[] coords = new Coordinate[2];
        coords[0] = new Coordinate(extended.minLongitude, extended.minLatitude);
        coords[1] = new Coordinate(extended.maxLongitude, extended.maxLatitude);
        GeometryFactory fact = new GeometryFactory();
        FeatureCollection features = new FeatureCollection();
        features.features = new ArrayList<>();
        Feature bbox = new Feature();
        bbox.geometry = fact.createLineString(coords);
        features.features.add(bbox);

        final ValutazioneDannoWPSRequest request = new ValutazioneDannoWPSRequest(
                features,
                scenario,
                accident,
                entity);

        String query;
        try {
            query = request.createWPSCallFromText(mContext);

        } catch (IllegalArgumentException iae) {
            Log.e(TAG, "error creating wps call");
            showError(mContext.getString(R.string.snackbar_noparam_text));
            done(null);
            return;
        }

        //everything prepared, show progress, disable controls

        SIIGRetrofitClient.postWPS(query, Config.DESTINATION_AUTHORIZATION, new SIIGRetrofitClient.WPSRequestFeedback() {
            @Override
            public void success(CRSFeatureCollection result) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "result arrived : " + result.toString());
                }

                if(!cancelled) {
                    persistResult(result, new ElaborationResult(title, description, null, p, bufferWidth));
                }
            }

            @Override
            public void error(String errorMessage) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "error : " + errorMessage);
                }
                if(cancelled){
                    return;
                }
                showError(errorMessage);

                done(null);

            }
        });


    }

    /**
     * calculates the radius according to the given params
     * @param scenarioID selected scenarion
     * @param accidentID selected accident
     * @param entityID selected entity
     * @return the radios in meters
     */
    public static int calculateBufferWidth(final int scenarioID, final int accidentID, final int entityID) {

        final int min = 100;
        final int max = 1500;


        //TODO implement

        return new Random().nextInt((max - min) + 1) + min;
    }

    /**
     * persists the result of a valutazione danno calculation
     * @param data the data of the result
     * @param elaborationResult the wrapper containing information about the result
     */
    private void persistResult(CRSFeatureCollection data, final ElaborationResult elaborationResult) {

        //save response asynchronously
        new AsyncTask<CRSFeatureCollection, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(CRSFeatureCollection... param) {

                final Database db = SpatialiteUtils.openSpatialiteDB(mContext);

                final CRSFeatureCollection response = param[0];

                if (response.features.size() > Config.WPS_MAX_FEATURES) {
                    showError(mContext.getString(R.string.result_too_large));
                    return false;
                }

                if (response.features.size() <= 0) {
                    showError(mContext.getString(R.string.result_empty));
                    return false;
                }

                if(cancelled){ //try to cancel before writing
                    try {
                        db.close();
                    } catch (jsqlite.Exception e) {
                        Log.e(TAG, "exception closing db", e);
                    }
                    return false;
                }

                final String geomType = response.features.get(0).geometry.getGeometryType().toUpperCase();

                final Pair<Boolean, String> resultPair = SpatialiteUtils.saveResult(db, response, geomType);

                if(!resultPair.first || resultPair.second == null){
                    showError(mContext.getString(R.string.error_saving, elaborationResult.getUserEditedName()));
                    return false;
                }
                elaborationResult.setResultTableName(resultPair.second);

                //and relate to users name
                boolean nameSaved = SpatialiteUtils.updateNameTableWithElaboration(db, elaborationResult);

                if(!nameSaved){
                    showError(mContext.getString(R.string.error_saving, elaborationResult.getUserEditedName()));
                    return false;
                }

                if(cancelled){//user cancelled while writing the result, delete it
                    if (!SpatialiteUtils.deleteResult(db, elaborationResult.getResultTableName())) {
                        Log.w(TAG, "error deleting result " + elaborationResult.getResultTableName());
                        return false;
                    }


                    if (!SpatialiteUtils.deleteResultFromNamesTable(db, elaborationResult.getResultTableName())) {
                        Log.w(TAG, "error deleting result from names table " + elaborationResult.getResultTableName());
                        return false;
                    }
                }

                try {
                    db.close();
                } catch (jsqlite.Exception e) {
                    Log.e(TAG, "exception closing db", e);
                }

                if(!cancelled) {
                    //reload the spatial data sources to be able to load this new table in the MainActivity
                    SpatialDataSourceManager.getInstance().clear();
                    SpatialDataSourceManager.getInstance().init(MapFilesProvider.getBaseDirectoryFile());
                }
                return true;
            }

            @Override
            protected void onPostExecute(final Boolean success) {
                super.onPostExecute(success);

                if(success){

                    done(elaborationResult);

                }else{

                    done(null);

                }
            }
        }.execute(data);

    }


    public void cancel() {

        this.cancelled = true;
    }
}
