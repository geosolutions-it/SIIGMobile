package it.geosolutions.android.siigmobile.wps;

import java.util.Map;
import java.util.TreeMap;

import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;

/**
 * Created by Robert Oehler on 29.06.15.
 *
 * A WPS request contains a collection of features and a map of parameters
 *
 */
public class WPSRequest {

    /**
     * Input feature collection
     */
    protected FeatureCollection featureCollection;

    protected Map<String,Object> parameters = new TreeMap<>();


    public void setParameter(final String param, final Object values){

        parameters.put(param,values);

    }

    public Map<String,Object> getParameters(){
        return parameters;
    }

    public FeatureCollection getFeatureCollection() {
        return featureCollection;
    }
}
