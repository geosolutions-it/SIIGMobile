package it.geosolutions.android.siigmobile;

import android.content.Context;
import android.test.ActivityUnitTestCase;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Geometry;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import it.geosolutions.android.map.wfs.geojson.GeometryJsonDeserializer;
import it.geosolutions.android.map.wfs.geojson.GeometryJsonSerializer;
import it.geosolutions.android.siigmobile.wfs.WFSBersagliDataActivity;
import it.geosolutions.android.siigmobile.wfs.WFSRequest;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Client;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;

/**
 * Created by Robert Oehler on 14.09.15.
 *
 */
public class WFSRequestTest  extends ActivityUnitTestCase<WFSBersagliDataActivity> {


    public WFSRequestTest() {
        super(WFSBersagliDataActivity.class);
    }


    /**
     * tests the general functionality of the wfs request / response parsing using a mock client
     */
    public void testWFSRequest() {

        Log.i("WFSTest", "start");

        final CountDownLatch signal = new CountDownLatch(1);

        assertNotNull(getInstrumentation().getTargetContext());

        final GeoPoint center = new GeoPoint(45.07037, 7.67416);
        final int radius = 200;
        final String layerName = Config.WFS_LAYERS[1];

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeHierarchyAdapter(Geometry.class, new GeometryJsonSerializer())
                .registerTypeHierarchyAdapter(Geometry.class, new GeometryJsonDeserializer()).create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(WFSRequest.ENDPOINT)
                .setClient(new MockClient())
                .setConverter(new GsonConverter(gson))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        BoundingBox bb = new BoundingBox(center.latitude, center.longitude, center.latitude, center.longitude);
        final BoundingBox extended = Util.extend(bb, radius);

        final String bb_string = String.format(Locale.US, "%f,%f,%f,%f,EPSG:4326",
                extended.minLongitude,
                extended.minLatitude,
                extended.maxLongitude,
                extended.maxLatitude);

        final String layer = String.format(Locale.getDefault(), "destination:%s", layerName);

        WFSRequest.SIIGWFSServices services = restAdapter.create(WFSRequest.SIIGWFSServices.class);
        services.getFeatureWFS(
                WFSRequest.VALUE_SERVICE,
                WFSRequest.VALUE_VERSION,
                WFSRequest.VALUE_REQUEST,
                layer,
                bb_string,
                Config.WFS_MAX_RESULTS,
                WFSRequest.VALUE_OUTPUTFORMAT,
                new Callback<CRSFeatureCollection>() {
                    @Override
                    public void success(CRSFeatureCollection featureResponse, Response response) {

                        Log.i("WFSTest", "response");

                        assertNotNull(featureResponse);

                        assertNotNull(featureResponse.features);

                        assertTrue(featureResponse.features.size() > 0);

                        assertNotNull(featureResponse.crs);

                        assertNotNull(featureResponse.features.get(0).geometry);
                        assertNotNull(featureResponse.features.get(0).properties);
                        assertNotNull(featureResponse.features.get(0).id);
                        assertNotNull(featureResponse.features.get(0).type);

                        Log.i("WFSTest", "passed");

                        signal.countDown();
                    }

                    @Override
                    public void failure(RetrofitError error) {

                        fail("failure " + error.getMessage());

                        signal.countDown();
                    }
                });

        Log.i("WFSTest", "request");

        try {
            signal.await(30, TimeUnit.SECONDS);// wait for callback
        } catch (InterruptedException e) {
            Log.e(WFSRequestTest.class.getSimpleName(), "exception waiting for callback");
            fail("time out exceeded");
        }

    }


    /**
     * Mock Implementation for Retrofit Client
     * The response is got from the application resources
     */
    public class MockClient implements Client {

        @Override
        public Response execute(Request request) throws IOException {

            final String response = getRawResourceAsString(getInstrumentation().getTargetContext(), R.raw.wfs_dummy_response);

            assertNotNull(response);

            return new Response(request.getUrl(), 200, "success", Collections.EMPTY_LIST, new TypedByteArray("application/json", response.getBytes()));
        }
    }

    public String getRawResourceAsString(final Context context, final int resourceID) throws IOException {

        InputStream is = context.getResources().openRawResource(resourceID);

        assertNotNull(is);

        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer);
    }
}
