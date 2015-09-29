/*
 * GeoSolutions Android Map Library - Digital field mapping on Android based devices
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
package it.geosolutions.android.map.utils;

import android.util.Log;

import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;

import java.util.ArrayList;
import java.util.Map;

import it.geosolutions.android.map.model.Attribute;
import it.geosolutions.android.map.model.Feature;
import it.geosolutions.android.map.view.AdvancedMapView;
import it.geosolutions.android.map.wms.GetFeatureInfoConfiguration;

/**
 * An utility class to perform conversion operations of point between pixels and long/lat.
 * @author Jacopo Pianigiani (jacopo.pianigiani85@gmail.com)
 */
public class ConversionUtilities {

	/**
	 * Perform a conversion from pixels to longitude of a point on map.
	 * @param view
	 * @param pixel_x pixels of the point.
	 * @return longitude value for point.
	 */
	public static double convertFromPixelsToLongitude(AdvancedMapView view, double pixel_x){
		MapPosition mapPosition = view.getMapViewPosition()
                .getMapPosition();
        byte zoomLevel = view.getMapViewPosition().getZoomLevel();
        GeoPoint geoPoint = mapPosition.geoPoint;
        
        double pixelLeft = MercatorProjection.longitudeToPixelX(
                geoPoint.longitude, mapPosition.zoomLevel);      
        pixelLeft -= view.getWidth() >> 1;
        double ret = 0;
        try{
        	ret = MercatorProjection.pixelXToLongitude(pixelLeft + pixel_x, zoomLevel);
        }catch(IllegalArgumentException e){
        	ret = 180;
        }
    	return ret;
	}
	
	/**
	 * Perform a conversion from pixels to latitude of a point on map.
	 * @param view
	 * @param pixel_y pixels of the point.
	 * @return latitude value for point.
	 */
	public static double convertFromPixelsToLatitude(AdvancedMapView view, double pixel_y){
		MapPosition mapPosition = view.getMapViewPosition()
                .getMapPosition();
        byte zoomLevel = view.getMapViewPosition().getZoomLevel();
        GeoPoint geoPoint = mapPosition.geoPoint;
        
        double pixelTop = MercatorProjection.latitudeToPixelY(
                geoPoint.latitude, mapPosition.zoomLevel);        
        pixelTop -= view.getHeight() >> 1;
        double ret=0;
        try{
           ret  = MercatorProjection.pixelYToLatitude(pixelTop + pixel_y, zoomLevel);
        }catch(IllegalArgumentException e){
        	ret = MercatorProjection.LATITUDE_MAX;
        }
    	return  ret;
	}	
	
	/**
	 * Perform a conversion from latitude to pixels of a point on map.
	 * @param view
	 * @param latitude latitude of the point.
	 * @return pixels value for point.
	 */
	public static double convertFromLatitudeToPixels(AdvancedMapView view, double latitude){
        byte zoomLevel = view.getMapViewPosition().getZoomLevel();
        GeoPoint mapCenter = view.getMapViewPosition().getCenter(); //MapCenter
     
        // calculate the pixel coordinates of the top left corner
        double pixelY = MercatorProjection.latitudeToPixelY(mapCenter.latitude, zoomLevel)
                - (view.getHeight() >> 1);
   
        return ( MercatorProjection.latitudeToPixelY(latitude, zoomLevel) - pixelY);
	}
	
	/**
	 * Perform a conversion from longitude to pixels of a point on map.
	 * @param view
	 * @param longitude longitude of the point.
	 * @return pixels value for point.
	 */
	public static double convertFromLongitudeToPixels(AdvancedMapView view, double longitude){
        byte zoomLevel = view.getMapViewPosition().getZoomLevel();
        GeoPoint mapCenter = view.getMapViewPosition().getCenter(); //MapCenter
        
        // calculate the pixel coordinates of the top left corner
        double pixelX = MercatorProjection.longitudeToPixelX(mapCenter.longitude, zoomLevel)
        - (view.getWidth() >> 1);
        
        return (MercatorProjection.longitudeToPixelX(longitude, zoomLevel) - pixelX);
	}

    /**
     * converts a list of
     * it.geosolutions.android.map.wfs.geojson.feature.Feature
     * to
     * it.geosolutions.android.map.model.Feature
     *
     * applying the Locale configuration
     * it loads the config for the current language -or the default language if the current is not supported-
     * and adds feature properties which are contained in the config
     *
     * @param locale the configuration to apply (may be null)
     * @param layerName the layername to filter the config
     * @param features the features to convert
     * @return the map.model.Features containing Attributes
     */
    public static ArrayList<Feature> convertWFSFeatures(final GetFeatureInfoConfiguration.Locale locale,final String layerName,final ArrayList<it.geosolutions.android.map.wfs.geojson.feature.Feature> features) {

        Map<String, String> props = null;
        if(locale != null){
            props = locale.getPropertiesForLayer(layerName);
        }

        ArrayList<it.geosolutions.android.map.model.Feature> mapModelFeatures = new ArrayList<>();
        for (it.geosolutions.android.map.wfs.geojson.feature.Feature f : features) {

            it.geosolutions.android.map.model.Feature mapModelFeature = new it.geosolutions.android.map.model.Feature();

            if (f.geometry != null) {
                mapModelFeature.setGeometry(f.geometry);
            }
            for (Map.Entry<String, Object> entry : f.properties.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (key == null) { //without key this entry is senseless
                    continue;
                }

                if (props != null) {
                    //if config contains this key, add this
                    if (props.containsKey(key)) {

                        mapModelFeature.add(createAttribute(props.get(key), value));
                    }
                } else { //no props, show all
                    mapModelFeature.add(createAttribute(key, value));
                }
            }
            mapModelFeatures.add(mapModelFeature);

        }
        return mapModelFeatures;
    }

    /**
     * creates an attribute
     *
     * @param key   key must not be null
     * @param value can be null
     * @return the attribute
     */
    public static Attribute createAttribute(String key, Object value) {
        Attribute attribute = new Attribute();
        attribute.setName(key);

        if (value == null) {

            Log.w("WMSSource", "value null for key : " + key);

        } else {

            attribute.setValue(value.toString());
        }
        return attribute;
    }

}