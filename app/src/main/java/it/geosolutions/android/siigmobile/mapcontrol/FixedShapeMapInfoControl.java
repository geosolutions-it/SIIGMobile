/*
 * GeoSolutions GeoSolutions Android Map Library - Digital mapping on Android based devices
 * Copyright (C) 2013  GeoSolutions (www.geo-solutions.it)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.android.siigmobile.mapcontrol;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;

import it.geosolutions.android.map.BuildConfig;
import it.geosolutions.android.map.common.Constants.Modes;
import it.geosolutions.android.map.control.MapControl;
import it.geosolutions.android.map.control.todraw.Circle;
import it.geosolutions.android.map.control.todraw.Polygon;
import it.geosolutions.android.map.control.todraw.Rectangle;
import it.geosolutions.android.map.listeners.MapInfoListener;
import it.geosolutions.android.map.listeners.OneTapListener;
import it.geosolutions.android.map.listeners.PolygonTapListener;
import it.geosolutions.android.map.utils.Coordinates.Coordinates_Query;
import it.geosolutions.android.map.view.AdvancedMapView;

/**
 * Created by Robert Oehler on 30.08.15.
 *
 * Mirroring the functionality of MapInfoControl
 * this MapControl uses a shape defined in the constructor
 * which won't change during lifetime
 *
 * the event when a shape selection has been completed
 * may be directly evaluated using the static constructors
 * for the single shapes
 *
 * all other properties of a normal MapInfoControl remain the same
 *
 */
public class FixedShapeMapInfoControl extends MapControl {

    //Listeners
    protected MapInfoListener mapListener;
    protected OneTapListener oneTapListener;
    protected PolygonTapListener polygonTapListener;

    private static Paint paint_fill = new Paint();
    private static Paint paint_stroke = new Paint();
    private static int FILL_COLOR = Color.BLUE;
    private static int FILL_ALPHA = 50;
    private static int STROKE_COLOR = Color.BLACK;
    private static int STROKE_ALPHA = 100;
    private static int STROKE_WIDTH = 3;
    private static boolean STROKE_DASHED = false;
    private static float STROKE_SPACES = 10f;
    private static float STROKE_SHAPE_DIMENSION = 15f;
    private static Paint.Join STROKE_ANGLES = Paint.Join.ROUND;

    public static final int DONT_CONNECT = -1;

    public Activity activity;
    public AdvancedMapView mapView;

    private String mEnabledMessage;

    public enum ShapeType
    {
        Rectangular,
        Circular,
        OnePoint,
        Polygonal
    }

    private ShapeType mShapeType;
    private SelectionCallback selectionCallback;

    /**
     * created a MapControl for a one point selection
     * the result of the selection is reported via @param feedback
     * @param mapView to use
     * @param activity to use
     * @param buttonToConnectToID id of the button defined in the layout to connect this control to
     * @param enabledMessage a message to show via toast when the control is enabled (may be null)
     * @param callback the callback to be informed about the selection result
     * @return the control
     */
    public static FixedShapeMapInfoControl createOnePointControl(AdvancedMapView mapView, Activity activity,final int buttonToConnectToID,final String enabledMessage, final OnePointSelectionCallback callback){

        final FixedShapeMapInfoControl mif = new FixedShapeMapInfoControl(mapView,activity,ShapeType.OnePoint);
        mif.setSelectionCallback(callback);
        mif.setOneTapListener(new OneTapListener(mapView, activity) {
            @Override
            protected void infoDialogCircle(double lon, double lat, double radius, byte zoomLevel) {

                float pixel_x = this.getStartX();
                float pixel_y = this.getStartY();

                if (mif.getSelectionCallback() != null && mif.getSelectionCallback() instanceof OnePointSelectionCallback) {
                    ((OnePointSelectionCallback) mif.getSelectionCallback()).pointSelected(lat, lon, pixel_x, pixel_y, radius, zoomLevel);
                }
            }
        });
        if(enabledMessage != null){
            mif.setEnabledMessage(enabledMessage);
        }
        if(buttonToConnectToID != DONT_CONNECT) {
            mif.setActivationButton((ImageButton) activity.findViewById(buttonToConnectToID));
        }
        mif.setMode(MapControl.MODE_VIEW);

        if(mapView != null) {
            mapView.addControl(mif);
        }
        return mif;
    }
    /**
     * created a MapControl for a polygon selection
     * the result of the selection is reported via @param feedback
     * @param mapView to use
     * @param activity to use
     * @param buttonToConnectToID id of the button defined in the layout to connect this control to
     * @param enabledMessage a message to show via toast when the control is enabled (may be null)
     * @param callback the callback to be informed about the selection result
     * @return the control
     */
    public static FixedShapeMapInfoControl createPolygonControl(AdvancedMapView mapView, Activity activity,final int buttonToConnectToID,final String enabledMessage,final PolygonCreatedCallback callback){

        final FixedShapeMapInfoControl mif = new FixedShapeMapInfoControl(mapView,activity,ShapeType.Polygonal);
        mif.setSelectionCallback(callback);
        mif.setPolygonTapListener(new PolygonTapListener(mapView, activity) {
            @Override
            protected void infoDialogPolygon(ArrayList<Coordinates_Query> polygon_points, byte zoomLevel) {

                if (mif.getSelectionCallback() != null && mif.getSelectionCallback() instanceof PolygonCreatedCallback) {
                    ((PolygonCreatedCallback) mif.getSelectionCallback()).polygonCreated(polygon_points, zoomLevel);
                }
            }
        });

        if(enabledMessage != null){
            mif.setEnabledMessage(enabledMessage);
        }
        if(buttonToConnectToID != DONT_CONNECT) {
            mif.setActivationButton((ImageButton) activity.findViewById(buttonToConnectToID));
        }
        mif.setMode(MapControl.MODE_VIEW);

        if(mapView != null) {
            mapView.addControl(mif);
        }

        return mif;
    }

