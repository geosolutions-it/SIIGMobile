package it.geosolutions.android.siigmobile.wfs;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.mapsforge.core.model.GeoPoint;

import java.util.ArrayList;

import it.geosolutions.android.map.BuildConfig;
import it.geosolutions.android.map.adapters.FeatureInfoAttributesAdapter;
import it.geosolutions.android.map.model.Feature;
import it.geosolutions.android.map.utils.ConversionUtilities;
import it.geosolutions.android.map.wms.GetFeatureInfoConfiguration;
import it.geosolutions.android.siigmobile.Config;
import it.geosolutions.android.siigmobile.R;
import it.geosolutions.android.siigmobile.Util;
import it.geosolutions.android.siigmobile.elaboration.ElaborationResult;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;

/**
 * Created by Robert Oehler on 11.09.15.
 *
 * Activity containing a list fragment to show results of
 * a WFS getFeature request for a certain area and layer
 *
 * The fragment is based on it.geosolutions.android.map.fragment.featureinfo.FeatureInfoAttributeListFragment
 *
 */
public class WFSBersagliDataActivity extends AppCompatActivity {

    private final static String TAG = WFSBersagliDataActivity.class.getSimpleName();

    public final static String PARAM_ELABORATION = "PARAM_ELAB";

    private ProgressDialog pd;

