package it.geosolutions.android.siigmobile;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import it.geosolutions.android.map.activities.MapActivityBase;
import it.geosolutions.android.map.control.CoordinateControl;
import it.geosolutions.android.map.control.LocationControl;
import it.geosolutions.android.map.control.MapControl;
import it.geosolutions.android.map.control.MapInfoControl;
import it.geosolutions.android.map.model.Layer;
import it.geosolutions.android.map.model.MSMMap;
import it.geosolutions.android.map.overlay.managers.MultiSourceOverlayManager;
import it.geosolutions.android.map.overlay.managers.OverlayManager;
import it.geosolutions.android.map.spatialite.SpatialiteLayer;
import it.geosolutions.android.map.utils.MapFilesProvider;
import it.geosolutions.android.map.utils.SpatialDbUtils;
import it.geosolutions.android.map.view.AdvancedMapView;


public class MainActivity extends MapActivityBase
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {


    // default path for files
    private static final File MAP_DIR = MapFilesProvider.getBaseDirectoryFile();
    private static final File MAP_FILE = MapFilesProvider.getBackgroundMapFile();

    /**
     * Tag for Logging
     */
    private static final String TAG = "MainActivity";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;


    private MultiSourceOverlayManager layerManager;

    public AdvancedMapView mapView;
    public OverlayManager overlayManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mapView =  (AdvancedMapView) findViewById(R.id.advancedMapView);

        // Setup LayerManager
        layerManager =  new MultiSourceOverlayManager(mapView);

        // Enable Touch events
        mapView.setClickable(true);

        // Show ScaleBar
        mapView.getMapScaleBar().setShowMapScaleBar(true);

        // Coordinate Control
        mapView.addControl(new CoordinateControl(mapView, true));

        // Info Control
        MapInfoControl ic= new MapInfoControl(mapView,this);
        ic.setActivationButton((ImageButton) findViewById(R.id.ButtonInfo));
        ic.setMode(MapControl.MODE_VIEW);
        mapView.addControl(ic);

        // Location Control
        LocationControl lc  =new LocationControl(mapView);
        lc.setActivationButton((ImageButton)findViewById(R.id.ButtonLocation));
        mapView.addControl(lc);

        // Add Layers
        MSMMap mapConfig = SpatialDbUtils.mapFromDb(true);

        ArrayList<Layer> layers = new ArrayList<Layer>();
        for(Layer l : mapConfig.layers){
            Log.d(TAG, "Layer Title: " + l.getTitle());
            if(l.getTitle().startsWith("v_elab")){
                layers.add(l);
            }
        }

        // Add Neutral style
        for(Layer l : layers){
            if(l instanceof SpatialiteLayer){
                Log.d(TAG, "Setting Style for layer: " + l.getTitle());
                ((SpatialiteLayer) l).setStyleFileName(l.getTitle().replace("v_elab_std", "grafo"));
            }
        }

        layerManager.setLayers(layers);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        if(layerManager == null){
            // nothing to do
            return;
        }
        ArrayList<Layer> layers = layerManager.getLayers();
        switch (position){
            case 0:
                // Set Neutral style
                for(Layer l : layers){
                    if(l instanceof SpatialiteLayer){
                        Log.d(TAG, "Setting Style for layer: " + l.getTitle());
                        ((SpatialiteLayer) l).setStyleFileName(l.getTitle().replace("v_elab_std", "grafo"));
                    }
                }
                layerManager.setLayers(layers);
                mapView.redraw();
                break;
            case 1:
                // Set Neutral style
                for(Layer l : layers){
                    if(l instanceof SpatialiteLayer){
                        Log.d(TAG, "Setting Style for layer: " + l.getTitle());
                        ((SpatialiteLayer) l).setStyleFileName(l.getTitle().replace("v_elab_std", "ambientale"));
                    }
                }
                layerManager.setLayers(layers);
                mapView.redraw();
                break;
            case 2:
                // Set Neutral style
                for(Layer l : layers){
                    if(l instanceof SpatialiteLayer){
                        Log.d(TAG, "Setting Style for layer: " + l.getTitle());
                        ((SpatialiteLayer) l).setStyleFileName(l.getTitle().replace("v_elab_std", "sociale"));
                    }
                }
                layerManager.setLayers(layers);
                mapView.redraw();
                break;
            case 3:
                // Set Neutral style
                for(Layer l : layers){
                    if(l instanceof SpatialiteLayer){
                        Log.d(TAG, "Setting Style for layer: " + l.getTitle());
                        ((SpatialiteLayer) l).setStyleFileName(l.getTitle().replace("v_elab_std", "totale"));
                    }
                }
                layerManager.setLayers(layers);
                mapView.redraw();
                break;

            case 4:
                Toast.makeText(getBaseContext(), "Starting Form...", Toast.LENGTH_SHORT).show();
                // Start the form activity
                Intent formIntent = new Intent(this, ComputeFormActivity.class);
                startActivity(formIntent);
                break;
            default:
                /*
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                        .commit();
                */
                break;
        }

    }

    public void onSectionAttached(int number) {
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
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
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
        if (id == R.id.action_settings) {
            return true;
        }

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
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    public void onRiskRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((AppCompatRadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_risk:
                if (checked)

                    break;
            case R.id.radio_street:
                if (checked)

                    break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Controls can be refreshed getting the result of an intent, in this case
        // each control knows which intent he sent with their requestCode/resultCode
        for(MapControl control : mapView.getControls()){
            control.refreshControl(requestCode,resultCode, data);
        }
    }
}