    /**
     * created a MapControl for a rectangular selection
     * the result of the selection is reported via @param feedback
     * @param mapView to use
     * @param activity to use
     * @param buttonToConnectToID id of the button defined in the layout to connect this control to
     * @param enabledMessage a message to show via toast when the control is enabled (may be null)
     * @param callback the callback to be informed about the selection result
     * @return the control
     */
    public static FixedShapeMapInfoControl createRectangularControl(AdvancedMapView mapView, Activity activity,final int buttonToConnectToID,final String enabledMessage, final RectangleCreatedCallback callback){

        final FixedShapeMapInfoControl mif = new FixedShapeMapInfoControl(mapView,activity,ShapeType.Rectangular);
        mif.setSelectionCallback(callback);
        mif.setMapListener(new MapInfoListener(mapView, activity) {
            @Override
            protected void infoDialog(double n, double w, double s, double e, byte zoomLevel) {

                if (mif.getSelectionCallback() != null && mif.getSelectionCallback() instanceof RectangleCreatedCallback) {
                    ((RectangleCreatedCallback) mif.getSelectionCallback()).rectangleCreated(w, s, e, n, zoomLevel);
                }
            }
        });

        if(enabledMessage != null){
            mif.setEnabledMessage(enabledMessage);
        }
        if(buttonToConnectToID != DONT_CONNECT) {
            mif.setActivationButton((ImageButton) activity.findViewById(buttonToConnectToID));
        }
        mif.setMode(MapControl.MODE_VIEW);

        if(mapView != null) {
            mapView.addControl(mif);
        }

        return mif;

    }
    /**
     * created a MapControl for a circular selection
     * the result of the selection is reported via @param feedback
     * @param mapView to use
     * @param activity to use
     * @param buttonToConnectToID id of the button defined in the layout to connect this control to
     * @param enabledMessage a message to show via toast when the control is enabled (may be null)
     * @param callback the callback to be informed about the selection result
     * @return the control
     */
    public static FixedShapeMapInfoControl createCircularControl(AdvancedMapView mapView, Activity activity,final int buttonToConnectToID,final String enabledMessage, final CircleCreatedCallback callback){

        final FixedShapeMapInfoControl mif = new FixedShapeMapInfoControl(mapView,activity,ShapeType.Circular);
        mif.setSelectionCallback(callback);
        mif.setMapListener(new MapInfoListener(mapView, activity) {
            @Override
            protected void infoDialogCircle(double x, double y, double radius, byte zoomLevel) {

                if (mif.getSelectionCallback() != null && mif.getSelectionCallback() instanceof  CircleCreatedCallback) {
                    ((CircleCreatedCallback)mif.getSelectionCallback()).circleCreated(y, x, radius, zoomLevel);
                }
            }
        });

        if(enabledMessage != null){
            mif.setEnabledMessage(enabledMessage);
        }
        if(buttonToConnectToID != DONT_CONNECT) {
            mif.setActivationButton((ImageButton) activity.findViewById(buttonToConnectToID));
        }
        mif.setMode(MapControl.MODE_VIEW);

        if(mapView != null) {
            mapView.addControl(mif);
        }

        return mif;
    }

    /**
     * Creates a new MapInfoControl object and the associated listener.
     * @param mapView to use
     * @param activity to use
     */
    public FixedShapeMapInfoControl(AdvancedMapView mapView, Activity activity, final ShapeType selectionType) {
        super(mapView);
        this.mapView = mapView;
        this.activity=activity;

        mShapeType = selectionType;

        instantiateListener();
        setMode(mode);
    }

