package it.geosolutions.android.siigmobile;

import android.graphics.Paint;
import android.test.ActivityUnitTestCase;

import org.mapsforge.android.maps.overlay.Circle;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.model.GeoPoint;

/**
 * Created by Robert Oehler on 07.09.15.
 *
 */
public class CircleOverlayTest extends ActivityUnitTestCase<MainActivity> {

    public CircleOverlayTest(){
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();


        setActivity(launchActivity(getInstrumentation().getTargetContext().getPackageName(), MainActivity.class, null));

    }

    public void testCircleOverlay(){

        assertNotNull(getActivity());

        final GeoPoint p = new GeoPoint(45.698879417964164,9.658253669738777);

        final int radius = 1000;
        final int stroke = 0xff000000;
        final int fill   = 0xffff0000;

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(stroke);

        Paint  fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(fill);

        getActivity().createOrUpdateCircleOverlay(p,radius,strokePaint,fillPaint);

        boolean found = false;
        Circle circle = null;
        //search circle
        for(OverlayItem item : getActivity().getOverlayItemList().getOverlayItems()){
            if(item instanceof Circle){
                circle = (Circle) item;
                found = true;
            }
        }

        //assert found
        assertTrue(found);

        //remove
        getActivity().removeOverlayItem(circle);

        //test removal
        for(OverlayItem item : getActivity().getOverlayItemList().getOverlayItems()){
            if(item instanceof Circle){
                fail("circle found after being removed");
            }
        }

    }
}
