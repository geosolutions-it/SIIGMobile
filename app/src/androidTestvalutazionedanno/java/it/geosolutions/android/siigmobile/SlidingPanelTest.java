package it.geosolutions.android.siigmobile;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.test.ActivityUnitTestCase;

import com.loopj.android.http.PersistentCookieStore;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.mapsforge.core.model.BoundingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.cookie.Cookie;
import it.geosolutions.android.map.fragment.featureinfo.FeatureInfoAttributeListFragment;
import it.geosolutions.android.map.model.query.WMSGetFeatureInfoQuery;
import it.geosolutions.android.map.utils.ProjectionUtils;
import it.geosolutions.android.map.view.AdvancedMapView;

/**
 * Created by Robert Oehler on 14.10.15.
 *
 */
public class SlidingPanelTest  extends ActivityUnitTestCase<MainActivity> {

    public SlidingPanelTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        setActivity(launchActivity(getInstrumentation().getTargetContext().getPackageName(), MainActivity.class, null));
    }

    public void testSlidingPanelFragment(){

        assertNotNull(getActivity());

        final AdvancedMapView mapView = getActivity().mapView;

        assertNotNull(mapView);

        final SlidingUpPanelLayout panel = getActivity().getSlidingPanel();

        assertNotNull(panel);

        assertTrue(panel.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN);

        //create a query
        Bundle bundle = new Bundle();
        bundle.putInt("start", 0);
        bundle.putInt("limit", 1);
        bundle.putInt(FeatureInfoAttributeListFragment.PARAM_CUSTOM_LAYOUT, R.layout.feature_fragment_cust);

        final String wfsLayer = Config.WFS_LAYERS[1];
        final String wmsLayer = Config.WMS_LAYERS[1];

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

        WMSGetFeatureInfoQuery query = new WMSGetFeatureInfoQuery();
        query.setLat(mapView.getMapViewPosition().getCenter().latitude);
        query.setLon(mapView.getMapViewPosition().getCenter().longitude);
        query.setPixel_X(345.0f);
        query.setPixel_Y(555.0f);
        query.setZoomLevel(mapView.getMapViewPosition().getZoomLevel());
        query.setMapSize(mapSize);
        query.setBbox(boundingBox);
        query.setLayers(new String[]{wmsLayer});
        query.setQueryLayers(new String[]{wfsLayer});
        query.setVersion(version);
        query.setCrs(crs);
        query.setLocale(env);
        query.setStyles("");
        query.setAdditionalParams(additionalParameters);
        query.setEndPoint(Config.DESTINATION_WMS_URL);
        bundle.putParcelable("query", query);

        List<Pair<String,String>> headers = new ArrayList<Pair<String, String>>();
        headers.add(new Pair<String, String>("Authorization", Config.DESTINATION_AUTHORIZATION));

        query.setHeaders(headers);
        getActivity().showFragment(bundle);

        //wait for inflation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        FeatureInfoAttributeListFragment wmsInfo = (FeatureInfoAttributeListFragment) getActivity().getSupportFragmentManager().findFragmentByTag(MainActivity.WMS_INFO_FRAGMENT_TAG);

        assertNotNull(wmsInfo);


    }
}
