package it.geosolutions.android.siigmobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.newrelic.agent.android.NewRelic;
import com.squareup.okhttp.Headers;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.ListOverlay;
import org.mapsforge.android.maps.overlay.Marker;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import it.geosolutions.android.map.style.AdvancedStyle;
import it.geosolutions.android.map.style.StyleManager;
import it.geosolutions.android.map.utils.MapFilesProvider;
import it.geosolutions.android.map.utils.SpatialDbUtils;
import it.geosolutions.android.map.utils.ZipFileManager;
import it.geosolutions.android.map.view.AdvancedMapView;
import it.geosolutions.android.map.wms.WMSLayer;
import it.geosolutions.android.map.wms.WMSSource;
import it.geosolutions.android.siigmobile.geocoding.GeoCodingSearchView;
import it.geosolutions.android.siigmobile.geocoding.GeoCodingTask;
import it.geosolutions.android.siigmobile.geocoding.IGeoCoder;
import it.geosolutions.android.siigmobile.geocoding.NominatimGeoCoder;
import it.geosolutions.android.siigmobile.legend.LegendAdapter;
import it.geosolutions.android.siigmobile.spatialite.DeleteUnsavedResultsTask;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;
import jsqlite.Database;
import it.geosolutions.android.siigmobile.legend.LegendAdapter;

