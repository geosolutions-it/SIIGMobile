/*
 * GeoSolutions map - Digital field mapping on Android based devices
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
package it.geosolutions.android.map.wms;

import android.util.Log;

import com.squareup.okhttp.Headers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.geosolutions.android.map.BuildConfig;
import it.geosolutions.android.map.model.Attribute;
import it.geosolutions.android.map.model.Source;
import it.geosolutions.android.map.model.query.FeatureInfoQueryResult;
import it.geosolutions.android.map.model.query.FeatureInfoTaskQuery;
import it.geosolutions.android.map.model.query.WMSGetFeatureInfoQuery;
import it.geosolutions.android.map.model.query.WMSGetFeatureInfoTaskQuery;
import it.geosolutions.android.map.utils.ConversionUtilities;
import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;


/**
 * Represents a WMS Source
 * @author Lorenzo Natali(lorenzo.natali@geo-solutions.it)
 *
 */
public class WMSSource implements Source{

    // Tag for Logging
    private static String TAG = "WMSSource";

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((baseParams == null) ? 0 : baseParams.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WMSSource other = (WMSSource) obj;
		if (baseParams == null) {
			if (other.baseParams != null)
				return false;
		} else if (!baseParams.equals(other.baseParams))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

	public HashMap<String,String> baseParams= new HashMap<String,String>();
	private String url;
	private String title;
    private Headers headers;

    public Headers getHeaders() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    /**
	 * Create a WMSSource using its URL
	 * @param url
	 */
	public WMSSource(String url){
		setDefaultParameters();
		this.url=url;
	}
	
	/**
	 * @return the URL of the WMS Service
	 */
	public String getUrl(){
		return url;
	}
	
	/**
	 * set default parameters, to be applied to all the layers
	 */
	private void setDefaultParameters(){
		baseParams.put("format","image/png8");
		baseParams.put("transparent","true");
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	/* (non-Javadoc)
	 * @see it.geosolutions.android.map.model.Source#performQuery(it.geosolutions.android.map.model.query.FeatureInfoQuery, it.geosolutions.android.map.model.query.FeatureInfoQueryResult)
	 */
	@Override
	public int performQuery(FeatureInfoTaskQuery q, List<FeatureInfoQueryResult> data) {

        final String layerName  =  ((WMSLayer)q.getLayer()).getName();

        if (q instanceof WMSGetFeatureInfoTaskQuery) {

            final WMSGetFeatureInfoQuery query = (WMSGetFeatureInfoQuery) q;

            final WMSGetFeatureInfo getFeatureInfo = new WMSGetFeatureInfo(
                    query.getVersion(),
                    query.getLayers(),
                    query.getStyles(),
                    query.getCrs(),
                    query.getBbox(),
                    query.getMapSize(),
                    query.getQueryLayers(),
                    (int) Math.rint(query.getPixel_X()),
                    (int) Math.rint(query.getPixel_Y()),
                    query.getAdditionalParams(),
                    query.getEndPoint());

            if (query.getHeaders() != null) {
                getFeatureInfo.setHeaders(query.getHeaders());
            }

            FeatureCollection result = null;

            try{
                result = getFeatureInfo.requestGetFeatureInfo();
            }catch ( Exception e){
                // Something went wrong during WMS request
                if(BuildConfig.DEBUG){
                    Log.e(TAG, "Error during WMS request", e);
                }
                return 0;
            }

            final GetFeatureInfoConfiguration.Locale locale = query.getLocaleConfig();

            if (result != null && result.features != null && result.features.size() > 0) {

                if(BuildConfig.DEBUG) {
                    Log.i("WMSSource", "features arrived : " + result.features.size());
                }
                FeatureInfoQueryResult queryResult = new FeatureInfoQueryResult();

                ArrayList<it.geosolutions.android.map.model.Feature> mapModelFeatures = ConversionUtilities.convertWFSFeatures(locale, layerName, result.features);

                queryResult.setFeatures(mapModelFeatures);

                data.add(queryResult);

                return result.features.size();
            }


        } else {
            //TODO implement
            Log.e("WMSSource", "unexpected FeatureInfoTaskQuery type for wms source arrived : " + q.getClass().getSimpleName() + ", no implementation available");
        }

		return 0;
		
	}

    /**
     * creates an attribute
     * @param key key must not be null
     * @param value can be null
     * @return the attribute
     */
    public Attribute createAttribute(String key, Object value){
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
