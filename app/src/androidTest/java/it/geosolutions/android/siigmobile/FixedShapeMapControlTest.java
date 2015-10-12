package it.geosolutions.android.siigmobile;

import android.os.SystemClock;
import android.test.ActivityUnitTestCase;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import it.geosolutions.android.map.utils.Coordinates.Coordinates_Query;
import it.geosolutions.android.map.view.AdvancedMapView;
import it.geosolutions.android.siigmobile.mapcontrol.FixedShapeMapInfoControl;

/**
 * Created by Robert Oehler on 01.09.15.
 *
 * tests the creation of the map controls
 *
 */
public class FixedShapeMapControlTest extends ActivityUnitTestCase<MainActivity> {


    public FixedShapeMapControlTest(){
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        setActivity(launchActivity(getInstrumentation().getTargetContext().getPackageName(), MainActivity.class, null));
    }

    public void testLongPressControl(){

        final CountDownLatch latch = new CountDownLatch(4);

        assertNotNull(getActivity());

        final float touch_x1 = 347.0f;
        final float touch_y1 =  42.0f;

        AdvancedMapView mapView = getActivity().mapView;

        FixedShapeMapInfoControl longPressControl = FixedShapeMapInfoControl.createLongPressControl(mapView, getActivity(), FixedShapeMapInfoControl.DONT_CONNECT, null, new FixedShapeMapInfoControl.OnePointSelectionCallback() {
            @Override
            public void pointSelected(double lat, double lon, float pixel_x, float pixel_y, double radius, byte zoomLevel) {


                assertEquals(pixel_x,touch_x1);
                assertEquals(pixel_y,touch_y1);

                Log.i(FixedShapeMapControlTest.class.getSimpleName(), "passed");

                latch.countDown();
            }
        });


        MotionEvent down = MotionEvent.obtain(SystemClock.uptimeMillis() - 1000,SystemClock.uptimeMillis() - 1000,MotionEvent.ACTION_DOWN,touch_x1,touch_y1,0);
        MotionEvent longPress = MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,touch_x1,touch_y1,0);

