package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.test.ActivityUnitTestCase;
import android.util.Log;

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
     *
     * the alphabetical order of the parameters
     *
     * its validity comparing it to a mock request from resources
     *
     * sending it to a mock server
     *
     * To await (virtual) completion a CountDownLatch is used
     */
    public void testWPSCallCreation(){

        RiskWPSRequest request = createDummyRequest();

        assertNotNull(request);

        assertNotNull(request.getFeatureCollection());

        assertNotNull(request.getParameters());

        request.setParameter(RiskWPSRequest.KEY_EXTENDEDSCHEMA, false);

        //1.ensure parameters in alphabetical key order
        int i = 0;
        for(Map.Entry<String,Object> entry : request.getParameters().entrySet()) {
            Log.i(TAG,String.format(Locale.getDefault(),"Parameter %s : %s",entry.getKey(),entry.getValue().toString()));
            switch (i){
                case 0:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_BATCH));
                    break;
                case 1:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_ENTITIES));
                    break;
                case 2:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_EXTENDEDSCHEMA));
                    break;
                case 3:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_FORMULA));
                    break;
                case 4:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_FP));
                    break;
                case 5:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_KEMLER));
                    break;
                case 6:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_LEVEL));
                    break;
                case 7:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_MATERIALS));
                    break;
                case 8:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_PRECISION));
                    break;
                case 9:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_PROCESSING));
                    break;
                case 10:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_SCENARIOS));
                    break;
                case 11:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_SEVERENESS));
                    break;
                case 12:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_STORE));
                    break;
                case 13:
                    assertTrue(entry.getKey().equals(RiskWPSRequest.KEY_TARGET));
                    break;
            }

            i++;
        }

        //2.having ensured the order, create the query
        final String query = RiskWPSRequest.createWPSCallFromText(request);

        assertNotNull(query);

        try {
            //3.compare it against a dummy wps request from resources
            final String response = getRawResourceAsString(getInstrumentation().getTargetContext(), R.raw.dummy_wps);

            assertEquals(query, response);

        } catch (IOException e) {
            Log.e(TAG, "IOException getting dummy wps", e);
            fail();
        }

        //4.Test making a call against a mock server

        //CountDownLatch lets us wait until async ops return and prevents the test from being killed
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
                assertEquals(result.features.size(),6);
                assertEquals(result.crs.properties.get("name"),"EPSG:32632");
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

    private RiskWPSRequest createDummyRequest(){

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

        return new RiskWPSRequest(
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

    }
}