    /**
     * Loads preferences about style of selection and checks if any of these has been
     * changed from user.
     */
    public void loadStyleSelectorPreferences(){

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());

        //Load preferences about style of fill
        int fill_color = pref.getInt("FillColor", FILL_COLOR);
        if(fill_color != FILL_COLOR) FILL_COLOR = fill_color; //Check if default color for selection has been selected, otherwise it changes the variable FILL_COLOR

        int fill_alpha = pref.getInt("FillAlpha", FILL_ALPHA);
        if(fill_alpha != FILL_ALPHA) FILL_ALPHA = fill_alpha;

        //Load preferences about style of stroke
        int stroke_color = pref.getInt("StrokeColor", STROKE_COLOR);
        if(stroke_color != STROKE_COLOR) STROKE_COLOR = stroke_color;

        int stroke_alpha = pref.getInt("StrokeAlpha", STROKE_ALPHA);
        if(stroke_alpha != STROKE_ALPHA) STROKE_ALPHA = stroke_alpha;

        int stroke_width = pref.getInt("StrokeWidth", STROKE_WIDTH);
        if(stroke_width != STROKE_WIDTH) STROKE_WIDTH = stroke_width;

        boolean stroke_dashed = pref.getBoolean("DashedStroke", STROKE_DASHED);

        int stroke_sp = pref.getInt("StrokeSpaces", (int) STROKE_SPACES);
        float stroke_spaces = (float)stroke_sp;

        int stroke_dim = pref.getInt("StrokeDim", (int) STROKE_SHAPE_DIMENSION);
        float stroke_shape_dim = (float)stroke_dim;

        //Load preference about shape of angles of stroke
        boolean has_changed = false;
        String angles_shape = pref.getString("StrokeAngles", "BEVEL");

        if(!angles_shape.equals(STROKE_ANGLES.toString())){
            has_changed = true;
            if(angles_shape.equals("BEVEL")) STROKE_ANGLES = Paint.Join.BEVEL;
            else if(angles_shape.equals("MITER")) STROKE_ANGLES = Paint.Join.MITER;
            else if(angles_shape.equals("ROUND")) STROKE_ANGLES = Paint.Join.ROUND;
        }


