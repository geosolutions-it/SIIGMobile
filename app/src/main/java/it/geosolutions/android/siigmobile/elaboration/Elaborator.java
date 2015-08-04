package it.geosolutions.android.siigmobile.elaboration;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.mapsforge.core.model.BoundingBox;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import it.geosolutions.android.map.database.SpatialDataSourceManager;
import it.geosolutions.android.map.utils.MapFilesProvider;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonDeserializer;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonSerializer;
import it.geosolutions.android.map.wfs.geojson.feature.Feature;
import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;
import it.geosolutions.android.siigmobile.BuildConfig;
import it.geosolutions.android.siigmobile.Config;
import it.geosolutions.android.siigmobile.ProgressDialogCallback;
import it.geosolutions.android.siigmobile.R;
import it.geosolutions.android.siigmobile.Util;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;
import it.geosolutions.android.siigmobile.wps.RiskWPSRequest;
import it.geosolutions.android.siigmobile.wps.SIIGRetrofitClient;
import it.geosolutions.android.siigmobile.wps.SIIGWPSServices;
import jsqlite.Database;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedString;

/**
 * Created by Robert Oehler on 27.07.15.
 *
 */
public abstract class Elaborator {

    private final static String TAG = Elaborator.class.getSimpleName();

    public abstract void showError(int resource);
    public abstract void showMessage(String message);

    public abstract void done(ElaborationResult result);

    private Context mContext;

    private ElaborationResult mResult;

    public Elaborator(final Context pContext){
        this.mContext = pContext;
    }


    /**
     * starts a calculation based on a BoundingBox
     * @param bb the bounding box to use
     * @param isPolygon if the query is polygon or not
     * @param title user edited title of the processing
     * @param description user edited description of the processing (may be null)
     */
    public void startCalc(final BoundingBox bb, final boolean isPolygon, final String title, final String description) {

        if (!Util.isOnline(mContext)) {
            showError(R.string.snackbar_offline_text);
            return;
        }

        if (bb == null) {
            showError(R.string.snackbar_noparam_text);
            return;
        }

        mResult = new ElaborationResult(title,description,null,null);

        calc(bb,Config.FORMULA_RISK,isPolygon);

    }

