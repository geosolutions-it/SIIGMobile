package it.geosolutions.android.siigmobile.wps;

import java.util.Properties;

import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;

/**
 * Created by Robert Oehler on 02.07.15.
 *
 */
public class CRSFeatureCollection extends FeatureCollection {

    public Crs crs;
    public String tableName;
    public String userEditedName;
    public String userEditedDescription;
    public boolean isPIS;

    public static class Crs {

        public String type;
        public Properties properties;

        public Crs(final int epsg){

            this.type = "name";
            this.properties = new Properties();
            this.properties.put("name","EPSG:"+epsg);

        }
    }


}