public class MainActivity extends MapActivityBase
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    // default path for files
    private static File MAP_FILE;

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
    private static boolean usePIS = false;

    private MultiSourceOverlayManager layerManager;

    public AdvancedMapView mapView;
    public OverlayManager overlayManager;

    private final static int RESULT_REQUEST_CODE = 1234;
    private final static int COMPUTE_REQUEST_CODE = 2345;

    private String  user_edited_layer_title;
    private boolean user_layer_saveable = false;
    private boolean user_layer_clearable = false;
    
    private static int currentStyle = Config.DEFAULT_STYLE;// Start with "Rischio Totale" theme

    private ProgressDialog pd;
    private SimpleCursorAdapter searchViewAdapter;
    private IGeoCoder mGeoCoder;
    private GeoCodingTask mGeoCodingTask;
    private LegendAdapter legendAdapter;
    private TextView legendTitle;
    private BoundingBox geoCodingBoundingBox;
    private Marker geoCodingMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        NewRelic.withApplicationToken(
//                ""
//        ).start(this.getApplication());

        MapFilesProvider.setBaseDir(Config.BASE_DIR_NAME);
        MAP_FILE = MapFilesProvider.getBackgroundMapFile();

        checkDefaults();

        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mapView =  (AdvancedMapView) findViewById(R.id.advancedMapView);

        final ListView legendListview = (ListView) findViewById(R.id.legend_listview);
        legendTitle = (TextView) findViewById(R.id.legend_drawer_title);
        legendTitle.setText(getResources().getStringArray(R.array.drawer_items)[currentStyle]);
        legendAdapter = new LegendAdapter(getBaseContext());
        legendListview.setAdapter(legendAdapter);


        // If MAP_FILE does not exists, it will be null
        // MAP_FILE.exists() will always be true
        if(MAP_FILE == null){

            askForDownload();

        }else{
            //check if unsaved results need to be deleted
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            if(prefs.getLong(Config.PARAM_LAST_UNSAVED_DELETION,0l) < System.currentTimeMillis() + Config.UNSAVED_DELETION_INTERVAL){
                if(BuildConfig.DEBUG){
                    Log.d(TAG, "deleting unsaved results");
                }
                new DeleteUnsavedResultsTask(){
                    @Override
                    public void started() {
                        showProgress(getString(R.string.please_wait));
                    }

                    @Override
                    public void done() {
                        hideProgress();

                        SharedPreferences.Editor ed = prefs.edit();
                        ed.putLong(Config.PARAM_LAST_UNSAVED_DELETION, System.currentTimeMillis());
                        ed.apply();

                        setupMap();
                    }
                }.execute(getBaseContext());

            }else{

                setupMap();
            }
        }

        searchViewAdapter = new SimpleCursorAdapter(getBaseContext(),
                R.layout.suggestion_item,
                null,
                new String[] {Config.CURSOR_TITLE}, //the row of the cursor to use in the adapter entry view
                new int[] {R.id.suggestion_tv},//the view id in the layout file to be edited by the adapter
                0);

        //select here the geocoder implementation
        mGeoCoder = new NominatimGeoCoder();

    }

    private void setupMap() {

        if(MAP_FILE != null) {

            // Enforce Background and initial position
            MapPosition mp = mapView.getMapViewPosition().getMapPosition();
            if (mapView.getMapFile() == null || !mapView.getMapFile().exists()) {
                mapView.setMapFile(MAP_FILE);
                mp = new MapPosition(new GeoPoint(Config.INITIAL_LATITUDE, Config.INITIAL_LONGITUDE), (byte) Config.INITIAL_ZOOMLEVEL);
                mapView.getMapViewPosition().setMapPosition(mp);
            }

            if (mp.geoPoint.latitude == 0 && mp.geoPoint.longitude == 0) {
                mp = new MapPosition(new GeoPoint(Config.INITIAL_LATITUDE, Config.INITIAL_LONGITUDE), (byte) Config.INITIAL_ZOOMLEVEL);
                mapView.getMapViewPosition().setMapPosition(mp);
            }

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

            loadDBLayers(null);

        }else{

            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.map_config_error))
                    .setPositiveButton(getString(R.string.ok_string), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();

        }
    }

    /**
     * loads spatialite layers (database files) from the apps folder
     * if @param layerToCenter is not null and among the loaded layers
     * the bounding box of this layer is calculated and the mapview centered on it
     */
    private void loadDBLayers(final String layerToCenter) {

        ArrayList<Layer> layers = new ArrayList<>();

        if(layerToCenter == null || currentStyle == 0) {

            /**
             * Adding WMS layers
             */
            WMSSource destinationSource = new WMSSource(Config.DESTINATION_WMS_URL);

            // Authorization Headers
            Headers.Builder hbuilder = new Headers.Builder();
            hbuilder.add("Authorization", Config.DESTINATION_AUTHORIZATION);
            destinationSource.setHeaders(hbuilder.build());

            WMSLayer layer = new WMSLayer(destinationSource, Config.WMS_LAYERS[
                    (currentStyle >= Config.WMS_LAYERS.length ? Config.DEFAULT_STYLE:currentStyle)
                    ]
            );
            //layer.setTitle("Rischio Totale");
            //layer.setGroup(mlayer.group);
            layer.setVisibility(true);
            //TODO Now skip tiled option
            layer.setTiled(false);
            //create base parameters
            HashMap<String, String> baseParams = new HashMap<String, String>();
            baseParams.put("format", "image/png8");
            //if(mlayer.styles != null) baseParams.put("styles",mlayer.styles );
            //if(mlayer.buffer != null) baseParams.put("buffer",mlayer.buffer.toString() );
            baseParams.put("transparent", "true");

            // Need additional parameters
            if (currentStyle > 0) {
                baseParams.put("ENV", Config.WMS_ENV[currentStyle]);
                baseParams.put("RISKPANEL", Config.WMS_RISKPANEL[currentStyle]);
                baseParams.put("DEFAULTENV", Config.WMS_DEFAULTENV[currentStyle]);
            }
            layer.setBaseParams(baseParams);

            layers.add(layer);

        }

        if(layerToCenter != null) {

            if (BuildConfig.DEBUG) {
                Log.i(TAG, "selected result arrived " + layerToCenter);
            }

            // Add Result Layer
            MSMMap mapConfig = SpatialDbUtils.mapFromDb(true);
            for (Layer l : mapConfig.layers) {
                if (layerToCenter.equals(l.getTitle())) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Adding Layer : " + l.getTitle());
                    }
                    layers.add(l);
                }
            }

        }


        // Set the styles
        for(Layer l : layers){
            if (l instanceof SpatialiteLayer) {
                if(BuildConfig.DEBUG) {
                    Log.d(TAG, "Setting Style for layer: " + l.getTitle());
                }

                if(l.getTitle().startsWith(Config.RESULT_PREFIX)){

                    ((SpatialiteLayer) l).setStyleFileName(Config.RESULT_STYLES[resultModeForRiskMode()]);

                    if (layerToCenter != null && l.getTitle().equals(layerToCenter)) {

//                        final BoundingBox bb = SpatialiteUtils.getBoundingBoxForSpatialiteTable(getBaseContext(), layerToCenter);
//
//                        if (bb != null) {
//
//                            mapView.getMapViewPosition().setCenter(bb.getCenterPoint());
//                        }

                        break; //stop looping, only this result layer interests

                    }
                }else{

                    ((SpatialiteLayer) l).setStyleFileName(l.getTitle().replace(Config.LAYERS_PREFIX, Config.STYLES_PREFIX_ARRAY[currentStyle]));
                }
            }
        }

        layerManager.setLayers(layers);

        // Update the Legend Panel
        StyleManager styleManager = StyleManager.getInstance();
        AdvancedStyle legendStyle = usePIS
                ? styleManager.getStyle(Config.RESULT_STYLES[3])
                : styleManager.getStyle(Config.STYLES_PREFIX_ARRAY[currentStyle] + "_1");
        legendAdapter.applyStyle(legendStyle);
        if(usePIS){
            legendTitle.setText(getResources().getString(R.string.pis_title));
        }else {
            legendTitle.setText(getResources().getStringArray(R.array.drawer_items)[currentStyle]);
        }

        mapView.redraw();

    }
    //TODO get the result mode from the result itself
    public int resultModeForRiskMode(){

        if(usePIS){
            return 3;
        }

        switch (currentStyle){
            case 0:
            case 3:
                return 2;
            case 1:
                return 0;
            case 2:
                return 1;
            case 4:
                return 3;
            default:
                return 2;
        }
    }

    private void askForDownload() {
        File external_storage = Environment.getExternalStorageDirectory();

        if(external_storage.getFreeSpace() < Config.REQUIRED_SPACE){

            new AlertDialog.Builder(this)
                .setMessage(getString(R.string.not_enough_space))
                .setPositiveButton(getString(R.string.ok_string), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                    .create().show();
            return;
        }


        String dir_path = external_storage.getPath();
        ZipFileManager zfm = new ZipFileManager(
                this,
                dir_path,
                MapFilesProvider.getBaseDir(),
                Config.BASE_PACKAGE_URL,
                getString(R.string.download_base_data_title),
                getString(R.string.download_base_data)) {

            // TODO: This method is badly named, it should be a post-execute callback
            @Override
            public void launchMainActivity(boolean success) {
                if(success){
                    // Update the map file reference
                    MAP_FILE = MapFilesProvider.getBackgroundMapFile();
                    // Reconfigure the map
                    setupMap();
                }
            }
        };
    }

    /**
     * Check if the necessary options are set and eventually set the default values
     */
    private void checkDefaults() {

        //MAP_FILE is the file, can be null

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor ed = prefs.edit();
        if("not_found".equals(prefs.getString(MapView.MAPSFORGE_BACKGROUND_RENDERER_TYPE, "not_found"))){
            ed.putString(MapView.MAPSFORGE_BACKGROUND_RENDERER_TYPE, "0");
        }
        if(MAP_FILE != null && prefs.getString(MapView.MAPSFORGE_BACKGROUND_FILEPATH, null) == null){
            ed.putString(MapView.MAPSFORGE_BACKGROUND_FILEPATH, MAP_FILE.getAbsolutePath());
        }
        ed.commit();

        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        SharedPreferences.Editor mapPrefEditor = sharedPreferences.edit();

        if(!containsMapViewPosition(sharedPreferences)){
            mapPrefEditor.putFloat(KEY_LATITUDE, Config.INITIAL_LATITUDE);
            mapPrefEditor.putFloat(KEY_LONGITUDE, Config.INITIAL_LONGITUDE);
            mapPrefEditor.putInt(KEY_ZOOM_LEVEL, Config.INITIAL_ZOOMLEVEL);
        }
        mapPrefEditor.commit();
        //MapFilesProvider.setBackgroundFileName(MAP_FILE.getName());
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        // Update Title
        onSectionAttached(position);

        // update the main content by replacing fragments
        if(layerManager == null && position < 4){
            // nothing to do
            return;
        }

        if(usePIS){
            currentStyle = 4;
            switch (position){
                case 0:
                    currentStyle = position;
                case 1:
                    //reload, if an elaboration arrived center on it
                    loadDBLayers(user_edited_layer_title);
                    break;
                case 2:

                    final BoundingBox bb = mapView.getMapViewPosition().getBoundingBox();
                    // Start the form activity
                    Intent formIntent = new Intent(this, ComputeFormActivity.class);
                    formIntent.putExtra(ComputeFormActivity.PARAM_BOUNDINGBOX, bb);

                    final boolean isPolygonRequest = mapView.getMapViewPosition().getZoomLevel() <= 13;

                    formIntent.putExtra(ComputeFormActivity.PARAM_POLYGON, isPolygonRequest);

                    startActivityForResult(formIntent, COMPUTE_REQUEST_CODE);
                    break;
                case 3:
                    Intent resultsIntent = new Intent(this, LoadResultsActivity.class);
                    startActivityForResult(resultsIntent, RESULT_REQUEST_CODE);
                    break;
                default:
                    break;
            }
            return;
        }
        switch (position){
            case 0:
            case 1:
            case 2:
            case 3:
                currentStyle = position;
                //reload, if an elaboration arrived center on it
                loadDBLayers(user_edited_layer_title);
                break;
            case 4:

                final BoundingBox bb = mapView.getMapViewPosition().getBoundingBox();
                // Start the form activity
                Intent formIntent = new Intent(this, ComputeFormActivity.class);
                formIntent.putExtra(ComputeFormActivity.PARAM_BOUNDINGBOX, bb);

                final boolean isPolygonRequest = mapView.getMapViewPosition().getZoomLevel() <= 13;

                formIntent.putExtra(ComputeFormActivity.PARAM_POLYGON, isPolygonRequest);

                startActivityForResult(formIntent, COMPUTE_REQUEST_CODE);
                break;
           case 5:
               Intent resultsIntent = new Intent(this, LoadResultsActivity.class);
               startActivityForResult(resultsIntent, RESULT_REQUEST_CODE);
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

        mTitle = getResources().getStringArray(R.array.drawer_items)[number];

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
            if(user_layer_saveable ){
                getMenuInflater().inflate(R.menu.saveable, menu);
            }
            if(user_layer_clearable){
                getMenuInflater().inflate(R.menu.clearable, menu);
            }
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();

            final MenuItem search = menu.findItem(R.id.action_search);
            final GeoCodingSearchView geoCodingSearchView = (GeoCodingSearchView) MenuItemCompat.getActionView(search);

            geoCodingSearchView.setSuggestionsAdapter(searchViewAdapter);
            geoCodingSearchView.setCallback(new GeoCodingSearchView.GeoCodingCallback() {
                @Override
                public void doQuery(String query) {

                    if(Util.isOnline(getBaseContext())){

                        populateAdapter(query);

                    }else{
                        Toast.makeText(getBaseContext(),getString(R.string.not_online),Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void suggestionSelected(int position) {

                    final Cursor cur = searchViewAdapter.getCursor();
                    cur.moveToPosition(position);

                    final String title = cur.getString(cur.getColumnIndex(Config.CURSOR_TITLE));
                    final String lat   = cur.getString(cur.getColumnIndex(Config.CURSOR_LAT));
                    final String lon   = cur.getString(cur.getColumnIndex(Config.CURSOR_LON));

                    if(BuildConfig.DEBUG) {
                        Log.i(TAG, "selected " + title + " lat : " + lat + " lon " + lon);
                    }

                    final double dLat = Double.parseDouble(lat);
                    final double dLon = Double.parseDouble(lon);

                    final GeoPoint p = new GeoPoint(dLat, dLon);

                    try {
                        //if mapview covers, center on place, zoom in and collapse search view
                        if( mapView.getMapDatabase().hasOpenFile() && (mapView.getMapDatabase().getMapFileInfo().boundingBox.contains(p))) {

                            createOrUpdateGeoCodingMarker(p);

                            mapView.getMapViewPosition().setCenter(p);

                            mapView.getMapViewPosition().setZoomLevel(Config.PLACE_SELECTED_ZOOM_TO_LEVEL);

                            mapView.redraw();

                            //close keyboard collapse actionview
                            MenuItemCompat.collapseActionView(search);
                        } else {
                            //the map file does not cover, inform user, leave search view expanded
                            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(getString(R.string.app_name))
                                    .setMessage(getString(R.string.place_not_covered))
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            dialog.dismiss();
                                        }
                                    })
                                    .create();
                             dialog.show();
                        }
                    }catch(IllegalArgumentException e){
                        Log.e(TAG," error checking the map views bounds, is no map file open ?",e);
                    }
                }
            });

            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * populates the adapter of the searchview with the result of a GeoCoding request
     *
     * @param query the text to use in the geocoding request
     */
    private void populateAdapter(final String query) {

        if(mGeoCodingTask != null){
            mGeoCodingTask.cancel(true);
        }

        mGeoCodingTask = new GeoCodingTask(mGeoCoder, query, getGeoCodingBoundingBox()) {
            @Override
            public void done(Cursor cursor) {

                searchViewAdapter.changeCursor(cursor);
            }
        };
        mGeoCodingTask.execute();
    }

    /**
     * @return the bounding box of the current map file if available otherwise the bounding box containing the whole world
     */
    public BoundingBox getGeoCodingBoundingBox()
    {
        if(geoCodingBoundingBox == null){
            if(mapView.getMapDatabase() != null &&
                    mapView.getMapDatabase().getMapFileInfo() != null &&
                    mapView.getMapDatabase().getMapFileInfo().boundingBox != null &&
                    mapView.getMapDatabase().hasOpenFile()){
                geoCodingBoundingBox = mapView.getMapDatabase().getMapFileInfo().boundingBox;
            }else{
                geoCodingBoundingBox = new BoundingBox(Config.LATITUDE_MIN,Config.LONGITUDE_MIN,Config.LATITUDE_MAX,Config.LONGITUDE_MAX);
            }

        }
        return geoCodingBoundingBox;
    }

    /**
     * creates or updates a marker inside a marker overlay
     * @param p the GeoPoint to create the marker with / center it
     */
    public void createOrUpdateGeoCodingMarker(final GeoPoint p) {

        if(geoCodingMarker == null){

            geoCodingMarker = new Marker(p, Marker.boundCenterBottom(getResources().getDrawable(R.drawable.pin_red)));

            //This is the default Mapsforge 0.3.x way to create and add an overlay
            final ListOverlay overlay = new ListOverlay();
            List<OverlayItem> overlayItems = overlay.getOverlayItems();

            overlayItems.add(geoCodingMarker);

            mapView.getOverlays().add(overlay);
        }   else{

            geoCodingMarker.setGeoPoint(p);
        }

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
        } else if (id == R.id.action_save) {

            showEditElaborationTitleAndDescriptionDialog();
        } else if (id == R.id.action_clear){

            if(usePIS){
                usePIS = false;
                currentStyle = Config.DEFAULT_STYLE;
                mNavigationDrawerFragment.setEntries(getResources().getStringArray(R.array.drawer_items));
            }
            clearMenu();

            loadDBLayers(null);
        }

        return super.onOptionsItemSelected(item);
    }

    public void showEditElaborationTitleAndDescriptionDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.elab_dialog_title));

        LayoutInflater inflater = LayoutInflater.from(getBaseContext());

        final View editView = inflater.inflate(R.layout.enter_title_layout, null);

        builder.setView(editView);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        final EditText title_ed = (EditText) editView.findViewById(R.id.title_field);
                        final EditText desc_ed = (EditText) editView.findViewById(R.id.description_field);

                        final String title = title_ed.getText().toString();
                        final String desc = desc_ed.getText().toString();

                        if (TextUtils.isEmpty(title)) {

                            Toast.makeText(getBaseContext(), getString(R.string.elab_enter_title), Toast.LENGTH_SHORT).show();
                            dialog.dismiss();//release

                            //reshow
                            showEditElaborationTitleAndDescriptionDialog();
                            return;
                        }

                        new AsyncTask<Void,Void,Boolean>() {
                            private ProgressDialog pd;
                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                pd = new ProgressDialog(MainActivity.this, ProgressDialog.STYLE_SPINNER);
                                pd.setMessage(getBaseContext().getString(R.string.saving_source));
                                pd.setCancelable(false);
                                pd.setIcon(R.drawable.ic_launcher);
                                pd.show();
                            }

                            @Override
                            protected Boolean doInBackground(Void... params) {

                                final Database db = SpatialiteUtils.openSpatialiteDB(getBaseContext());
                                final boolean result = SpatialiteUtils.updateNameTableWithUserData(db, user_edited_layer_title, title,desc);

                                try {
                                    db.close();
                                }catch (jsqlite.Exception e) {
                                    Log.e(TAG, "exception closing db",e);
                                }
                                return result;
                            }

                            @Override
                            protected void onPostExecute(Boolean success) {
                                super.onPostExecute(success);

                                if(pd != null && pd.isShowing()) {
                                    pd.dismiss();
                                }

                                //user saved, remove save button
                                if(success) {
                                    clearMenu();
                                }

                                //TODO saved, now what switch back to all risks ? -> loadDBLayers()

                                Toast.makeText(getBaseContext(), success ? getString(R.string.elab_success) : getString(R.string.elab_failure),Toast.LENGTH_SHORT).show();

                            }
                        }.execute();



                        dialog.dismiss();
                    }
                }

        );
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()

                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();

                    }
                }

        );

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Controls can be refreshed getting the result of an intent, in this case
        // each control knows which intent he sent with their requestCode/resultCode
        for (MapControl control : mapView.getControls()) {
            control.refreshControl(requestCode, resultCode, data);
        }

        // The user still don't have the data, ask for download
        if (MAP_FILE == null) {
            askForDownload();
        }

        //Result of click on a local elaboration
        if ((requestCode == RESULT_REQUEST_CODE  || requestCode == COMPUTE_REQUEST_CODE ) && resultCode == RESULT_OK) {

            final String tableName = data.getStringExtra(Config.RESULT_TABLENAME);
            Integer formulaType = data.getIntExtra(Config.RESULT_FORMULA, Config.FORMULA_RISK);
            if(formulaType == Config.FORMULA_STREET){
                usePIS = true;
                currentStyle = 4;
                mNavigationDrawerFragment.setEntries(new String[]{
                                getResources().getStringArray(R.array.drawer_items)[0],
                                getString(R.string.pis_title),
                                getResources().getStringArray(R.array.drawer_items)[4],
                                getResources().getStringArray(R.array.drawer_items)[5]
                        }
                );
            }else{
                if(usePIS){
                    usePIS = false;
                    currentStyle = Config.DEFAULT_STYLE;
                    mNavigationDrawerFragment.setEntries(getResources().getStringArray(R.array.drawer_items));
                }
            }

            if (tableName != null) {

                loadDBLayers(tableName);

            }

            if (requestCode == COMPUTE_REQUEST_CODE && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.elab_dialog_title));
            }

            invalidateMenu(tableName, true, true);
        }
    }


    /**
     * removes the save button from the toolbar
     */
    public void invalidateMenu(final String user_edited_table, final boolean saveable, final boolean clearable){

        user_edited_layer_title = user_edited_table;

        user_layer_saveable  = saveable;
        user_layer_clearable = clearable;

        invalidateOptionsMenu();
    }

    public void clearMenu(){

        invalidateMenu(null, false, false);

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
}
