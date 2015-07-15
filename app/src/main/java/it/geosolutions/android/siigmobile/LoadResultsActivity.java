package it.geosolutions.android.siigmobile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import it.geosolutions.android.map.database.SpatialDataSourceManager;
import it.geosolutions.android.map.utils.MapFilesProvider;
import it.geosolutions.android.siigmobile.spatialite.LoadResultsTask;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;
import jsqlite.Database;
import jsqlite.Exception;

/**
 * Created by Robert Oehler on 02.07.15.
 *
 */
public class LoadResultsActivity extends AppCompatActivity  implements ComputeNavigationDrawerFragment.NavigationDrawerCallbacks{

    private final static String TAG = LoadResultsActivity.class.getSimpleName();

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private ComputeNavigationDrawerFragment mNavigationDrawerFragment;

    private RiskResultAdapter rra;

    private ProgressDialog pd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.risk_result_layout);

        final ListView lv = (ListView) findViewById(R.id.result_lv);

        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        lv.setItemsCanFocus(false);
        lv.setEmptyView(findViewById(R.id.empty));

        rra = new RiskResultAdapter(getBaseContext(), new ArrayList<CRSFeatureCollection>());

        lv.setAdapter(rra);

        //load results
        loadResults();

        lv.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

                final int checkedCount = lv.getCheckedItemCount();

                mode.setTitle(checkedCount +" "+ getString(R.string.selected));

                if(checked){
                    rra.select(position);
                }else{
                    rra.unselect(position);
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {

                mode.getMenuInflater().inflate(R.menu.deletable,menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {


                new AlertDialog.Builder(LoadResultsActivity.this)
                        .setTitle(getString(R.string.app_name))
                        .setMessage("Do you really want to delete these processings ?")
                        .setPositiveButton(getString(R.string.ok_string), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                SparseBooleanArray checked = lv.getCheckedItemPositions();

                                final ArrayList<CRSFeatureCollection> selectedItems = new ArrayList<>();
                                for (int i = 0; i < checked.size(); i++) {
                                    int pos = checked.keyAt(i);

                                    if (checked.valueAt(i)) {
                                        selectedItems.add(rra.getItem(pos));
                                    }
                                }
                                for(CRSFeatureCollection cfc : selectedItems){
                                    Log.i(TAG, "selected "+cfc.tableName);
                                }

                                new AsyncTask<Void,Void,Boolean>(){

                                    @Override
                                    protected void onPreExecute() {
                                        super.onPreExecute();
                                        showProgress(getString(R.string.deleting));
                                    }

                                    @Override
                                    protected Boolean doInBackground(Void... params) {

                                        //1.close external dbs
                                        try{
                                            SpatialDataSourceManager.getInstance().closeDatabases();
                                        }catch(java.lang.Exception e) {
                                            Log.e(TAG, "exception closing external dbs",e);
                                        }

                                        //open this one
                                        final Database db = SpatialiteUtils.openSpatialiteDB(getBaseContext());
                                        try {

                                            for (CRSFeatureCollection cfc : selectedItems) {


                                                if (!SpatialiteUtils.deleteResult(db, cfc.tableName)) {
                                                    Log.w(TAG, "error deleting result " + cfc.tableName);
                                                    return false;
                                                }

                                                if (!SpatialiteUtils.deleteResultFromNamesTable(db, cfc.tableName)) {
                                                    Log.w(TAG, "error deleting result from names table " + cfc.tableName);
                                                    return false;
                                                }

                                            }
                                            return true;
                                        } finally {
                                            try {
                                                db.close();

                                                //reopen external dbs
                                                SpatialDataSourceManager.getInstance().clear();
                                                SpatialDataSourceManager.getInstance().init(MapFilesProvider.getBaseDirectoryFile());

                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception closing db", e);
                                            }
                                        }
                                    }

                                    @Override
                                    protected void onPostExecute(Boolean aBoolean) {
                                        super.onPostExecute(aBoolean);

                                        hideProgress();

                                        mode.finish();

                                        rra.clearSelection();

                                        loadResults();
                                    }
                                }.execute();

                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                mode.finish();

                                rra.clearSelection();

                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();



                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {


            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "item " + position + " clicked");
                }

                CRSFeatureCollection item = rra.getItem(position);

                Intent returnIntent = new Intent();
                returnIntent.putExtra(Config.RESULT, item.tableName);
                setResult(RESULT_OK, returnIntent);
                finish();

            }
        });

        mNavigationDrawerFragment = (ComputeNavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.result_navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.result_navigation_drawer,
                (DrawerLayout) findViewById(R.id.result_drawer_layout));

        mNavigationDrawerFragment.setEntries(new String[]{
                        getString(R.string.go_back)
                }
        );

    }

    private void loadResults() {

        new LoadResultsTask() {
            @Override
            public void started() {

                showProgress(getBaseContext().getString(R.string.loading_layer_list));
            }

            @Override
            public void done(List<CRSFeatureCollection> results) {

                hideProgress();

                if (results != null) {
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "task loaded " + results.size() + " results");
                    }
                    rra.clear();
                    rra.addAll(results);
                    rra.notifyDataSetChanged();
                } else {
                    Log.w(TAG, "load task results null");
                }
            }
        }.execute(getBaseContext());
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        if(mNavigationDrawerFragment != null && position == 0){

            finish();
        }
    }

    public class RiskResultAdapter extends ArrayAdapter<CRSFeatureCollection>
    {
        private HashMap<Integer, Boolean> selection = new HashMap<>();

        public RiskResultAdapter(Context context,  List<CRSFeatureCollection> objects) {
            super(context, R.layout.risk_result_item, objects);
        }

        public void select(int pos){

            selection.put(pos, true);
            notifyDataSetChanged();
        }
        public void unselect(int pos){

            if(selection.keySet().contains(pos)){

                selection.remove(pos);
                notifyDataSetChanged();
            }
        }
        public void clearSelection(){

            selection.clear();

            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v;

            if(convertView != null){
                v = convertView;
            }else{
                LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.risk_result_item, parent, false);
            }

            if(selection.get(position) != null){
                v.setBackgroundColor(getBaseContext().getResources().getColor(R.color.list_selected)); //selected
            }else{
                v.setBackgroundColor(getBaseContext().getResources().getColor(R.color.list_deselected)); //unselected
            }

            final TextView left_tv   = (TextView) v.findViewById(R.id.id_tv);
            final TextView center_tv = (TextView) v.findViewById(R.id.crs_tv);
            final TextView right_tv  = (TextView) v.findViewById(R.id.feature_count_tv);

            final CRSFeatureCollection item = getItem(position);

            //title -> usereditedname
            left_tv.setText(String.format(Locale.getDefault(), "%s", item.userEditedName));
            //if a user edited description is available use it
            if(item.userEditedDescription != null) {
                center_tv.setText(item.userEditedDescription);
            }else {//otherwise use the crs ?!
                if (item.crs != null && item.crs.properties != null && item.crs.properties.containsKey("name")) {
                    center_tv.setText(String.format(Locale.getDefault(), "%s", item.crs.properties.get("name")));
                }
            }
            if(item.totalFeatures != null){
                right_tv.setText(String.format(Locale.getDefault(), "Features : %d", item.totalFeatures));
            }

            return v;
        }
    }



    public void showProgress(final String message){

        if(pd == null){
            pd = new ProgressDialog(LoadResultsActivity.this, ProgressDialog.STYLE_SPINNER);
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
