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
import java.util.HashMap;
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

    static HashMap <Integer, HashMap <Integer, HashMap<String, Integer[]>>> rdata;

    static HashMap<Integer, Integer> scenarioMapping;

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
     *
     * scenarioID -> 0 = all
     *
     * accidentID -> 0 = all
     *
     * entityID -> 42 = all
     *              0 = lieve
     *              1 = grave
     * @param scenarioID selected scenarion
     * @param accidentID selected accident
     * @param entityID selected entity
     * @return the radios in meters
     */
    public static int calculateBufferWidth(final int scenarioID, final int accidentID, final int entityID) {

        final int min = 100;
        final int max = 1500;

        Log.d(TAG, "scenarioID: " + scenarioID + " accidentID: " + accidentID +" entityID: "+ entityID);
        Integer originalScenarioID = getScenarioMapping().get(scenarioID);

        int radiusMax = 0;

        // Scenario
        for ( Integer scenId : getRData().keySet() ){
            if(scenarioID == 0 || scenId.equals(originalScenarioID) ){
                if(getRData().get(scenId) != null) {

                    //Accident
                    for (Integer accID : getRData().get(scenId).keySet()) {
                        if(accidentID == 0 || accID == accidentID ){
                            if(getRData().get(scenId).get(accID) != null){

                                // Seriousness
                                for (String serTAG : getRData().get(scenId).get(accID).keySet()) {
                                    if( entityID == 42 || serTAG.equals(entityID == 0 ? "L" : "G") ) {
                                        if(getRData().get(scenId).get(accID).get(serTAG) != null) {
                                            //Actual compare

                                            Integer [] hnh = getRData().get(scenId).get(accID).get(serTAG);
                                            // Human
                                            if(radiusMax < hnh[0]){
                                                radiusMax = hnh[0];
                                            }
                                            // Not Human
                                            if(radiusMax < hnh[1]){
                                                radiusMax = hnh[1];
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return radiusMax;
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


    public static HashMap<Integer, Integer> getScenarioMapping(){
        if(scenarioMapping == null || scenarioMapping.isEmpty()){

            scenarioMapping = new HashMap<Integer, Integer>();
            // Fill scenario mapping
            scenarioMapping.put(1,1);
            scenarioMapping.put(2,2);
            scenarioMapping.put(3,3);
            scenarioMapping.put(4,4);
            scenarioMapping.put(5,5);
            scenarioMapping.put(6,6);
            scenarioMapping.put(7,7);
            scenarioMapping.put(8,8);
            scenarioMapping.put(9,9);
            scenarioMapping.put(10,10);
            scenarioMapping.put(11,11);
            scenarioMapping.put(12,12);
            scenarioMapping.put(100,5);
            scenarioMapping.put(101,5);
            scenarioMapping.put(102,5);
            scenarioMapping.put(103,4);
            scenarioMapping.put(104,4);
            scenarioMapping.put(105,10);
            scenarioMapping.put(106,10);
            scenarioMapping.put(107,10);
            scenarioMapping.put(108,10);
            scenarioMapping.put(109,10);
            scenarioMapping.put(110,10);
            scenarioMapping.put(111,10);
            scenarioMapping.put(112,11);
            scenarioMapping.put(113,11);
            scenarioMapping.put(114,11);
            scenarioMapping.put(115,11);
            scenarioMapping.put(116,9);
            scenarioMapping.put(117,10);
            scenarioMapping.put(118,10);
            scenarioMapping.put(119,10);
            scenarioMapping.put(120,2);
            scenarioMapping.put(121,11);
            scenarioMapping.put(122,2);
            scenarioMapping.put(123,2);
            scenarioMapping.put(124,2);
            scenarioMapping.put(125,2);
            scenarioMapping.put(126,2);
            scenarioMapping.put(127,2);
            scenarioMapping.put(128,2);
            scenarioMapping.put(129,2);
            scenarioMapping.put(130,12);
            scenarioMapping.put(131,11);
            scenarioMapping.put(132,12);
            scenarioMapping.put(133,12);
            scenarioMapping.put(134,12);
            scenarioMapping.put(135,12);
            scenarioMapping.put(136,12);
            scenarioMapping.put(137,12);
            scenarioMapping.put(138,12);
            scenarioMapping.put(139,12);
            scenarioMapping.put(140,12);
            scenarioMapping.put(141,12);
            scenarioMapping.put(142,12);
            scenarioMapping.put(143,10);
            scenarioMapping.put(144,9);
            scenarioMapping.put(145,10);
            scenarioMapping.put(146,10);
            scenarioMapping.put(147,10);
            scenarioMapping.put(148,12);
            scenarioMapping.put(150,3);
            scenarioMapping.put(151,8);
            scenarioMapping.put(152,7);
            scenarioMapping.put(153,5);
            scenarioMapping.put(154,2);
            scenarioMapping.put(155,6);
            scenarioMapping.put(156,4);
            scenarioMapping.put(157,9);
            scenarioMapping.put(158,10);
            scenarioMapping.put(159,11);
            scenarioMapping.put(160,12);
            scenarioMapping.put(161,2);
            scenarioMapping.put(162,2);
            scenarioMapping.put(163,12);
            scenarioMapping.put(164,9);
            scenarioMapping.put(165,2);
            scenarioMapping.put(166,12);
            scenarioMapping.put(167,12);
        }

        return scenarioMapping;
    }

    public static  HashMap <Integer,HashMap<Integer, HashMap<String, Integer[]>>> getRData(){
        if(rdata == null){
            rdata = new HashMap <Integer,HashMap<Integer, HashMap<String, Integer[]>>>();

            Integer [] hnh = {0,0};
            HashMap <Integer, HashMap<String, Integer[]>> newacc = new HashMap <Integer, HashMap<String, Integer[]>>();
            HashMap<String, Integer[]> newser = new HashMap <String, Integer[]>();

            //Sost: 1 Acc: 1 Ser:L -> 15 , 10
            hnh[0] = 15 ; hnh[1] = 10 ;
            newser.put("L" , hnh);
            hnh = new Integer[2];
            //Sost: 1 Acc: 1 Ser:G -> 35 , 15
            hnh[0] = 35 ; hnh[1] = 15 ;
            newser.put("G" , hnh);
            newacc.put(1 , newser);
            rdata.put( 1 , newacc );
            newacc = new HashMap <Integer, HashMap <String, Integer[]>>();
            newser = new HashMap <String, Integer[]>();
            //Sost: 2 Acc: 12 Ser:L -> 5 , 5
            hnh = new Integer[2];
            hnh[0] = 5 ; hnh[1] = 5 ;
            newser.put("L" , hnh);
            //Sost: 2 Acc: 12 Ser:G -> 5 , 5
            hnh = new Integer[2];
            hnh[0] = 5 ; hnh[1] = 5 ;
            newser.put("G" , hnh);
            newacc.put(12 , newser);
            rdata.put( 2 , newacc );
            newacc = new HashMap <Integer, HashMap <String, Integer[]>>();
            newser = new HashMap <String, Integer[]>();
            //Sost: 3 Acc: 13 Ser:L -> 5 , 5
            hnh = new Integer[2];
            hnh[0] = 5 ; hnh[1] = 5 ;
            newser.put("L" , hnh);
            //Sost: 3 Acc: 13 Ser:G -> 5 , 5
            hnh = new Integer[2];
            hnh[0] = 5 ; hnh[1] = 5 ;
            newser.put("G" , hnh);
            newacc.put(13 , newser);
            rdata.put( 3 , newacc );
            newacc = new HashMap <Integer, HashMap <String, Integer[]>>();
            newser = new HashMap <String, Integer[]>();
            //Sost: 4 Acc: 2 Ser:L -> 5 , 5
            hnh = new Integer[2];
            hnh[0] = 5 ; hnh[1] = 5 ;
            newser.put("L" , hnh);
            //Sost: 4 Acc: 2 Ser:G -> 5 , 5
            hnh = new Integer[2];
            hnh[0] = 5 ; hnh[1] = 5 ;
            newser.put("G" , hnh);
            newacc.put(2 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 4 Acc: 10 Ser:L -> 100 , 5
            hnh = new Integer[2];
            hnh[0] = 100 ; hnh[1] = 5 ;
            newser.put("L" , hnh);
            //Sost: 4 Acc: 10 Ser:G -> 180 , 10
            hnh = new Integer[2];
            hnh[0] = 180 ; hnh[1] = 10 ;
            newser.put("G" , hnh);
            newacc.put(10 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 4 Acc: 14 Ser:L -> 0 , 10
            hnh = new Integer[2];
            hnh[0] = 0 ; hnh[1] = 10 ;
            newser.put("L" , hnh);
            //Sost: 4 Acc: 14 Ser:G -> 0 , 25
            hnh = new Integer[2];
            hnh[0] = 0 ; hnh[1] = 25 ;
            newser.put("G" , hnh);
            newacc.put(14 , newser);
            rdata.put( 4 , newacc );
            newacc = new HashMap <Integer, HashMap <String, Integer[]>>();
            newser = new HashMap <String, Integer[]>();
            //Sost: 5 Acc: 3 Ser:L -> 60 , 45
            hnh = new Integer[2];
            hnh[0] = 60 ; hnh[1] = 45 ;
            newser.put("L" , hnh);
            //Sost: 5 Acc: 3 Ser:G -> 120 , 100
            hnh = new Integer[2];
            hnh[0] = 120 ; hnh[1] = 100 ;
            newser.put("G" , hnh);
            newacc.put(3 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 5 Acc: 4 Ser:L -> 60 , 25
            hnh = new Integer[2];
            hnh[0] = 60 ; hnh[1] = 25 ;
            newser.put("L" , hnh);
            //Sost: 5 Acc: 4 Ser:G -> 120 , 60
            hnh = new Integer[2];
            hnh[0] = 120 ; hnh[1] = 60 ;
            newser.put("G" , hnh);
            newacc.put(4 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 5 Acc: 5 Ser:L -> 180 , 35
            hnh = new Integer[2];
            hnh[0] = 180 ; hnh[1] = 35 ;
            newser.put("L" , hnh);
            //Sost: 5 Acc: 5 Ser:G -> 400 , 60
            hnh = new Integer[2];
            hnh[0] = 400 ; hnh[1] = 60 ;
            newser.put("G" , hnh);
            newacc.put(5 , newser);
            rdata.put( 5 , newacc );
            newacc = new HashMap <Integer, HashMap <String, Integer[]>>();
            newser = new HashMap <String, Integer[]>();
            //Sost: 6 Acc: 3 Ser:L -> 60 , 45
            hnh = new Integer[2];
            hnh[0] = 60 ; hnh[1] = 45 ;
            newser.put("L" , hnh);
            //Sost: 6 Acc: 3 Ser:G -> 140 , 100
            hnh = new Integer[2];
            hnh[0] = 140 ; hnh[1] = 100 ;
            newser.put("G" , hnh);
            newacc.put(3 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 6 Acc: 4 Ser:L -> 60 , 25
            hnh = new Integer[2];
            hnh[0] = 60 ; hnh[1] = 25 ;
            newser.put("L" , hnh);
            //Sost: 6 Acc: 4 Ser:G -> 120 , 60
            hnh = new Integer[2];
            hnh[0] = 120 ; hnh[1] = 60 ;
            newser.put("G" , hnh);
            newacc.put(4 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 6 Acc: 5 Ser:L -> 180 , 45
            hnh = new Integer[2];
            hnh[0] = 180 ; hnh[1] = 45 ;
            newser.put("L" , hnh);
            //Sost: 6 Acc: 5 Ser:G -> 400 , 80
            hnh = new Integer[2];
            hnh[0] = 400 ; hnh[1] = 80 ;
            newser.put("G" , hnh);
            newacc.put(5 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 6 Acc: 10 Ser:L -> 550 , 160
            hnh = new Integer[2];
            hnh[0] = 550 ; hnh[1] = 160 ;
            newser.put("L" , hnh);
            //Sost: 6 Acc: 10 Ser:G -> 1060 , 300
            hnh = new Integer[2];
            hnh[0] = 1060 ; hnh[1] = 300 ;
            newser.put("G" , hnh);
            newacc.put(10 , newser);
            rdata.put( 6 , newacc );
            newacc = new HashMap <Integer, HashMap <String, Integer[]>>();
            newser = new HashMap <String, Integer[]>();
            //Sost: 7 Acc: 12 Ser:L -> 80 , 45
            hnh = new Integer[2];
            hnh[0] = 80 ; hnh[1] = 45 ;
            newser.put("L" , hnh);
            //Sost: 7 Acc: 12 Ser:G -> 160 , 80
            hnh = new Integer[2];
            hnh[0] = 160 ; hnh[1] = 80 ;
            newser.put("G" , hnh);
            newacc.put(12 , newser);
            rdata.put( 7 , newacc );
            newacc = new HashMap <Integer, HashMap <String, Integer[]>>();
            newser = new HashMap <String, Integer[]>();
            //Sost: 8 Acc: 13 Ser:L -> 25 , 10
            hnh = new Integer[2];
            hnh[0] = 25 ; hnh[1] = 10 ;
            newser.put("L" , hnh);
            //Sost: 8 Acc: 13 Ser:G -> 35 , 15
            hnh = new Integer[2];
            hnh[0] = 35 ; hnh[1] = 15 ;
            newser.put("G" , hnh);
            newacc.put(13 , newser);
            rdata.put( 8 , newacc );
            newacc = new HashMap <Integer, HashMap <String, Integer[]>>();
            newser = new HashMap <String, Integer[]>();
            //Sost: 9 Acc: 6 Ser:L -> 60 , 25
            hnh = new Integer[2];
            hnh[0] = 60 ; hnh[1] = 25 ;
            newser.put("L" , hnh);
            //Sost: 9 Acc: 6 Ser:G -> 180 , 100
            hnh = new Integer[2];
            hnh[0] = 180 ; hnh[1] = 100 ;
            newser.put("G" , hnh);
            newacc.put(6 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 9 Acc: 14 Ser:L -> 0 , 10
            hnh = new Integer[2];
            hnh[0] = 0 ; hnh[1] = 10 ;
            newser.put("L" , hnh);
            //Sost: 9 Acc: 14 Ser:G -> 0 , 25
            hnh = new Integer[2];
            hnh[0] = 0 ; hnh[1] = 25 ;
            newser.put("G" , hnh);
            newacc.put(14 , newser);
            rdata.put( 9 , newacc );
            newacc = new HashMap <Integer, HashMap <String, Integer[]>>();
            newser = new HashMap <String, Integer[]>();
            //Sost: 10 Acc: 8 Ser:L -> 60 , 35
            hnh = new Integer[2];
            hnh[0] = 60 ; hnh[1] = 35 ;
            newser.put("L" , hnh);
            //Sost: 10 Acc: 8 Ser:G -> 180 , 100
            hnh = new Integer[2];
            hnh[0] = 180 ; hnh[1] = 100 ;
            newser.put("G" , hnh);
            newacc.put(8 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 10 Acc: 9 Ser:L -> 60 , 45
            hnh = new Integer[2];
            hnh[0] = 60 ; hnh[1] = 45 ;
            newser.put("L" , hnh);
            //Sost: 10 Acc: 9 Ser:G -> 120 , 80
            hnh = new Integer[2];
            hnh[0] = 120 ; hnh[1] = 80 ;
            newser.put("G" , hnh);
            newacc.put(9 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 10 Acc: 14 Ser:L -> 0 , 10
            hnh = new Integer[2];
            hnh[0] = 0 ; hnh[1] = 10 ;
            newser.put("L" , hnh);
            //Sost: 10 Acc: 14 Ser:G -> 0 , 25
            hnh = new Integer[2];
            hnh[0] = 0 ; hnh[1] = 25 ;
            newser.put("G" , hnh);
            newacc.put(14 , newser);
            rdata.put( 10 , newacc );
            newacc = new HashMap <Integer, HashMap <String, Integer[]>>();
            newser = new HashMap <String, Integer[]>();
            //Sost: 11 Acc: 8 Ser:L -> 25 , 15
            hnh = new Integer[2];
            hnh[0] = 25 ; hnh[1] = 15 ;
            newser.put("L" , hnh);
            //Sost: 11 Acc: 8 Ser:G -> 80 , 35
            hnh = new Integer[2];
            hnh[0] = 80 ; hnh[1] = 35 ;
            newser.put("G" , hnh);
            newacc.put(8 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 11 Acc: 9 Ser:L -> 5 , 5
            hnh = new Integer[2];
            hnh[0] = 5 ; hnh[1] = 5 ;
            newser.put("L" , hnh);
            //Sost: 11 Acc: 9 Ser:G -> 5 , 5
            hnh = new Integer[2];
            hnh[0] = 5 ; hnh[1] = 5 ;
            newser.put("G" , hnh);
            newacc.put(9 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 11 Acc: 11 Ser:L -> 10 , 5
            hnh = new Integer[2];
            hnh[0] = 10 ; hnh[1] = 5 ;
            newser.put("L" , hnh);
            //Sost: 11 Acc: 11 Ser:G -> 35 , 5
            hnh = new Integer[2];
            hnh[0] = 35 ; hnh[1] = 5 ;
            newser.put("G" , hnh);
            newacc.put(11 , newser);
            rdata.put( 11 , newacc );
            newacc = new HashMap <Integer, HashMap <String, Integer[]>>();
            newser = new HashMap <String, Integer[]>();
            //Sost: 12 Acc: 6 Ser:L -> 35 , 15
            hnh = new Integer[2];
            hnh[0] = 35 ; hnh[1] = 15 ;
            newser.put("L" , hnh);
            //Sost: 12 Acc: 6 Ser:G -> 100 , 60
            hnh = new Integer[2];
            hnh[0] = 100 ; hnh[1] = 60 ;
            newser.put("G" , hnh);
            newacc.put(6 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 12 Acc: 7 Ser:L -> 5 , 5
            hnh = new Integer[2];
            hnh[0] = 5 ; hnh[1] = 5 ;
            newser.put("L" , hnh);
            //Sost: 12 Acc: 7 Ser:G -> 5 , 5
            hnh = new Integer[2];
            hnh[0] = 5 ; hnh[1] = 5 ;
            newser.put("G" , hnh);
            newacc.put(7 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 12 Acc: 11 Ser:L -> 35 , 5
            hnh = new Integer[2];
            hnh[0] = 35 ; hnh[1] = 5 ;
            newser.put("L" , hnh);
            //Sost: 12 Acc: 11 Ser:G -> 160 , 5
            hnh = new Integer[2];
            hnh[0] = 160 ; hnh[1] = 5 ;
            newser.put("G" , hnh);
            newacc.put(11 , newser);
            newser = new HashMap <String, Integer[]>();
            //Sost: 12 Acc: 14 Ser:L -> 0 , 10
            hnh = new Integer[2];
            hnh[0] = 0 ; hnh[1] = 10 ;
            newser.put("L" , hnh);
            //Sost: 12 Acc: 14 Ser:G -> 0 , 25
            hnh = new Integer[2];
            hnh[0] = 0 ; hnh[1] = 25 ;
            newser.put("G" , hnh);
            newacc.put(14 , newser);
            rdata.put( 12 , newacc );
        }

        return rdata;
    }
}
