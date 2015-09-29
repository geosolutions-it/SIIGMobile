package it.geosolutions.android.siigmobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
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

import com.google.gson.Gson;
import com.newrelic.agent.android.NewRelic;
import com.squareup.okhttp.Headers;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.Circle;
import org.mapsforge.android.maps.overlay.ListOverlay;
import org.mapsforge.android.maps.overlay.Marker;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import it.geosolutions.android.map.activities.GetFeatureInfoAttributeActivity;
import it.geosolutions.android.map.activities.MapActivityBase;
import it.geosolutions.android.map.control.LocationControl;
import it.geosolutions.android.map.control.MapControl;
import it.geosolutions.android.map.control.MapInfoControl;
import it.geosolutions.android.map.model.Layer;
import it.geosolutions.android.map.model.MSMMap;
import it.geosolutions.android.map.model.query.WMSGetFeatureInfoQuery;
import it.geosolutions.android.map.overlay.managers.MultiSourceOverlayManager;
import it.geosolutions.android.map.overlay.managers.OverlayManager;
import it.geosolutions.android.map.spatialite.SpatialiteLayer;
import it.geosolutions.android.map.style.AdvancedStyle;
import it.geosolutions.android.map.style.StyleManager;
import it.geosolutions.android.map.utils.MapFilesProvider;
import it.geosolutions.android.map.utils.ProjectionUtils;
import it.geosolutions.android.map.utils.SpatialDbUtils;
import it.geosolutions.android.map.utils.ZipFileManager;
import it.geosolutions.android.map.view.AdvancedMapView;
import it.geosolutions.android.map.wms.GetFeatureInfoConfiguration;
import it.geosolutions.android.map.wms.WMSLayer;
import it.geosolutions.android.map.wms.WMSSource;
import it.geosolutions.android.siigmobile.elaboration.ElaborationResult;
import it.geosolutions.android.siigmobile.geocoding.GeoCodingSearchView;
import it.geosolutions.android.siigmobile.geocoding.GeoCodingTask;
import it.geosolutions.android.siigmobile.geocoding.IGeoCoder;
import it.geosolutions.android.siigmobile.geocoding.NominatimGeoCoder;
import it.geosolutions.android.siigmobile.legend.LegendAdapter;
import it.geosolutions.android.siigmobile.mapcontrol.FixedShapeMapInfoControl;
import it.geosolutions.android.siigmobile.spatialite.DeleteUnsavedResultsTask;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;