    private WFSResultFragment fragment;
    private ElaborationResult mResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wfs_bersagli_data_form);

        Toolbar toolbar = (Toolbar) findViewById(R.id.m_toolbar);
        setSupportActionBar(toolbar);

        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Inflate a menu to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.main);

        //get the elaboration result from the intent
        if(getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey(PARAM_ELABORATION)){
            mResult = (ElaborationResult) getIntent().getExtras().get(PARAM_ELABORATION);
        }

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.target_list));
        }

        final Spinner layerSpinner = (Spinner) findViewById(R.id.layer_spinner);

        final String[] layers = getResources().getStringArray(R.array.drawer_items);

        //layers contains grafo , calc and show results, filter them
        String[] finalLayers = new String[Config.WFS_LAYERS.length];
        for(int i = 0; i < finalLayers.length; i++){
            if(i == 0){
                finalLayers[0] = getString(R.string.select_layer);
            }else{
                finalLayers[i] = layers[i];
            }
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, finalLayers);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        layerSpinner.setAdapter(spinnerArrayAdapter);

        layerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position != 0) {
                    //start calc
                    showProgress(getString(R.string.please_wait));

                    final String layerName = Config.WFS_LAYERS[position];

                    if(mResult == null){
                        Log.w(TAG, "No result is loaded");
                        hideProgress();
                        return;
                    }

                    final GeoPoint p =  mResult.getLocation();
                    final int radius =  mResult.getRadius();

                    if(p == null || radius <= 0){
                        Log.w(TAG, "Invalid result data");
                        hideProgress();
                        return;
                    }

                    WFSRequest.getWFS(layerName, p, radius, Config.DESTINATION_AUTHORIZATION, new WFSRequest.WFSRequestFeedback() {
                        @Override
                        public void success(CRSFeatureCollection result) {

                            hideProgress();

                            if (BuildConfig.DEBUG) {
                                Log.i(TAG, "success : " + result.features.size() + " features");
                            }
                            if (result.features != null && result.features.size() > 0) {

                                GetFeatureInfoConfiguration.Locale locale = Util.getLocaleConfig(getBaseContext());

                                ArrayList<it.geosolutions.android.map.model.Feature> mapModelFeatures = ConversionUtilities.convertWFSFeatures(locale, layerName, result.features);

                                fragment.setFeatures(mapModelFeatures);

                            } else {

                                fragment.setNoData();

                            }

                        }

                        @Override
                        public void error(String errorMessage) {

                            hideProgress();
                            Log.w(TAG, "error WFS query " + errorMessage);

                        }
                    });

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        fragment = WFSResultFragment.newInstance();

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.compute_container, fragment)
                .commit();

    }

    public void showProgress(final String message){

        if(pd == null){
            pd = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
            pd.setCancelable(false);
            pd.setIcon(R.drawable.ic_launcher);
        }
        pd.setMessage(message);
        pd.show();

    }
    public void hideProgress(){

        if(pd != null && pd.isShowing()){
            pd.dismiss();
        }
    }

    public WFSResultFragment getFragment(){
        return fragment;
    }


    public static class WFSResultFragment extends ListFragment {

        private FeatureInfoAttributesAdapter adapter;

        protected ArrayList<Feature> currentFeatures;

        public static final String PARAM_FEATURES = "features";

        private int currentSelectedItem = 0;

        private ImageButton prevButton;
        private ImageButton nextButton;
        private RelativeLayout buttonLayout;
        private TextView noData;

        /**
         * Returns a new instance of this fragment
         */
        public static WFSResultFragment newInstance() {

            return new WFSResultFragment();
        }

        public WFSResultFragment() {
        }

        /**
         * Called only once
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // view operations

            setRetainInstance(true);
            // start progress bars
            getActivity().setProgressBarIndeterminateVisibility(true);
            getActivity().setProgressBarVisibility(true);

            // get data from the intent

            Bundle extras = getActivity().getIntent().getExtras();

            if(extras != null) {
                currentFeatures = (ArrayList<Feature>) extras.getSerializable(PARAM_FEATURES);
            }

            // setup the listView
            adapter = new FeatureInfoAttributesAdapter(getActivity(),it.geosolutions.android.map.R.layout.feature_info_attribute_row);
            setListAdapter(adapter);

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

            return inflater.inflate(it.geosolutions.android.map.R.layout.feature_info_attribute_list, container, false);

        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            buttonLayout = (RelativeLayout) view.findViewById(R.id.attributeButtonBar);
            prevButton   = (ImageButton) view.findViewById(R.id.previousButton);
            nextButton   = (ImageButton) view.findViewById(R.id.nextButton);
            noData       = (TextView) view.findViewById(it.geosolutions.android.map.R.id.empty_text);
            final ImageButton marker   = (ImageButton) view.findViewById(R.id.use_for_marker);

            marker.setVisibility(View.GONE);

            // load the previous page on click
            prevButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    adapter.clear();
                    adapter.addAll(currentFeatures.get(--currentSelectedItem));
                    adapter.notifyDataSetChanged();
                    setButtonBarVisibility();
                }
            });

            // load the next page on press
            nextButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    adapter.clear();
                    adapter.addAll(currentFeatures.get(++currentSelectedItem));
                    adapter.notifyDataSetChanged();
                    setButtonBarVisibility();

                }
            });

            setButtonBarVisibility();

            if(currentFeatures == null){
                setNoData();
            } else {

                adapter.addAll(currentFeatures.get(currentSelectedItem));
            }
        }

        /**
         * Set the visibility using the size of the features
         *
         */
        private void setButtonBarVisibility() {

            if(currentFeatures == null){
                buttonLayout.setVisibility(View.INVISIBLE);
            }else {
                buttonLayout.setVisibility(View.VISIBLE);

                if (currentSelectedItem == 0) { //no prev

                    if (prevButton != null) {
                        prevButton.setVisibility(View.INVISIBLE);
                    }

                } else { // > 0
                    if (prevButton != null) {
                        prevButton.setVisibility(View.VISIBLE);
                    }
                }

                if (currentSelectedItem + 1 < currentFeatures.size()) {
                    if (nextButton != null) {
                        nextButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (nextButton != null) {
                        nextButton.setVisibility(View.GONE);
                    }
                }
            }
        }

        /**
         * sets no data view in default listview empty text
         */
        private void setNoData() {

            if(adapter != null){
                adapter.clear();
                adapter.notifyDataSetChanged();
            }

            if(noData != null){
                noData.setText(R.string.wfs_no_data);
            }
            currentFeatures = null;
            currentSelectedItem = 0;
            setButtonBarVisibility();
        }

        public void setFeatures(ArrayList<it.geosolutions.android.map.model.Feature> features){

            currentSelectedItem = 0;
            currentFeatures = features;

            adapter.clear();
            adapter.addAll(currentFeatures.get(currentSelectedItem));
            adapter.notifyDataSetChanged();
            setButtonBarVisibility();

        }

        public FeatureInfoAttributesAdapter getAdapter() {
            return adapter;
        }
    }

}