        if(stroke_dashed != STROKE_DASHED || stroke_spaces != STROKE_SPACES || stroke_shape_dim != STROKE_SHAPE_DIMENSION || has_changed){
            STROKE_DASHED = stroke_dashed;
            STROKE_SPACES = stroke_spaces;
            STROKE_SHAPE_DIMENSION = stroke_shape_dim;

            //When user unchecks option for dashed stroke to reset paint is necessary because otherwise the stroke remains dashed.
            paint_stroke.reset();
        }
    }

    /**
     * Method used to draw on map, possible selections is: rectangular, circular, one point.
     * @param canvas to draw on
     */
    @Override
    public void draw(Canvas canvas) {
        // fill	properties
        paint_fill.setStyle(Paint.Style.FILL);
        paint_fill.setColor(FILL_COLOR);
        paint_fill.setAlpha(FILL_ALPHA);

        // border properties
        paint_stroke.setStyle(Paint.Style.STROKE);
        paint_stroke.setColor(STROKE_COLOR);
        paint_stroke.setAlpha(STROKE_ALPHA);
        paint_stroke.setStrokeWidth(STROKE_WIDTH);
        paint_stroke.setStrokeJoin(STROKE_ANGLES);

        //Checks if user required dashed stroke
        if(STROKE_DASHED)
            paint_stroke.setPathEffect(new DashPathEffect(new float[]{STROKE_SHAPE_DIMENSION,STROKE_SPACES}, 0));

        switch (mShapeType){

            case Rectangular:
                    if(!mapListener.isDragStarted()){
                        return;
                    }

                    Rectangle r = new Rectangle(canvas);
                    r.buildObject(mapListener);
                    r.draw(paint_fill);
                    r.draw(paint_stroke);
                break;
            case Circular:
                    if(!mapListener.isDragStarted()){
                        return;
                    }

                    Circle c = new Circle(canvas);
                    c.buildObject(mapListener);
                    c.draw(paint_fill);
                    c.draw(paint_stroke);
                break;
            case OnePoint:
                    if(!oneTapListener.pointsAcquired()){
                        return;
                    }

                    Circle circle = new Circle(canvas);
                    circle.buildObject(oneTapListener);
                    circle.draw(paint_fill);
                    circle.draw(paint_stroke);
                break;
            case Polygonal:
                    if(!polygonTapListener.isAcquisitionStarted() || polygonTapListener.getNumberOfPoints() < 1){
                        return;
                    }
                    Polygon p = new Polygon(canvas,view);
                    p.buildPolygon(polygonTapListener,paint_stroke);
                    p.draw(paint_fill);
                    p.draw(paint_stroke);
                break;
        }

    }

    @Override
    public void setMode(int mode){
        super.setMode(mode);
        if(mode == MODE_VIEW){
            if(mapListener != null)
                mapListener.setMode(Modes.MODE_VIEW);
            if(oneTapListener != null)
                oneTapListener.setMode(Modes.MODE_VIEW);
            if(polygonTapListener !=null)
                polygonTapListener.setMode(Modes.MODE_VIEW);
        }
        else{
            if(mapListener != null)
                mapListener.setMode(Modes.MODE_EDIT);
            if(oneTapListener != null)
                oneTapListener.setMode(Modes.MODE_EDIT);
            if(polygonTapListener !=null)
                polygonTapListener.setMode(Modes.MODE_EDIT);
        }
    }

    @Override
    public void refreshControl(int requestCode, int resultCode, Intent data) {

        if(BuildConfig.DEBUG){
            Log.v("MapInfoControl", "requestCode:" + requestCode);
            Log.v("MapInfoControl", "resultCode:"+resultCode);
        }
        disable();
        getActivationButton().setSelected(false);
        loadStyleSelectorPreferences();
        instantiateListener();
        setMode(mode);
    }

    /**
     * Override the method of MapControl to cancel polygonal selection when
     * polygon is not closed and button info is not selected.
     */
    @Override
    public void setEnabled(boolean enabled){
        super.setEnabled(enabled);
        if(enabled && mEnabledMessage != null){
            Toast.makeText(mapView.getContext(),mEnabledMessage,Toast.LENGTH_SHORT).show();
        }
        if(!enabled && polygonTapListener != null && mShapeType == ShapeType.Polygonal ){
            polygonTapListener.reset();
        }
    }

    //Overrides the MapListener
    @Override
    public OnTouchListener getMapListener() {
        OnTouchListener tl;

        switch (mShapeType) {

            case Rectangular:
            case Circular:
                tl = this.mapListener;
                break;
            case OnePoint:
                tl = getOneTapListener();
                break;
            case Polygonal:
                tl = getPolygonTapListener();
                break;
            default:
                tl = this.mapListener;
                break;
        }

        return tl;
    }

    //Override the OneTapListener
    public OneTapListener getOneTapListener() {
        return this.oneTapListener;
    }

    //Override the OneTapListener
    public PolygonTapListener getPolygonTapListener() {
        return this.polygonTapListener;
    }

    /**
     * Instantiate listener for selection
     */
    public void instantiateListener(){

        switch (mShapeType) {

            case Rectangular:
            case Circular:
                if(this.mapListener == null){
                    this.mapListener = new MapInfoListener(mapView,activity);
                }
                break;
            case OnePoint:
                if(oneTapListener == null){
                    this.oneTapListener = new OneTapListener(mapView,activity);

                }
                break;
            case Polygonal:
                if(polygonTapListener == null){

                    this.polygonTapListener = new PolygonTapListener(mapView,activity);
                }
                break;
            default:
                if(this.mapListener == null){
                    this.mapListener = new MapInfoListener(mapView,activity);
                }
                break;
        }
    }


    public void setOneTapListener(final OneTapListener oneTapListener){
        this.oneTapListener = oneTapListener;
    }
    public void setPolygonTapListener(final PolygonTapListener polygonTapListener){
        this.polygonTapListener = polygonTapListener;
    }
    public void setMapListener(MapInfoListener listener){
        this.mapListener = listener;
    }

    public void setEnabledMessage(String message){
        this.mEnabledMessage = message;
    }

    public void setSelectionCallback(SelectionCallback selectionCallback) {
        this.selectionCallback = selectionCallback;
    }

    public SelectionCallback getSelectionCallback() {
        return selectionCallback;
    }

    public interface SelectionCallback{   }

    public interface OnePointSelectionCallback extends SelectionCallback
    {
        void pointSelected(double lat, double lon, float pixel_x, float pixel_y, double radius, byte zoomLevel);
    }
    public interface PolygonCreatedCallback extends SelectionCallback
    {
        void polygonCreated(ArrayList<Coordinates_Query> polygon_points, byte zoomLevel);
    }
    public interface RectangleCreatedCallback extends SelectionCallback
    {
        void rectangleCreated(final double minLon, final double minLat, final double maxLon, final double maxLat, byte zoomLevel);
    }

    public interface CircleCreatedCallback extends SelectionCallback
    {
        void circleCreated(final double lat, final double lon, final double radius, byte zoomLevel);
    }
}