public class MainActivity extends MapActivityBase
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    // default path for files
    private static File MAP_FILE;

    /**
     * Tag for Logging
     */
    private static final String TAG = "MainActivity";
    private static final String KEY_RESULT = "ELABORATION_RESULT";

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

    private final static int RESULT_REQUEST_CODE = 1234;

    private boolean user_layer_clearable = false;

    private static int currentStyle = Config.DEFAULT_STYLE;// Start with "Populazione Residenziale" theme

    private ProgressDialog pd;
    private SimpleCursorAdapter searchViewAdapter;
    private IGeoCoder mGeoCoder;
    private GeoCodingTask mGeoCodingTask;
    private LegendAdapter legendAdapter;
    private TextView legendTitle;
    private BoundingBox geoCodingBoundingBox;
    private Marker geoCodingMarker;
    private ElaborationResult elaborationResult;
    private FixedShapeMapInfoControl invisibleLocationAcquireControl;

    private Toolbar mToolbar;
    private ListOverlay overlayItemList;
    private Circle overlayCircle;
    private Paint circlePaint;

    private MapControl mapInfoControl;
    private enum MapInfoSelectionMode {
        GetFeatureInfo,
        GetSpatialiteInfo
    }
    private MapInfoSelectionMode mapInfoSelectionMode = MapInfoSelectionMode.GetFeatureInfo;

    private GetFeatureInfoConfiguration getFeatureInfoConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!BuildConfig.NEWRELIC_APPLICATION_TOKEN.isEmpty()) {

            if(BuildConfig.DEBUG){
                Log.v(TAG, "NewRelic App Token: "+ BuildConfig.NEWRELIC_APPLICATION_TOKEN);
            }

            // Start NewRelic monitoring
            NewRelic.withApplicationToken(
                    BuildConfig.NEWRELIC_APPLICATION_TOKEN
            ).start(this.getApplication());

        }else{
            if(BuildConfig.DEBUG){
                Log.v(TAG, "NewRelic App Token not found");
            }
        }

        MapFilesProvider.setBaseDir(Config.BASE_DIR_NAME);
        MAP_FILE = MapFilesProvider.getBackgroundMapFile();

        checkDefaults();

        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.m_toolbar);
        setSupportActionBar(mToolbar);

        mToolbar.setTitleTextColor(getResources().getColor(R.color.white));

        // Inflate a menu to be displayed in the toolbar
        mToolbar.inflateMenu(R.menu.main);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                int id = item.getItemId();

                if (id == R.id.action_clear) {

                    clearMenu();
                    currentStyle = Config.DEFAULT_STYLE;

                    loadDBLayers(null);
                    removeOverlayItem(overlayCircle);
                    mTitle = getResources().getStringArray(R.array.drawer_items)[currentStyle];

                    return true;
                }
                return false;
            }
        });

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

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
            //mapView.addControl(new CoordinateControl(mapView, true));

            // Info Control
            setupMapInfoControl();

            // Location Control
            LocationControl lc  =new LocationControl(mapView);
            lc.setActivationButton((ImageButton) findViewById(R.id.ButtonLocation));
            mapView.addControl(lc);

            if (elaborationResult == null) {
                loadDBLayers(null);
            } else {
                loadDBLayers(elaborationResult.getResultTableName());
                invalidateMenu(elaborationResult.getResultTableName(), true);
            }

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

    private void setupMapInfoControl(){

        if(mapInfoControl != null){
            mapView.removeControl(mapInfoControl);
            mapInfoControl = null;
        }

        switch (mapInfoSelectionMode){
            case GetFeatureInfo:

                //WMS Info control
                mapInfoControl = FixedShapeMapInfoControl.createOnePointControl(
                        mapView,
                        this,
                        R.id.ButtonInfo,
                        null,
                        new FixedShapeMapInfoControl.OnePointSelectionCallback() {
                            @Override
                            public void pointSelected(double lat, double lon, float pixel_x, float pixel_y, double radius, byte zoomLevel) {

                                final String wfsLayer = Config.WFS_LAYERS[currentStyle];
                                final String wmsLayer = Config.WMS_LAYERS[currentStyle];

                                long[] mapSize;
                                synchronized (mapView.getProjection()) {
                                    mapSize = ProjectionUtils.calculateMapSize(mapView.getMeasuredWidth(), mapView.getMeasuredHeight(), mapView.getProjection());
                                }

                                final BoundingBox boundingBox = mapView.getMapViewPosition().getBoundingBox();
                                final String version = Config.WMS_GETINFO_VERSION;
                                final String crs = String.format(Locale.US, "EPSG:%d", Config.WMS_GETINFO_EPSG);

                                //check if the user language is supported
                                final String local_code = Locale.getDefault().getLanguage();
                                boolean supported = false;
                                for (String locale : Config.DESTINATION_SUPPORTED_LANGUAGES) {
                                    if (local_code.toLowerCase().equals(locale)) {
                                        supported = true;
                                        break;
                                    }
                                }
                                final String env = String.format(Locale.US, "locale:%s", supported ? local_code : Config.DESTINATION_SUPPORTED_LANGUAGES[0]);

                                final HashMap<String, String> additionalParameters = new HashMap<>();

                                additionalParameters.put(Config.WMS_GETINFO_PARAMETER_FEATURE_COUNT, String.valueOf(Config.WMS_INFO_FEATURE_COUNT));
                                additionalParameters.put(Config.WMS_GETINFO_PARAMETER_FORMAT, Config.WMS_GETINFO_FORMAT);
                                additionalParameters.put(Config.WMS_GETINFO_PARAMETER_LOCALE, env);

                                Intent i = new Intent(MainActivity.this, GetFeatureInfoAttributeActivity.class);

                                WMSGetFeatureInfoQuery query = new WMSGetFeatureInfoQuery();
                                query.setLat(lat);
                                query.setLon(lon);
                                query.setPixel_X(pixel_x);
                                query.setPixel_Y(pixel_y);
                                query.setZoomLevel(zoomLevel);
                                query.setMapSize(mapSize);
                                query.setBbox(boundingBox);
                                query.setLayers(new String[]{wmsLayer});
                                query.setQueryLayers(new String[]{wfsLayer});
                                query.setVersion(version);
                                query.setCrs(crs);
                                query.setLocale(env);
                                query.setStyles("");
                                query.setAuthToken(Config.DESTINATION_AUTHORIZATION);
                                query.setAdditionalParams(additionalParameters);
                                query.setEndPoint(Config.DESTINATION_WMS_URL);

                                if (getFeatureInfoConfiguration() != null) {
                                    query.setLocaleConfig(getFeatureInfoConfiguration().getLocaleForLanguageCode(local_code));
                                }
                                i.putExtra("query", query);
                                i.setAction(Intent.ACTION_VIEW);
                                startActivityForResult(i, GetFeatureInfoAttributeActivity.GET_ITEM);

                                if (mapInfoControl != null) {
                                    mapInfoControl.disable();
                                    if (mapInfoControl.getActivationButton() != null) {
                                        mapInfoControl.getActivationButton().setSelected(false);
                                    }
                                }

                            }
                        });


                break;
            case GetSpatialiteInfo:
                // Info Control
                mapInfoControl = new MapInfoControl(mapView,this);
                mapInfoControl.setActivationButton((ImageButton) findViewById(R.id.ButtonInfo));
                mapInfoControl.setMode(MapControl.MODE_VIEW);
                break;
        }

        mapView.addControl(mapInfoControl);
    }

    /**
     * loads spatialite layers (database files) from the apps folder
     * if @param layerToCenter is not null and among the loaded layers
     * the bounding box of this layer is calculated and the mapview centered on it
     */
    private void loadDBLayers(final String layerToCenter) {

        ArrayList<Layer> layers = new ArrayList<>();

        if (layerToCenter == null || currentStyle == 0) { //wms mode

            if (mapInfoSelectionMode != MapInfoSelectionMode.GetFeatureInfo) {
                mapInfoSelectionMode = MapInfoSelectionMode.GetFeatureInfo;
                setupMapInfoControl();
            }

            /**
             * Adding WMS layers
             */
            WMSSource destinationSource = new WMSSource(Config.DESTINATION_WMS_URL);

            // Authorization Headers
            Headers.Builder hbuilder = new Headers.Builder();
            hbuilder.add("Authorization", Config.DESTINATION_AUTHORIZATION);
            destinationSource.setHeaders(hbuilder.build());

            WMSLayer layer = new WMSLayer(destinationSource, Config.WMS_LAYERS[
                    (currentStyle >= Config.WMS_LAYERS.length ? Config.DEFAULT_STYLE : currentStyle)
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
                baseParams.put("ENV", Config.WMS_ENV[(currentStyle >= Config.WMS_ENV.length ? Config.DEFAULT_STYLE : currentStyle)]);
                baseParams.put("RISKPANEL", Config.WMS_RISKPANEL[(currentStyle >= Config.WMS_RISKPANEL.length ? Config.DEFAULT_STYLE : currentStyle)]);
                baseParams.put("DEFAULTENV", Config.WMS_DEFAULTENV[(currentStyle >= Config.WMS_DEFAULTENV.length ? Config.DEFAULT_STYLE : currentStyle)]);
            }
            layer.setBaseParams(baseParams);

            layers.add(layer);

        }

        if (layerToCenter != null) {//result mode

            if(mapInfoSelectionMode != MapInfoSelectionMode.GetSpatialiteInfo) {
                mapInfoSelectionMode = MapInfoSelectionMode.GetSpatialiteInfo;
                setupMapInfoControl();
            }

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
        for (Layer l : layers) {
            if (l instanceof SpatialiteLayer) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Setting Style for layer: " + l.getTitle());
                }
                //TODO reconfigure for val danno results
                if (l.getTitle().startsWith(Config.RESULT_PREFIX)) {

                    ((SpatialiteLayer) l).setStyleFileName(Config.RESULT_STYLES[resultModeForRiskMode()]);

                    if (layerToCenter != null && l.getTitle().equals(layerToCenter)) {

                        break; //stop looping, only this result layer interests

                    }
                } else {

                    ((SpatialiteLayer) l).setStyleFileName(l.getTitle().replace(Config.LAYERS_PREFIX, Config.STYLES_PREFIX_ARRAY[currentStyle]));
                }
            }
        }

        layerManager.setLayers(layers);

        // Update the Legend Panel
        StyleManager styleManager = StyleManager.getInstance();

        AdvancedStyle legendStyle = null;
        if (currentStyle == 0){
            legendStyle = styleManager.getStyle("grafo_1");
        }else if( currentStyle < getResources().getStringArray(R.array.drawer_items).length - 2) { //WMS Layers
            legendStyle = styleManager.getStyle(Config.WFS_LAYERS[currentStyle]);
        }else{
            //TODO which style for result layers ?
        }
        //TODO remove this limitation when result styles available
        if(currentStyle < getResources().getStringArray(R.array.drawer_items).length - 2) {
            legendAdapter.applyStyle(legendStyle);

            legendTitle.setText(getResources().getStringArray(R.array.drawer_items)[currentStyle]);
        }

        mapView.redraw();

    }
    //TODO get the result mode from the result itself
    public int resultModeForRiskMode(){

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


        if(position == getResources().getStringArray(R.array.drawer_items).length - 2){ // valutazione danno

            // creates a mapInfoControl which is not tied to an imageview but "invisible"
            invisibleLocationAcquireControl = FixedShapeMapInfoControl.createOnePointControl(
                    mapView,
                    this,
                    FixedShapeMapInfoControl.DONT_CONNECT,
                    getString(R.string.touch_point),
                    new FixedShapeMapInfoControl.OnePointSelectionCallback() {
                @Override
                public void pointSelected(double lat, double lon, float pixel_x, float pixel_y, double radius, byte zoomLevel) {

                    mapView.removeControl(invisibleLocationAcquireControl);
                    invisibleLocationAcquireControl = null;

                    final GeoPoint p = new GeoPoint(lat,lon);

                    Intent i = new Intent(MainActivity.this, ComputeFormActivity.class);
                    i.putExtra(ComputeFormActivity.PARAM_POINT, p);

                    startActivityForResult(i, RESULT_REQUEST_CODE);

                }
            });
            //it is enabled by code
            invisibleLocationAcquireControl.setEnabled(true);


        }else if(position == getResources().getStringArray(R.array.drawer_items).length - 1){ // carica elaborazione
          
            Intent resultsIntent = new Intent(this, LoadResultsActivity.class);
            startActivityForResult(resultsIntent, RESULT_REQUEST_CODE);

        }else{ //WMS Layer
            currentStyle = position;
            //reload, if an elaboration arrived center on it
            if(elaborationResult == null){
                loadDBLayers(null);
            } else {
                loadDBLayers(elaborationResult.getResultTableName());
            }
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
            if (user_layer_clearable) {
                getMenuInflater().inflate(R.menu.clearable, menu);
            }

            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();

            final MenuItem search = menu.findItem(R.id.action_search);
            final GeoCodingSearchView geoCodingSearchView = (GeoCodingSearchView) MenuItemCompat.getActionView(search);

            EditText ed = (EditText) geoCodingSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
            ed.setHintTextColor(getResources().getColor(R.color.toolbargrey));
            ed.setTextColor(getResources().getColor(R.color.black));

            //arrow in the toolbar

            MenuItemCompat.setOnActionExpandListener(search, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {

                    Log.i(TAG, "expand");
                    mToolbar.setBackgroundColor(getResources().getColor(R.color.white));

                    //ToolbarColorizeHelper.colorizeToolbar(mToolbar, getResources().getColor(R.color.black),MainActivity.this);

                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {

                    Log.i(TAG, "collapse");

                    mToolbar.setBackgroundColor(getResources().getColor(R.color.primary));

                    //ToolbarColorizeHelper.colorizeToolbar(mToolbar, getResources().getColor(R.color.white),MainActivity.this);

                    return true;
                }
            });

            geoCodingSearchView.setSuggestionsAdapter(searchViewAdapter);
            geoCodingSearchView.setCallback(new GeoCodingSearchView.GeoCodingCallback() {
                @Override
                public void doQuery(String query) {

                    if (Util.isOnline(getBaseContext())) {

                        populateAdapter(query);

                    } else {
                        Toast.makeText(getBaseContext(), getString(R.string.not_online), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void suggestionSelected(int position) {

                    final Cursor cur = searchViewAdapter.getCursor();
                    cur.moveToPosition(position);

                    final String title = cur.getString(cur.getColumnIndex(Config.CURSOR_TITLE));
                    final String lat = cur.getString(cur.getColumnIndex(Config.CURSOR_LAT));
                    final String lon = cur.getString(cur.getColumnIndex(Config.CURSOR_LON));

                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "selected " + title + " lat : " + lat + " lon " + lon);
                    }

                    final double dLat = Double.parseDouble(lat);
                    final double dLon = Double.parseDouble(lon);

                    final GeoPoint p = new GeoPoint(dLat, dLon);

                    try {
                        //if mapview covers, center on place, zoom in and collapse search view
                        if (getGeoCodingBoundingBox().contains(p)) {

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
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, " error checking the map views bounds, is no map file open ?", e);
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
            try{
                if( mapView.getMapDatabase() != null &&
                    mapView.getMapDatabase().hasOpenFile() &&
                    mapView.getMapDatabase().getMapFileInfo() != null &&
                    mapView.getMapDatabase().getMapFileInfo().boundingBox != null )
                {
                    geoCodingBoundingBox = mapView.getMapDatabase().getMapFileInfo().boundingBox;
                }else{
                    geoCodingBoundingBox = new BoundingBox(Config.LATITUDE_MIN,Config.LONGITUDE_MIN,Config.LATITUDE_MAX,Config.LONGITUDE_MAX);
                }
            }catch (IllegalStateException ise){
                // The map is not loaded
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

            geoCodingMarker = new Marker(p, Marker.boundCenterBottom(ContextCompat.getDrawable(getBaseContext(), R.drawable.pin_red)));

            //This is the default Mapsforge 0.3.x way to create and add an overlay
            if(overlayItemList == null){
                overlayItemList = new ListOverlay();
                mapView.getOverlays().add(overlayItemList);
            }
            List<OverlayItem> overlayItems = overlayItemList.getOverlayItems();

            overlayItems.add(geoCodingMarker);


        }   else {

            geoCodingMarker.setGeoPoint(p);
        }
    }

    /**
     * adds a circle overlay to the map at @param p with radius @param radius
     *
     * paint objects may be null, be sure to pass at least one to see the item on the map
     *
     * @param p the center point of the circle
     * @param radius the radius in meters for the circle
     * @param stroke the stroke paintof the circle (can be null)
     * @param fill the fill paint of the circle (can be null)
     */
    public void createOrUpdateCircleOverlay(final GeoPoint p, final int radius, final Paint stroke, final Paint fill) {


        if (overlayCircle == null) {

            overlayCircle = new Circle(p, radius, fill, stroke);

            if (overlayItemList == null) {
                overlayItemList = new ListOverlay();
                mapView.getOverlays().add(overlayItemList);
            }

            List<OverlayItem> overlayItems = overlayItemList.getOverlayItems();

            overlayItems.add(overlayCircle);
        } else {

            overlayCircle.setGeoPoint(p);
            overlayCircle.setRadius(radius);

            overlayCircle.setPaintFill(fill);
            overlayCircle.setPaintStroke(stroke);
        }

        mapView.redraw();
    }

    public Paint getCirclePaint() {

        if(circlePaint == null){
            circlePaint= new Paint(Paint.ANTI_ALIAS_FLAG);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setColor(Config.CIRCLE_OVERLAY_COLOR);
            circlePaint.setStrokeWidth(Config.CIRCLE_OVERLAY_STROKE);
        }
        return circlePaint;
    }

    /**
     * removes an item from the list of overlay items
     * @param item
     */
    public void removeOverlayItem(final OverlayItem item){

        if(overlayItemList != null){
            List<OverlayItem> overlayItems = overlayItemList.getOverlayItems();
            overlayItems.remove(item);
        }

        mapView.redraw();

    }

    public ListOverlay getOverlayItemList() {
        return overlayItemList;
    }


    /*
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();

            //noinspection SimplifiableIfStatement
    //        if (id == R.id.action_settings) {
    //            return true;
    //        } else
            if (id == R.id.action_clear) {

                clearMenu();
                currentStyle = Config.DEFAULT_STYLE;

                loadDBLayers(null);
            }

            return super.onOptionsItemSelected(item);
        }
    */



    public void showSnackBar(int stringResource){
        Snackbar
                .make(findViewById(R.id.snackbarPosition),
                        stringResource,
                        Snackbar.LENGTH_LONG)
                .show();
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
        if (requestCode == RESULT_REQUEST_CODE && resultCode == RESULT_OK) {

            //final String tableName = data.getStringExtra(Config.RESULT_TABLENAME);
            elaborationResult = (ElaborationResult) data.getSerializableExtra(Config.RESULT_ITEM);

            applyResult(elaborationResult);

        }
    }

    @Override
    public void onBackPressed() {

        boolean quit = true;
        for (MapControl control : mapView.getControls()) {

            if (control.isEnabled()) {
                control.disable();
                if (control.getActivationButton() != null) {
                    control.getActivationButton().setSelected(false);
                }
                if(control.equals(invisibleLocationAcquireControl)){
                    Toast.makeText(getBaseContext(), R.string.position_acquirement_cancelled, Toast.LENGTH_SHORT).show();
                }
                quit = false;
            }
        }
        if (quit) {
            super.onBackPressed();
        }
    }

    /**
     * loads the getfeature info configuration from res/raw
     * @return the GetFeatureInfoConfiguration or null if an error occurred
     */
    public GetFeatureInfoConfiguration getFeatureInfoConfiguration() {
        if(getFeatureInfoConfiguration == null){

            InputStream inputStream = getResources().openRawResource(R.raw.getfeatureinfo_config);
            if (inputStream != null) {
                final Gson gson = new Gson();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                getFeatureInfoConfiguration = gson.fromJson(reader, GetFeatureInfoConfiguration.class);

                if(getFeatureInfoConfiguration == null) {
                    Log.w(TAG, "error deserializing json ");
                }
            }

        }

        return getFeatureInfoConfiguration;
    }


    public void applyResult(ElaborationResult result){

        elaborationResult = result;

        loadDBLayers(result.getResultTableName());
        invalidateMenu(result.getResultTableName(), true);

        createOrUpdateCircleOverlay(elaborationResult.getLocation(), elaborationResult.getRadius(), getCirclePaint(), null);

        final BoundingBox bb = SpatialiteUtils.getBoundingBoxForSpatialiteTable(getBaseContext(), result.getResultTableName());

        if (bb != null) {
            mapView.getMapViewPosition().setCenter(bb.getCenterPoint());
        }
    }

    /**
     * removes the save button from the toolbar
     */
    public void invalidateMenu(final String tableName, final boolean clearable) {
        if(tableName == null){
            elaborationResult = null;
        }
        user_layer_clearable = clearable;

        invalidateOptionsMenu();

        if(mNavigationDrawerFragment != null){
            mNavigationDrawerFragment.setItemSelected(currentStyle);
        }

    }

    public void clearMenu() {

        invalidateMenu(null, false);
        if(mToolbar != null) {
            mToolbar.setBackgroundColor(getResources().getColor(R.color.primary));
        }

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

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        if(elaborationResult != null){
            outState.putSerializable(KEY_RESULT, elaborationResult);
        }

        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        elaborationResult = (ElaborationResult) savedInstanceState.getSerializable(KEY_RESULT);
    }
}