    /**
     * starts an internal calculation
     * if the formula is risk, a sequential street calculation is added
     * @param bb the BoundingBox to use
     * @param pFormula the formula to apply
     * @param isPolygon if this is a polygon request
     */
    private void calc(final BoundingBox bb, final int pFormula,final boolean isPolygon){

        final String userFormula = pFormula == Config.FORMULA_STREET ? mContext.getString(R.string.formula_street) : mContext.getString(R.string.formula_risk);

        final int[] formula = {pFormula};
        // Prepare dummy input features
        Coordinate[] coords = new Coordinate[2];
        coords[0] = new Coordinate(bb.minLongitude, bb.minLatitude);
        coords[1] = new Coordinate(bb.maxLongitude, bb.maxLatitude);
        GeometryFactory fact = new GeometryFactory();
        FeatureCollection features = new FeatureCollection();
        features.features = new ArrayList<Feature>();
        Feature bbox = new Feature();
        bbox.geometry = fact.createLineString(coords);
        features.features.add(bbox);

        // Fixed Values
        String store = "destination";
        int batch = 1000;
        int precision = 4;
        int processing = 1;
        int target = 100;
        //add geometry parameter to request polygon
        //parameter level 1 è multilinestring 3 è multipolygon
        int level = isPolygon ? 3 : 1;
        String kemler = "1,2,3,4,5,6,7,8,9,10,11,12";
        String materials = "1,2,3,4,5,6,7,8,9,10,11,12";
        String scenarios = "1,2,3,4,5,6,7,8,9,10,11,12,13,14";
        String severeness = "1,2,3,4,5";
        String entities = "0,1";
        String fp = "fp_scen_centrale";

        // Create RiskWPSRequest
        final RiskWPSRequest request = new RiskWPSRequest(
                features,
                store,
                batch,
                precision,
                processing,
                formula[0],
                target,
                level,
                kemler,
                materials,
                scenarios,
                entities,
                severeness,
                fp
        );

        request.setParameter(RiskWPSRequest.KEY_EXTENDEDSCHEMA, false);
        request.setParameter(RiskWPSRequest.KEY_MOBILE, true);


        String query = null;
        try {
            query = RiskWPSRequest.createWPSCallFromText(request);

        } catch (IllegalArgumentException iae) {
            showError(R.string.snackbar_noparam_text);
            return;
        }

        if (query == null) {
            showError(R.string.snackbar_noparam_text);
            return;
        }
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeHierarchyAdapter(Geometry.class,
                        new GeometryJsonSerializer())
                .registerTypeHierarchyAdapter(Geometry.class,
                        new GeometryJsonDeserializer()).create();

        final OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setReadTimeout(1, TimeUnit.MINUTES);
        okHttpClient.setConnectTimeout(1, TimeUnit.MINUTES);


        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(SIIGRetrofitClient.ENDPOINT)
                //.setLogLevel(RestAdapter.LogLevel.HEADERS_AND_ARGS)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Authorization", Config.DESTINATION_AUTHORIZATION);
                    }
                })
                .setConverter(new GsonConverter(gson))
                .setClient(new OkClient(okHttpClient))
                .build();

        //wrap the xml as "TypedString"
        //src : http://stackoverflow.com/questions/21398598/how-to-post-raw-whole-json-in-the-body-of-a-retrofit-request
        TypedString typedQueryString = new TypedString(query) {
            @Override
            public String mimeType() {
                return "application/xml";
            }
        };

        SIIGWPSServices siigService = restAdapter.create(SIIGWPSServices.class);

        ProgressDialogCallback<CRSFeatureCollection> fcCallback = new ProgressDialogCallback<CRSFeatureCollection>() {

            @Override
            public void success(CRSFeatureCollection result, Response response) {
                if (BuildConfig.DEBUG) {
                    Log.i("WPSCall", "success " + result.toString() + "\n" + response.toString());
                }

                //save response asynchronously
                new AsyncTask<CRSFeatureCollection, Void, Pair<Boolean,String>>() {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        if (pd != null && pd.isShowing()) {
                            pd.dismiss();
                        }
                        if(mContext instanceof Activity) {
                            pd = new ProgressDialog(mContext, ProgressDialog.STYLE_SPINNER);
                            pd.setMessage(mContext.getString(R.string.saving_data));
                            pd.setCancelable(false);
                            pd.setIcon(R.drawable.ic_launcher);
                            pd.show();
                        }
                    }

                    @Override
                    protected Pair<Boolean,String> doInBackground(CRSFeatureCollection... param) {

                        final Database db = SpatialiteUtils.openSpatialiteDB(mContext);

                        final CRSFeatureCollection response = param[0];

                        if (response.features.size() > Config.WPS_MAX_FEATURES) {
                            showError(R.string.result_too_large);
                            return null;
                        }

                        if (response.features.size() <= 0) {
                            showError(R.string.result_empty);
                            return null;
                        }

                        final String geomType = response.features.get(0).geometry.getGeometryType().toUpperCase();

                        final Pair<Boolean, String> resultPair = SpatialiteUtils.saveResult(db, response, geomType, formula[0] == Config.FORMULA_STREET);

                        if(!resultPair.first || resultPair.second == null){
                            showMessage(mContext.getString(R.string.error_saving, mResult.getUserEditedName()));
                            return null;
                        }

                        //and relate to users name
                        boolean nameSaved = SpatialiteUtils.updateNameTableWithUserData(db, resultPair.second, mResult.getUserEditedName(), mResult.getUserEditedDescription());

                        if(!nameSaved){
                            showMessage(mContext.getString(R.string.error_saving, mResult.getUserEditedName()));
                            return null;
                        }

                        try {

                            db.close();

                        } catch (jsqlite.Exception e) {
                            Log.e(TAG, "exception closing db", e);
                        }

                        //reload the spatial data sources to be able to load this new table in the MainActivity
                        SpatialDataSourceManager.getInstance().clear();
                        SpatialDataSourceManager.getInstance().init(MapFilesProvider.getBaseDirectoryFile());

                        return new Pair<>(true,resultPair.second);
                    }

                    @Override
                    protected void onPostExecute(final Pair<Boolean,String> result) {
                        super.onPostExecute(result);

                        if (pd != null && pd.isShowing()) {
                            pd.dismiss();
                        }

                        if(result.first){
                            if(pFormula == Config.FORMULA_RISK){
                                //set table name and continue
                                mResult.setRiskTableName(result.second);
                                calc(bb, Config.FORMULA_STREET, isPolygon);
                            }else{
                                //done, set table name and return result
                                mResult.setStreetTableName(result.second);

                                done(mResult);
                            }
                            showMessage(mContext.getString(R.string.success_saving, mResult.getUserEditedName(), userFormula));
                        }
                    }
                }.execute(result);

            }

            @Override
            public void failure(RetrofitError error) {

                Log.w("WPSCall", "failure " + error.getMessage());
                if (pd != null && pd.isShowing()) {
                    pd.dismiss();
                }
                showError(R.string.compute_error);
            }
        };

        if(mContext instanceof Activity) {

            fcCallback.pd = new ProgressDialog(mContext, ProgressDialog.STYLE_SPINNER);
            fcCallback.pd.setMessage(mContext.getString(R.string.query_ongoing));
            fcCallback.pd.setCancelable(false);
            fcCallback.pd.setIcon(R.drawable.ic_launcher);
            fcCallback.pd.show();
        }
        siigService.postWPS(typedQueryString, fcCallback);

    }

}
