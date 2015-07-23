package it.geosolutions.android.siigmobile;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.mapsforge.core.model.BoundingBox;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import it.geosolutions.android.map.database.SpatialDataSourceManager;
import it.geosolutions.android.map.utils.MapFilesProvider;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonDeserializer;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonSerializer;
import it.geosolutions.android.map.wfs.geojson.feature.Feature;
import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;
import it.geosolutions.android.siigmobile.wps.RiskWPSRequest;
import it.geosolutions.android.siigmobile.wps.SIIGRetrofitClient;
import it.geosolutions.android.siigmobile.wps.SIIGWPSServices;
import jsqlite.*;
import jsqlite.Exception;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedString;


public class ComputeFormActivity extends AppCompatActivity
        implements ComputeNavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = ComputeFormActivity.class.getSimpleName();

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private ComputeNavigationDrawerFragment mNavigationDrawerFragment;

    public final static String PARAM_BOUNDINGBOX = "BoundingBox";
    public final static String PARAM_POLYGON     = "Polygon";

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compute_form);

        mNavigationDrawerFragment = (ComputeNavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.compute_navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.compute_navigation_drawer,
                (DrawerLayout) findViewById(R.id.compute_drawer_layout));

        mNavigationDrawerFragment.setEntries(new String[]{
                        getString(R.string.go_back)
                }
        );


    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        if(mNavigationDrawerFragment != null && position == 0){

            finish();
        }

        BoundingBox bb = null;

        if(getIntent() != null && getIntent().getSerializableExtra(PARAM_BOUNDINGBOX) != null){
            bb = (BoundingBox) getIntent().getSerializableExtra(PARAM_BOUNDINGBOX);
        }
        boolean polygonRequest = false;
        if(getIntent() != null){
            polygonRequest = getIntent().getBooleanExtra(PARAM_POLYGON,false);
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.compute_container, PlaceholderFragment.newInstance(position + 1, bb, polygonRequest))
                .commit();
    }

    public void onSectionAttached(int number) {
        /*
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_graph);
                break;
            case 2:
                mTitle = getString(R.string.title_elab_start);
                break;
            case 3:
                mTitle = getString(R.string.title_elab_load);
                break;
        }
        */
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        //actionBar.setTitle(mTitle);
        actionBar.setTitle(getResources().getStringArray(R.array.drawer_items)[4]);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.compute_form, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber, final BoundingBox bb, final boolean polygonRequest) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putSerializable(PARAM_BOUNDINGBOX, bb);
            args.putBoolean(PARAM_POLYGON, polygonRequest);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_compute_form, container, false);

            final TextView ul_lat_tv = (TextView) rootView.findViewById(R.id.tv_upper_left_lat);
            final TextView ul_lon_tv = (TextView) rootView.findViewById(R.id.tv_upper_left_lon);
            final TextView lr_lat_tv = (TextView) rootView.findViewById(R.id.tv_lower_right_lat);
            final TextView lr_lon_tv = (TextView) rootView.findViewById(R.id.tv_lower_right_lon);

            if(getArguments() != null && getArguments().get(PARAM_BOUNDINGBOX) != null){

                final BoundingBox bb = (BoundingBox) getArguments().get(PARAM_BOUNDINGBOX);

                ul_lat_tv.setText(String.format(Locale.US, "%f",bb.maxLatitude));
                ul_lon_tv.setText(String.format(Locale.US, "%f",bb.minLongitude));

                lr_lat_tv.setText(String.format(Locale.US, "%f",bb.minLatitude));
                lr_lon_tv.setText(String.format(Locale.US, "%f",bb.maxLongitude));

                Log.v(TAG, "Area : " + String.format(Locale.US, "%f",((bb.maxLongitude -bb.minLongitude)*(bb.maxLatitude -bb.minLatitude))) );

            }

            // Default formula
            // 141 - Rischio
            final int[] formula = {Config.FORMULA_RISK};

            final Button computeButton = (Button) rootView.findViewById(R.id.compute_button);
            computeButton.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {

                    if(!Util.isOnline(getActivity())){
                        Snackbar
                                .make(  v.getRootView().findViewById(R.id.snackbarPosition),
                                        R.string.snackbar_offline_text,
                                        Snackbar.LENGTH_LONG)
                                .show();
                        return;
                    }

                    if(getArguments() == null || getArguments().get(PARAM_BOUNDINGBOX) == null){
                        Snackbar
                                .make(  v.getRootView().findViewById(R.id.snackbarPosition),
                                        R.string.snackbar_offline_text,
                                        Snackbar.LENGTH_LONG)
                                .show();
                        return;
                    }

                    final BoundingBox bb = (BoundingBox) getArguments().get(PARAM_BOUNDINGBOX);

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
                    int level = 3;
                    String kemler = "1,2,3,4,5,6,7,8,9,10,11,12";
                    String materials = "1,2,3,4,5,6,7,8,9,10,11,12";
                    String scenarios = "1,2,3,4,5,6,7,8,9,10,11,12,13,14";
                    String severeness = "1,2,3,4,5";
                    String entities = "0,1";
                    String fp = "fp_scen_centrale";

                    // Selected formula id

                    RadioButton tryRadioBtn = (RadioButton) v.getRootView().findViewById(R.id.radio_street);
                    // 22 - Pis
                    if(tryRadioBtn != null && tryRadioBtn.isChecked()){
                        formula[0] = Config.FORMULA_STREET;
                    }

                    // Create RiskWPSRequest
                    RiskWPSRequest request = new RiskWPSRequest(
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

                    }catch (IllegalArgumentException iae){
                        Snackbar
                                .make(  v.getRootView().findViewById(R.id.snackbarPosition),
                                        R.string.snackbar_noparam_text,
                                        Snackbar.LENGTH_LONG)
                                .show();
                        return;
                    }

                    if(query == null){
                        Snackbar
                                .make(  v.getRootView().findViewById(R.id.snackbarPosition),
                                        R.string.snackbar_noparam_text,
                                        Snackbar.LENGTH_LONG)
                                .show();
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
                            .setLogLevel(RestAdapter.LogLevel.HEADERS_AND_ARGS)
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
                    TypedString typedQueryString = new TypedString(query){
                        @Override public String mimeType() {
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
                            new AsyncTask<CRSFeatureCollection,Void,Pair<Boolean,String>>(){

                                @Override
                                protected void onPreExecute() {
                                    super.onPreExecute();
                                    if(pd != null && pd.isShowing()) {
                                        pd.dismiss();
                                    }
                                    pd = new ProgressDialog(getActivity(), ProgressDialog.STYLE_SPINNER);
                                    pd.setMessage(getActivity().getString(R.string.saving_data));
                                    pd.setCancelable(false);
                                    pd.setIcon(R.drawable.ic_launcher);
                                    pd.show();
                                }

                                @Override
                                protected Pair<Boolean,String> doInBackground(CRSFeatureCollection... param) {

                                    final Database db = SpatialiteUtils.openSpatialiteDB(getActivity().getBaseContext());

                                    final CRSFeatureCollection response = param[0];

                                    if(response.features.size() > Config.WPS_MAX_FEATURES){
                                        Snackbar
                                                .make(computeButton.getRootView().findViewById(R.id.snackbarPosition),
                                                        R.string.result_too_large,
                                                        Snackbar.LENGTH_LONG)
                                                .show();
                                        return null;
                                    }

                                    if(response.features.size() <= 0){
                                        Snackbar
                                                .make(computeButton.getRootView().findViewById(R.id.snackbarPosition),
                                                        R.string.result_empty,
                                                        Snackbar.LENGTH_LONG)
                                                .show();
                                        return null;
                                    }

                                    final String geomType = response.features.get(0).geometry.getGeometryType().toUpperCase();

                                    final Pair<Boolean, String> resultPair = SpatialiteUtils.saveResult(db, response, geomType, formula[0] == Config.FORMULA_STREET);
                                    try {

                                        db.close();

                                    } catch (Exception e) {
                                        Log.e(TAG, "exception closing db", e);
                                    }

                                    //reload the spatial data sources to be able to load this new table in the MainActivity
                                    SpatialDataSourceManager.getInstance().clear();
                                    SpatialDataSourceManager.getInstance().init(MapFilesProvider.getBaseDirectoryFile());

                                    return resultPair;
                                }

                                @Override
                                protected void onPostExecute(final Pair<Boolean,String> pair) {
                                    super.onPostExecute(pair);

                                    if(pd != null && pd.isShowing()) {
                                        pd.dismiss();
                                    }

                                    if(pair == null) {
                                        return;
                                    }

                                    if(pair.first) {

                                        Intent returnIntent = new Intent();
                                        returnIntent.putExtra(Config.RESULT_TABLENAME, pair.second);
                                        returnIntent.putExtra(Config.RESULT_FORMULA, formula[0]);
                                        getActivity().setResult(RESULT_OK, returnIntent);
                                        getActivity().finish();

                                    }else{

                                        Toast.makeText(getActivity().getBaseContext(),pair.second,Toast.LENGTH_LONG).show();
                                    }
                                }
                            }.execute(result);

                        }

                        @Override
                        public void failure(RetrofitError error) {

                            Log.w("WPSCall", "failure " + error.getMessage());
                            if(pd != null && pd.isShowing()) {
                                pd.dismiss();
                            }
                            Snackbar
                                    .make(computeButton.getRootView().findViewById(R.id.snackbarPosition),
                                            R.string.compute_error,
                                            Snackbar.LENGTH_LONG)
                                    .show();
                        }
                    };

                    fcCallback.pd = new ProgressDialog(getActivity(), ProgressDialog.STYLE_SPINNER);
                    fcCallback.pd.setMessage(getActivity().getString(R.string.query_ongoing));
                    fcCallback.pd.setCancelable(false);
                    fcCallback.pd.setIcon(R.drawable.ic_launcher);
                    fcCallback.pd.show();

                    siigService.postWPS(typedQueryString, fcCallback);


                    final boolean polygonRequest = getArguments().getBoolean(PARAM_POLYGON);
                    //TODO add geometry parameter to request polygon
                    //TODO parameter level 1 è multilinestring 3 è multipolygon

                    if(BuildConfig.DEBUG) {
                        Log.i(TAG, "is Polygon Request " + Boolean.toString(polygonRequest));
                    }


                }
            });

            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((ComputeFormActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    public void onRiskRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((AppCompatRadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_risk:
                if (checked){
                    TextView descrTextView = (TextView) view.getRootView().findViewById(R.id.form_compute_option_description);
                    if(descrTextView != null){
                        descrTextView.setText(R.string.risk_description);
                    }
                    TextView titleTextView = (TextView) view.getRootView().findViewById(R.id.option_description_title);
                    if(titleTextView != null){
                        titleTextView.setText(R.string.risk_description_title);
                    }
                }
                break;
            case R.id.radio_street:
                if (checked){
                    TextView descrTextView = (TextView) view.getRootView().findViewById(R.id.form_compute_option_description);
                    if(descrTextView != null){
                        descrTextView.setText(R.string.road_risk_description);
                    }
                    TextView titleTextView = (TextView) view.getRootView().findViewById(R.id.option_description_title);
                    if(titleTextView != null){
                        titleTextView.setText(R.string.road_risk_description_title);
                    }
                }
                break;
        }
    }


}
