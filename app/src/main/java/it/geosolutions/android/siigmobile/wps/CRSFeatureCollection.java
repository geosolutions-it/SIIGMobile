package it.geosolutions.android.siigmobile.wps;


import java.io.Serializable;
import java.util.Properties;

import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;

/**
 * Created by Robert Oehler on 29.06.15.
 *
 * Extends FeatureCollection with an CRS
 *
 */

public class CRSFeatureCollection extends FeatureCollection implements Serializable{

    public Crs crs;


    public class Crs {

        public String type;
        public Properties properties;
    }

}