        //simulate touch events
        longPressControl.getOneTapListener().onDown(down);
        longPressControl.getOneTapListener().onLongPress(longPress);

    }

    public void ignoretestControls(){

        final CountDownLatch latch = new CountDownLatch(4);

        assertNotNull(getActivity());

        AdvancedMapView mapView = getActivity().mapView;

        assertNotNull(mapView);

        final float touch_x1 = 347.0f;
        final float touch_y1 =  42.0f;

        final float touch_x2 = 367.0f;
        final float touch_y2 =  62.0f;

        final float touch_x3 = 387.0f;
        final float touch_y3 =  22.0f;

        /*** ONE POINT ***/

        final FixedShapeMapInfoControl onePointControl = FixedShapeMapInfoControl.createOnePointControl(mapView, getActivity(), it.geosolutions.android.map.R.id.ButtonInfo, null, new FixedShapeMapInfoControl.OnePointSelectionCallback() {
            @Override
            public void pointSelected(double lat, double lon, float pixel_x, float pixel_y, double radius, byte zoomLevel) {

                //we are not interested to test the locations but that this callback is called

                Log.i(FixedShapeMapControlTest.class.getSimpleName(), "passed");

                latch.countDown();
            }
        });

        assertNotNull(onePointControl);
        onePointControl.loadStyleSelectorPreferences();
        onePointControl.setEnabled(true);

        MotionEvent down = MotionEvent.obtain(SystemClock.uptimeMillis() - 1000,SystemClock.uptimeMillis() - 1000,MotionEvent.ACTION_DOWN,touch_x1,touch_y1,0);
        MotionEvent longPress = MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,touch_x1,touch_y1,0);

        //simulate touch events
        onePointControl.getOneTapListener().onDown(down);
        onePointControl.getOneTapListener().onLongPress(longPress);


        /*** RECTANGLE ***/

        final FixedShapeMapInfoControl rectangleControl = FixedShapeMapInfoControl.createRectangularControl(mapView, getActivity(), it.geosolutions.android.map.R.id.ButtonInfo, null, new FixedShapeMapInfoControl.RectangleCreatedCallback() {

            @Override
            public void rectangleCreated(double minLon, double minLat, double maxLon, double maxLat, byte zoomLevel) {

                //we are not interested to test the locations but that this callback is called

                Log.i(FixedShapeMapControlTest.class.getSimpleName(), "passed");

                latch.countDown();
            }
        });

        assertNotNull(rectangleControl);
        rectangleControl.loadStyleSelectorPreferences();
        rectangleControl.setEnabled(true);

        MotionEvent recDown = MotionEvent.obtain(SystemClock.uptimeMillis() - 1000,SystemClock.uptimeMillis() - 1000,MotionEvent.ACTION_DOWN,touch_x1,touch_y1,0);
        MotionEvent recUp   =  MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,touch_x2,touch_y2,0);

        //simulate touch events
        rectangleControl.getMapListener().onTouch(null, recDown);
        rectangleControl.getMapListener().onTouch(null, recUp);

        /*** CIRCLE ***/

        final FixedShapeMapInfoControl circleControl = FixedShapeMapInfoControl.createCircularControl(mapView, getActivity(), it.geosolutions.android.map.R.id.ButtonInfo, null, new FixedShapeMapInfoControl.CircleCreatedCallback() {


            @Override
            public void circleCreated(double lat, double lon, double radius, byte zoomLevel) {

                //we are not interested to test the locations but that this callback is called

                Log.i(FixedShapeMapControlTest.class.getSimpleName(), "passed");

                latch.countDown();
            }
        });

        assertNotNull(circleControl);
        circleControl.loadStyleSelectorPreferences();
        circleControl.setEnabled(true);


        MotionEvent cirDown = MotionEvent.obtain(SystemClock.uptimeMillis() - 1000,SystemClock.uptimeMillis() - 1000,MotionEvent.ACTION_DOWN,touch_x1,touch_y1,0);
        MotionEvent cirUp   =  MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,touch_x2,touch_y2,0);

        //simulate touch events
        circleControl.getMapListener().onTouch(null,cirDown);
        circleControl.getMapListener().onTouch(null,cirUp);


        /*** POLYGON ***/

        final FixedShapeMapInfoControl polygonControl = FixedShapeMapInfoControl.createPolygonControl(mapView, getActivity(), it.geosolutions.android.map.R.id.ButtonInfo, null, new FixedShapeMapInfoControl.PolygonCreatedCallback() {


            @Override
            public void polygonCreated(ArrayList<Coordinates_Query> polygon_points, byte zoomLevel) {
                //we are not interested to test the locations but that this callback is called

                Log.i(FixedShapeMapControlTest.class.getSimpleName(), "passed");

                latch.countDown();
            }
        });

        assertNotNull(polygonControl);
        polygonControl.loadStyleSelectorPreferences();
        polygonControl.setEnabled(true);

        MotionEvent down1 =  MotionEvent.obtain(SystemClock.uptimeMillis() - 1000,SystemClock.uptimeMillis() - 1000,MotionEvent.ACTION_DOWN,touch_x1,touch_y1,0);
        MotionEvent down2 =  MotionEvent.obtain(SystemClock.uptimeMillis()- 500,SystemClock.uptimeMillis() - 500,MotionEvent.ACTION_DOWN,touch_x2,touch_y2,0);
        MotionEvent tap   =  MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),MotionEvent.ACTION_DOWN,touch_x3,touch_y3,0);

        //simulate touch events
        polygonControl.getPolygonTapListener().onDown(down1);
        polygonControl.getPolygonTapListener().onDown(down2);
        polygonControl.getPolygonTapListener().onDoubleTap(tap);

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }
    }
}
