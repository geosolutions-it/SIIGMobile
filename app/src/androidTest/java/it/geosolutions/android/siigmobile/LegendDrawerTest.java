package it.geosolutions.android.siigmobile;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.test.ActivityUnitTestCase;
import android.widget.ListView;
import android.widget.TextView;

import it.geosolutions.android.map.style.AdvancedStyle;
import it.geosolutions.android.map.style.StyleManager;
import it.geosolutions.android.siigmobile.legend.LegendAdapter;
import it.geosolutions.android.siigmobile.legend.LegendItem;

/**
 * Created by Robert Oehler on 10.07.15.
 *
 */
public class LegendDrawerTest extends ActivityUnitTestCase<MainActivity> {


    TextView legendTitle;
    ListView legendListView;

    public LegendDrawerTest(){
        super(MainActivity.class);

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        //need to set a Theme.AppCompat theme to be able to start activity
        ContextThemeWrapper context = new ContextThemeWrapper(getInstrumentation().getTargetContext(), R.style.AppTheme);
        setActivityContext(context);

        // starts the MainActivity of the target application
        startActivity(new Intent(getInstrumentation().getTargetContext(), MainActivity.class), null, null);

        legendTitle =    (TextView) getActivity().findViewById(R.id.legend_drawer_title);
        legendListView = (ListView) getActivity().findViewById(R.id.legend_listview);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }


    /**
     * tests that the legend drawer after onCreate is filled with the data of the totale them
     *
     * afterwards a social style is loaded, applied and tested if the legend drawer adapted it successfully
     */
    public void testLegendDrawer(){

        assertNotNull(legendTitle);

        assertNotNull(legendListView);

        assertNotNull(legendListView.getAdapter());

        final String title = legendTitle.getText().toString();

        assertEquals(getInstrumentation().getTargetContext().getResources().getStringArray(R.array.drawer_items)[3], title);

        assertEquals(legendListView.getAdapter().getCount(), 9);

        AdvancedStyle totalStyle = StyleManager.getInstance().getStyle("totale_1");

        assertNotNull(totalStyle);

        assertNotNull(totalStyle.rules);

        final int totalItemToTest = 4; //range[0,8]

        final LegendItem totalItem =  (LegendItem) legendListView.getAdapter().getItem(totalItemToTest);

        assertEquals(totalItem.title, totalStyle.rules[totalItemToTest].getSymbolizer().title);

        assertEquals(totalItem.color, Color.parseColor(totalStyle.rules[totalItemToTest].getSymbolizer().strokecolor));

        final AdvancedStyle socialStyle = StyleManager.getInstance().getStyle("sociale_1");

        assertNotNull(socialStyle);

        assertNotNull(socialStyle.rules);

        assertTrue(legendListView.getAdapter() instanceof LegendAdapter);

        ((LegendAdapter)legendListView.getAdapter()).applyStlye(socialStyle);

        final int socialItemToTest = 1; //range[0,2]

        final LegendItem socialItem =  (LegendItem) legendListView.getAdapter().getItem(socialItemToTest);

        assertEquals(socialItem.title, socialStyle.rules[socialItemToTest].getSymbolizer().title);

        assertEquals(socialItem.color, Color.parseColor(socialStyle.rules[socialItemToTest].getSymbolizer().strokecolor));
    }

}
