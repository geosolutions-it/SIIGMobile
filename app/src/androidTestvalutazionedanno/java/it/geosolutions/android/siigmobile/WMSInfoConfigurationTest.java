package it.geosolutions.android.siigmobile;

import android.test.InstrumentationTestCase;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

import it.geosolutions.android.map.wms.GetFeatureInfoConfiguration;

/**
 * Created by Robert Oehler on 11.09.15.
 *
 */
public class WMSInfoConfigurationTest extends InstrumentationTestCase {

    public static final int PRESENT_LOCALS = 4;
    public static final int PRESENT_LAYERS = 13;

    public static final int[] LAYER_PROPERTIES_COUNT = new int[]{3,5,6,9,7,8,4,5,4,4,8,4,4};

    public static final String[] SUPPORTED_LANGUAGES = new String[]{"en","it","fr","de"};

    public static final String[] SKIP_KEYS = new String[]{"nr_letti_dh","sup_vendita","codice_clc","cod_fisc","nat_code","pres_max", "pres_med","letti_ordinari","profondita_max","quota_pdc"};

    /**
     * tests the wms info locale configuration for errors / inconsistencies
     * note: this may not cover all issues
     * like e.g. the correct matching of property keys and keys coming from the server
     *
     * //TODO when the configuration changes this test needs to be adapted
     */
    public void testWMSInfoConfiguration(){

        InputStream inputStream = getInstrumentation().getTargetContext().getResources().openRawResource(R.raw.getfeatureinfo_config);

        assertNotNull(inputStream);

        final Gson gson = new Gson();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        GetFeatureInfoConfiguration getFeatureInfoConfiguration = gson.fromJson(reader, GetFeatureInfoConfiguration.class);

        assertNotNull(getFeatureInfoConfiguration);
        assertEquals(getFeatureInfoConfiguration.getLocales().size(), PRESENT_LOCALS);

        for(int i = 0; i < getFeatureInfoConfiguration.getLocales().size();i++){

            final GetFeatureInfoConfiguration.Locale locale = getFeatureInfoConfiguration.getLocales().get(i);

            final String loc = locale.getLocale();

            assertEquals(locale.getLayers().size(), PRESENT_LAYERS);

            for (int j = 0; j < locale.getLayers().size(); j++) {

                GetFeatureInfoConfiguration.Layer layer = locale.getLayers().get(j);

                assertEquals(layer.getProperties().size(), LAYER_PROPERTIES_COUNT[j]);

                for (Map.Entry<String,String> entry : layer.getProperties().entrySet()) {

                    assertNotNull(entry.getKey());
                    assertNotNull(entry.getValue());

                    //ensure the local is correct
                    final String key = entry.getKey();
                    if (key.lastIndexOf("_") != -1) {

                        //skip the ones containing an underscore which do not contain a localization code
                        if (Arrays.asList(SKIP_KEYS).contains(key)){
                            continue;
                        }

                        final String suffix = key.substring(key.lastIndexOf("_") + 1);

                        //ensure it is supported
                        boolean valid = false;
                        for (String validLocal : SUPPORTED_LANGUAGES) {
                            if (suffix.equals(validLocal)) {
                                valid = true;
                                break;
                            }
                        }
                        assertTrue(String.format("invalid suffix %s", suffix), valid);
                        //ensure it fits the local of this configs locale
                        assertEquals(String.format("not equal suffix %s local %s", suffix, loc), suffix, loc);

                    }
                }

            }

        }

    }


}
