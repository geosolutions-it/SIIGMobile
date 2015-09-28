package it.geosolutions.android.siigmobile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.mapsforge.core.model.GeoPoint;

import java.util.Locale;

import it.geosolutions.android.map.BuildConfig;
import it.geosolutions.android.siigmobile.elaboration.ElaborationResult;
import it.geosolutions.android.siigmobile.elaboration.Elaborator;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;
import jsqlite.Database;


public class ComputeFormActivity extends AppCompatActivity
        implements ComputeNavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = ComputeFormActivity.class.getSimpleName();

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private ComputeNavigationDrawerFragment mNavigationDrawerFragment;

    public final static String PARAM_POINT = "Point";

    private PlaceholderFragment mFormFragment;

    public enum CalcMethodMode
    {
        BUFFER,
        BY_SUBSTANCE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compute_form);

        Toolbar toolbar = (Toolbar) findViewById(R.id.m_toolbar);
        setSupportActionBar(toolbar);

        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Handle the menu item
                return true;
            }
        });

        // Inflate a menu to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.main);

        mNavigationDrawerFragment = (ComputeNavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.compute_navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.compute_navigation_drawer,
                toolbar,
                (DrawerLayout) findViewById(R.id.compute_drawer_layout));

        mNavigationDrawerFragment.setEntries(new String[]{
                        getString(R.string.go_back)
                }
        );

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        if(mNavigationDrawerFragment != null && position == 0){

            finish();
        }

        GeoPoint p = null;

        if(getIntent() != null && getIntent().getSerializableExtra(PARAM_POINT) != null){
            p = (GeoPoint) getIntent().getSerializableExtra(PARAM_POINT);
        }

        mFormFragment = PlaceholderFragment.newInstance(position + 1, p);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.compute_container, mFormFragment)
                .commit();
    }

    public PlaceholderFragment getFragment(){
        return  mFormFragment;
    }

    public void restoreActionBar() {

        if(getSupportActionBar() !=  null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(getResources().getStringArray(R.array.drawer_items)[getResources().getStringArray(R.array.drawer_items).length - 2]);
        }
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private Button computeButton;

        private EditText titleEd ;
        private EditText bufferWidthEd;
        private EditText descriptionEd;

        private Spinner methodSpinner;
        private Spinner scenarioSpinner;
        private Spinner accidentSpinner;
        private Spinner entitySpinner;

        private CalcMethodMode calcMethodMode;
        private TextWatcher titleTextWatcher;
        private TextWatcher bufferWidthTextWatcher;

        private ProgressDialog pd;
        private Elaborator elaborator;

        private boolean spinnerFirstCall = true;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber, final GeoPoint selectedPoint) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putSerializable(PARAM_POINT, selectedPoint);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_compute_form, container, false);

            final TextView coord_tv = (TextView) rootView.findViewById(R.id.tv_coord_value);

            if(getArguments() != null && getArguments().get(PARAM_POINT) != null){

                final GeoPoint p = (GeoPoint) getArguments().get(PARAM_POINT);
                if(BuildConfig.DEBUG && p != null) {
                    Log.v(TAG, "Point : " + String.format(Locale.US, "lat %f lon %f", p.latitude, p.longitude));
                }
                //TODO how many decimals of the coordinate to show, for now 6 ?
                coord_tv.setText(String.format(Locale.US,"%.6f , %.6f",p.latitude,p.longitude));

            }

            titleEd = (EditText) rootView.findViewById(R.id.edittext_title);
            bufferWidthEd = (EditText) rootView.findViewById(R.id.edittext_buffer_width);
            descriptionEd = (EditText) rootView.findViewById(R.id.edittext_description);

            methodSpinner    = (Spinner) rootView.findViewById(R.id.spinner_method);
            scenarioSpinner =  (Spinner) rootView.findViewById(R.id.spinner_scenario);
            accidentSpinner  = (Spinner) rootView.findViewById(R.id.spinner_accident);
            entitySpinner    = (Spinner) rootView.findViewById(R.id.spinner_entity);

            calcMethodMode = CalcMethodMode.BUFFER;

            titleEd.requestFocus();

            computeButton = (Button) rootView.findViewById(R.id.compute_button);

            /**
             * the following components must be edited by the user to activate the compute button
             * to be able to trigger a calculation
             *
             * BUFFER METHOD
             *  - title
             *  - the buffer width
             *  - a selected accident
             *
             *  BY_SUBSTANCE
             *  - title
             *  - a selected accident
             *  - the buffer width is calculated in this mode
             *
             *  This is checked and managed with the following listeners
             */

            //a listener to start a recalculation
            final AdapterView.OnItemSelectedListener recalcRadiusListener = new AdapterView.OnItemSelectedListener(){
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                    if(calcMethodMode == CalcMethodMode.BY_SUBSTANCE) {

                        recalculateRadius();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            };

            scenarioSpinner.setOnItemSelectedListener(recalcRadiusListener);
            entitySpinner.setOnItemSelectedListener(recalcRadiusListener);

            final AdapterView.OnItemSelectedListener activateComputeButtonListener = new AdapterView.OnItemSelectedListener(){

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                    switch (calcMethodMode) {
                        case BUFFER:
                            //if the accident spinner item changed check to de-/activate compute button

                            if (!TextUtils.isEmpty(titleEd.getText().toString()) &&
                                    !TextUtils.isEmpty(bufferWidthEd.getText().toString()) &&
                                    accidentSpinner.getSelectedItemPosition() != 0) {
                                if (!computeButton.isEnabled()) {
                                    activateComputeButton();
                                }
                            } else {
                                if (computeButton.isEnabled()) {
                                    deactivateComputeButton();
                                }
                            }

                            break;
                        case BY_SUBSTANCE:
                            //for substance do also a recalc
                            recalculateRadius();

                            if (!TextUtils.isEmpty(titleEd.getText().toString()) &&
                                    accidentSpinner.getSelectedItemPosition() != 0) {
                                if (!computeButton.isEnabled()) {
                                    activateComputeButton();
                                }
                            } else {
                                if (computeButton.isEnabled()) {
                                    deactivateComputeButton();
                                }
                            }
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            };

            accidentSpinner.setOnItemSelectedListener(activateComputeButtonListener);

            methodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                    //this is triggered onCreate, we don't want that event, return
                    if(spinnerFirstCall){
                        spinnerFirstCall = false;
                        return;
                    }

                    switch (CalcMethodMode.values()[position]) {
                        case BUFFER:
                            bufferWidthEd.setEnabled(true);
                            bufferWidthEd.setText("");
                            bufferWidthEd.requestFocus();

                            if(!TextUtils.isEmpty(titleEd.getText().toString()) &&
                                    !TextUtils.isEmpty(bufferWidthEd.getText().toString()) &&
                                    accidentSpinner.getSelectedItemPosition() != 0){
                                activateComputeButton();
                            }else{
                                deactivateComputeButton();
                            }

                            calcMethodMode = CalcMethodMode.BUFFER;

                            break;
                        case BY_SUBSTANCE: //select scenario

                            bufferWidthEd.setEnabled(false);

                            recalculateRadius();

                            if(!TextUtils.isEmpty(titleEd.getText().toString()) &&
                                    accidentSpinner.getSelectedItemPosition() != 0){
                                activateComputeButton();
                            }else{
                                deactivateComputeButton();
                            }

                            calcMethodMode = CalcMethodMode.BY_SUBSTANCE;

                            break;
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) { }
            });

            titleEd.addTextChangedListener(getTitleTextWatcher());
            bufferWidthEd.addTextChangedListener(getBufferWidthTextWatcher());

            deactivateComputeButton();

            return rootView;
        }

        /**
         * determines the current spinner selection and triggers a new radius calculation
         *
         * the result is set in the buffer width edittext
         */
        private void recalculateRadius(){

            final int scenarioItem =  scenarioSpinner.getSelectedItemPosition();
            final int accidentItem =  accidentSpinner.getSelectedItemPosition();
            final int entityItem   =  entitySpinner.getSelectedItemPosition();

            final String  scenario = getActivity().getResources().getStringArray(R.array.scenario_ids)[scenarioItem];
            final String  accident = getActivity().getResources().getStringArray(R.array.accident_ids)[accidentItem];
            final String  entity  = getActivity().getResources().getStringArray(R.array.entity_ids)[entityItem];

            final int  scenarioID = Integer.parseInt(scenario);
            final int  accidentID = Integer.parseInt(accident);
            final int  entityID   = Integer.parseInt(entity);

            int buffer = Elaborator.calculateBufferWidth(scenarioID, accidentID, entityID);

            bufferWidthEd.setText(Integer.valueOf(buffer).toString());

        }

        /**
         * deactivates the "compute" button -> not enabled, no listener
         */
        private void deactivateComputeButton(){

            computeButton.setEnabled(false);
            computeButton.setOnClickListener(null);

        }

        /**
         * activates the "compute" button -> enabled, click listener
         * which starts the calculation of the "danno"
         */
        private void activateComputeButton(){

            computeButton.setEnabled(true);
            computeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(elaborator != null){
                        //TODO a cancelled elaboration is still ongoing, allow user to start a new one ?
                    }

                    //title
                    final String title = titleEd.getText().toString();
                    final String description = descriptionEd.getText().toString(); //can be null

                    //as the button is disabled when the title is empty, this should not be necessary, but its better to check
                    if (TextUtils.isEmpty(title)) {

                        Toast.makeText(v.getContext(), R.string.elab_enter_title, Toast.LENGTH_SHORT).show();
                        titleEd.requestFocus();
                        return;
                    }

                    final Database db = SpatialiteUtils.openSpatialiteDB(getActivity());

                    final boolean exists = SpatialiteUtils.checkIfUserTitleExists(db, title);
                    try {
                        db.close();
                    } catch (jsqlite.Exception e) {
                        Log.e(TAG, "exception closing db", e);
                    }

                    if (exists) {
                        Toast.makeText(v.getContext(), R.string.title_exists, Toast.LENGTH_SHORT).show();
                        titleEd.requestFocus();
                        return;
                    }

                    //bufferwidth
                    int bufferWidth = 0;

                    if (calcMethodMode == ComputeFormActivity.CalcMethodMode.BUFFER) {
                        final String bufferWidthString = bufferWidthEd.getText().toString();
                        try {
                            bufferWidth = Integer.parseInt(bufferWidthString);
                        } catch (NumberFormatException e) {
                            //the exception is handled by the next line "if (bufferWidth == 0) { ..."
                        }

                        if (bufferWidth == 0) {
                            Toast.makeText(getActivity().getBaseContext(), R.string.enter_buffer_width, Toast.LENGTH_SHORT).show();
                            bufferWidthEd.requestFocus();
                            return;
                        }

                    }

                    final int accidentItem = accidentSpinner.getSelectedItemPosition();

                    if (accidentItem == 0) {
                        //no accident selected
                        Toast.makeText(v.getContext(), R.string.accident_select, Toast.LENGTH_SHORT).show();
                        accidentSpinner.requestFocus();
                        return;
                    }
                    final int scenarioItem = scenarioSpinner.getSelectedItemPosition();
                    final int entityItem = entitySpinner.getSelectedItemPosition();

                    elaborator = new Elaborator(getActivity()) {
                        @Override
                        public void showError(String errorMessage) {

                            showToast(errorMessage);
                        }

                        @Override
                        public void done(ElaborationResult result) {


                            hideProgress();


                            if(result != null){

                                Intent returnIntent = new Intent();

                                returnIntent.putExtra(Config.RESULT_ITEM, result);

                                getActivity().setResult(RESULT_OK, returnIntent);

                                getActivity().finish();

                            }else{

                                //failed, the user is informed via showError, enable input items
                                toggleAllInputs(true);
                            }

                            elaborator = null;

                        }
                    };

                    showProgress(getString(R.string.query_ongoing));

                    toggleAllInputs(false);

                    //start calc
                    elaborator.startCalc( (GeoPoint) getArguments().get(PARAM_POINT),
                            title,
                            description,
                            bufferWidth,
                            accidentItem,
                            scenarioItem,
                            entityItem);


                }
            });

        }

        public TextWatcher getTitleTextWatcher() {
            if(titleTextWatcher == null){
                titleTextWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                        if(count > 0){
                            switch (CalcMethodMode.values()[calcMethodMode.ordinal()]) {
                                case BUFFER:
                                    //title, selected accident and manual buffer, if it is set activate
                                    if(!TextUtils.isEmpty(bufferWidthEd.getText().toString()) && accidentSpinner.getSelectedItemPosition() != 0){
                                        activateComputeButton();
                                    }
                                    break;
                                case BY_SUBSTANCE:
                                    //title and selected accident, the buffer is calculated --> activate
                                    if(accidentSpinner.getSelectedItemPosition() != 0) {
                                        activateComputeButton();
                                    }
                                    break;
                            }

                        }else{
                            // no longer a title
                            deactivateComputeButton();
                        }

                    }

                    @Override
                    public void afterTextChanged(Editable s) { }
                };
            }
            return titleTextWatcher;
        }

        public TextWatcher getBufferWidthTextWatcher() {
            if(bufferWidthTextWatcher == null){
                bufferWidthTextWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (count > 0) {
                            //when this is changed we are in "buffer set" mode
                            switch (CalcMethodMode.values()[calcMethodMode.ordinal()]) {
                                case BUFFER:
                                    //title and selected accident, manual buffer, if it is set activate
                                    if (!TextUtils.isEmpty(titleEd.getText().toString()) && accidentSpinner.getSelectedItemPosition() != 0 ) {
                                        activateComputeButton();
                                    }
                                    break;
                                case BY_SUBSTANCE:
                                    break;
                            }
                        } else {
                            //no longer a manual buffer width
                            deactivateComputeButton();
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) { }
                };
            }
            return bufferWidthTextWatcher;
        }

        public void toggleAllInputs(final boolean enable){

            titleEd.setEnabled(enable);
            bufferWidthEd.setEnabled(enable);
            descriptionEd.setEnabled(enable);

            methodSpinner.setEnabled(enable);
            scenarioSpinner.setEnabled(enable);
            accidentSpinner.setEnabled(enable);
            entitySpinner.setEnabled(enable);

        }

       private void showToast(final String message) {

           if(getActivity() != null) {
               getActivity().runOnUiThread(new Runnable() {
                   @Override
                   public void run() {

                       Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                   }
               });
           }
        }

        private void showProgress(final String message){

            if(pd == null){
                pd = new ProgressDialog(getActivity(), ProgressDialog.STYLE_SPINNER);
                pd.setCancelable(true);
                pd.setIcon(R.drawable.ic_launcher);
                pd.setCanceledOnTouchOutside(false);
                pd.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {

                        if (getActivity() == null) {
                            return;
                        }

                        new AlertDialog.Builder(getActivity())
                                .setMessage(getString(R.string.cancel_elaboration))
                                .setPositiveButton(getString(R.string.ok_string), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {


                                        elaborator.cancel();
                                        hideProgress();

                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create()
                                .show();



                    }
                });
            }
            pd.setMessage(message);
            pd.show();

        }
        private void hideProgress(){

            if(pd != null && pd.isShowing()){
                pd.dismiss();
            }
        }
    }
}
