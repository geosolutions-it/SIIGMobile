package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.test.ActivityUnitTestCase;
import android.util.Log;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import it.geosolutions.android.map.wfs.geojson.GeoJson;
import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;
import it.geosolutions.android.siigmobile.wps.RiskWPSRequest;
import it.geosolutions.android.siigmobile.wps.SIIGRetrofitClient;
import it.geosolutions.android.siigmobile.wps.SIIGWPSServices;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Client;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedString;

/**
 * Created by Robert Oehler on 01.07.15.
 *
 */
public class WPSCallTest extends ActivityUnitTestCase<MainActivity> {

    private final static String TAG = WPSCallTest.class.getSimpleName();

    public WPSCallTest(){
        super(MainActivity.class);
    }


    /**
     * Tests the creation of a wps request
     */
    public void testWPSCallCreation() {

        // Prepare dummy input features
        FeatureCollection features = createDummyFeatures();

        String store = "destination";
        int batch = 1000;
        int precision = 4;
        int processing = 1;
        int formula = 26;
        int target = 100;
        int level = 3;
        int kemler = 0;
        String materials = "1,2,3,4,5,6,7,8,9,10,11,12";
        String scenarios = "1,2,3,4,5,6,7,8,9,10,11,12,13,14";
        String entities = "1,2,3,4,5";
        String severeness = "0,1";
        String fp = "fp_scen_centrale";

        // Create RiskWPSRequest
        RiskWPSRequest request = new RiskWPSRequest(
                features,
                store,
                batch,
                precision,
                processing,
                formula,
                target,
                level,
                kemler,
                materials,
                scenarios,
                entities,
                severeness,
                fp
        );

        // Basic testing
        assertNotNull(request);

        assertNotNull(request.getFeatureCollection());

        assertNotNull(request.getParameters());

        // Set parameter
        request.setParameter(RiskWPSRequest.KEY_EXTENDEDSCHEMA, false);

        //Ensure parameters are correctly set
        Map<String, Object> parameters = request.getParameters();

        assertNotNull(parameters);

        assertNotNull(parameters.get(RiskWPSRequest.KEY_STORE));
        assertEquals(store, parameters.get(RiskWPSRequest.KEY_STORE));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_BATCH));
        assertEquals(batch, parameters.get(RiskWPSRequest.KEY_BATCH));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_PRECISION));
        assertEquals(precision, parameters.get(RiskWPSRequest.KEY_PRECISION));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_PROCESSING));
        assertEquals(processing, parameters.get(RiskWPSRequest.KEY_PROCESSING));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_FORMULA));
        assertEquals(formula, parameters.get(RiskWPSRequest.KEY_FORMULA));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_TARGET));
        assertEquals(target, parameters.get(RiskWPSRequest.KEY_TARGET));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_LEVEL));
        assertEquals(level, parameters.get(RiskWPSRequest.KEY_LEVEL));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_KEMLER));
        assertEquals(kemler, parameters.get(RiskWPSRequest.KEY_KEMLER));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_MATERIALS));
        assertEquals(materials, parameters.get(RiskWPSRequest.KEY_MATERIALS));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_SCENARIOS));
        assertEquals(scenarios, parameters.get(RiskWPSRequest.KEY_SCENARIOS));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_ENTITIES));
        assertEquals(entities, parameters.get(RiskWPSRequest.KEY_ENTITIES));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_SEVERENESS));
        assertEquals(severeness, parameters.get(RiskWPSRequest.KEY_SEVERENESS));

        assertNotNull(parameters.get(RiskWPSRequest.KEY_FP));
        assertEquals(fp, parameters.get(RiskWPSRequest.KEY_FP));
    }

    /**
     * Test for the RiskWPSRequest.createWPSCallFromText() method
     * The generated XML should be equivalent to the dummy_wps reference file content
     */
    public void testCreateWPSCallFromText(){

        // Prepare dummy input features
        FeatureCollection features = createDummyFeatures();

        // Create RiskWPSRequest
        RiskWPSRequest request = new RiskWPSRequest(
                features,
                "destination",
                1000,
                4,
                1,
                26,
                100,
                3,
                0,
                "1,2,3,4,5,6,7,8,9,10,11,12",
                "1,2,3,4,5,6,7,8,9,10,11,12,13,14",
                "1,2,3,4,5",
                "0,1",
                "fp_scen_centrale"
        );

        // Set parameter
        request.setParameter(RiskWPSRequest.KEY_EXTENDEDSCHEMA, false);

        //Create the query
        final String query = RiskWPSRequest.createWPSCallFromText(request);

        assertNotNull(query);

        //Compare it against a dummy wps request from resources
        try {

            String response  = getRawResourceAsString(getInstrumentation().getTargetContext(), R.raw.dummy_wps);

            Diff myDiff = new Diff(response, query);
            XMLUnit.setIgnoreWhitespace(true);
            assertTrue("XML are similar " + myDiff, myDiff.similar());

        } catch (IOException e) {
            Log.e(TAG, "IOException getting dummy wps", e);
            fail();
        } catch (SAXException e) {
            Log.e(TAG, "SAXException computing XML diff", e);
            fail(e.getMessage());
        }


    }

    /**
     * This is not a test
     * This is an example on how to call the WPS Service with Retrofit
     * In this example we use a MockServer to emulate the server
     *
     */
    public void demoWPSCall() {
        FeatureCollection features = createDummyFeatures();

        // Create RiskWPSRequest
        RiskWPSRequest request = new RiskWPSRequest(
                features,
                "destination",
                1000,
                4,
                1,
                26,
                100,
                3,
                0,
                "1,2,3,4,5,6,7,8,9,10,11,12",
                "1,2,3,4,5,6,7,8,9,10,11,12,13,14",
                "1,2,3,4,5",
                "0,1",
                "fp_scen_centrale"
        );

        request.setParameter(RiskWPSRequest.KEY_EXTENDEDSCHEMA, false);

        String query = RiskWPSRequest.createWPSCallFromText(request);

        //CountDownLatch lets us wait until async ops return and prevents the process from being killed
        final CountDownLatch signal = new CountDownLatch(1);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(SIIGRetrofitClient.ENDPOINT)
                .setClient(new MockClient())
                //.setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        //wrap the xml as "TypedString"
        //src : http://stackoverflow.com/questions/21398598/how-to-post-raw-whole-json-in-the-body-of-a-retrofit-request
        TypedString string = new TypedString(query){
            @Override public String mimeType() {
                return "application/xml";
            }
        };

        SIIGWPSServices siigService = restAdapter.create(SIIGWPSServices.class);
        siigService.postWPS(string, new Callback<CRSFeatureCollection>() {
            @Override
            public void success(CRSFeatureCollection result, Response response) {
                if (BuildConfig.DEBUG) {
                    Log.i("WPSCall", "success " + result.toString() + "\n" + response.toString());
                }

                signal.countDown();

                assertNotNull(result);
                assertNotNull(result.features);
                assertEquals(6, result.features.size());
                assertEquals("EPSG:32632", result.crs.properties.get("name"));
            }

            @Override
            public void failure(RetrofitError error) {

                Log.w("WPSCall", "failure " + error.getMessage());

                signal.countDown();

                fail("error received");
            }
        });

        try {
            signal.await();// wait for callback
        }catch (InterruptedException e){
            Log.e(TAG, "exception waiting for callback");
        }
    }

    /**
     * Mock Implementation for Retrofit Client
     * The response is got from the application resources
     */
    public class MockClient implements Client {

        @Override
        public Response execute(Request request) throws IOException {

            final String response = getRawResourceAsString(getInstrumentation().getTargetContext(),R.raw.dummy_response_multilinestring);

            assertNotNull(response);

            return new Response(request.getUrl(), 200, "success", Collections.EMPTY_LIST, new TypedByteArray("application/json", response.getBytes()));
        }
    }

    public String getRawResourceAsString(final Context context, final int resourceID) throws IOException{

        InputStream is = context.getResources().openRawResource(resourceID);

        assertNotNull(is);

        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer);
    }

    private FeatureCollection createDummyFeatures(){

        final  String sampleFeatureJSON =

                "{\n" +
                        "  \"type\": \"FeatureCollection\",\n" +
                        "  \"features\": [\n" +
                        "    {\n" +
                        "      \"type\": \"Feature\",\n" +
                        "      \"properties\": {},\n" +
                        "      \"geometry\": {\n" +
                        "        \"type\": \"Polygon\",\n" +
                        "        \"coordinates\": [\n" +
                        "          [\n" +
                        "            [\n" +
                        "              9.251861572265625,\n" +
                        "              45.303146403608906\n" +
                        "            ],\n" +
                        "            [\n" +
                        "              9.251861572265625,\n" +
                        "              45.3128234450559\n" +
                        "            ],\n" +
                        "            [\n" +
                        "              9.263790588378906,\n" +
                        "              45.3128234450559\n" +
                        "            ],\n" +
                        "            [\n" +
                        "              9.263790588378906,\n" +
                        "              45.303146403608906\n" +
                        "            ],\n" +
                        "            [\n" +
                        "              9.251861572265625,\n" +
                        "              45.303146403608906\n" +
                        "            ]\n" +
                        "          ]\n" +
                        "        ]\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

        FeatureCollection features = new GeoJson().fromJson(sampleFeatureJSON,null);

        if(features == null){
            throw new IllegalArgumentException("could not parse sample features");
        }

        return features;

    }
}
