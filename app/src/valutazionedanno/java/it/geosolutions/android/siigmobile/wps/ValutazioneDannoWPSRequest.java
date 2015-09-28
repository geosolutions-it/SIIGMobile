package it.geosolutions.android.siigmobile.wps;

import android.content.Context;
import android.util.Log;

import com.vividsolutions.jts.geom.Envelope;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import it.geosolutions.android.map.wfs.geojson.feature.Feature;
import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;
import it.geosolutions.android.siigmobile.BuildConfig;
import it.geosolutions.android.siigmobile.spatialite.SpatialiteUtils;

/**
 * Created by Robert Oehler on 02.09.15.
 *
 */
public class ValutazioneDannoWPSRequest extends WPSRequest {

    public static final String KEY_FORMULA = "formula";
    public static final String KEY_PROCESSING = "processing";
    public static final String KEY_PRECISION = "precision";
    public static final String KEY_TARGET = "target";
    public static final String KEY_EXTENDEDSCHEMA = "extendedSchema";
    public static final String KEY_MOBILE = "mobile";
    public static final String KEY_ENTITIES = "entities";
    public static final String KEY_FP = "fp";
    public static final String KEY_MATERIALS = "materials";
    public static final String KEY_SEVERENESS = "severeness";
    public static final String KEY_LEVEL = "level";
    public static final String KEY_BATCH = "batch";
    public static final String KEY_STORE = "store";
    public static final String KEY_SCENARIOS = "scenarios";
    public static final String KEY_KEMLER = "kemler";
    public static final String KEY_DAMAGE_AREA= "damageArea";

    /**
     * Default values for this operation
     */
    public static final String VALUE_FORMULA = "124";
    public static final String VALUE_PROCESSING  = "4";
    public static final String VALUE_PRECISION  = "4";
    public static final String VALUE_EXTENDED_SCHEMA = "false";
    public static final String VALUE_MOBILE = "true";
    public static final String VALUE_FP = "fp_scen_centrale";
    public static final String VALUE_SEVERENESS = "1,2,3,4,5";
    public static final String VALUE_BATCH = "1000";
    public static final String VALUE_STORE = "destination";
    public static final String VALUE_TARGET = "100";

    public static final int DAMAGE_AREA_EPSG = 32632;

    private static final String TAG = "ValutaDannoWPSRequest";

    private static final NavigableMap<Double, String> areas = new TreeMap<>();
    // Max values
    static {
        areas.put(0.00025, "destination:siig_geo_ln_arco_1");
        areas.put(0.001, "destination:siig_geo_ln_arco_2");
        areas.put(0.004, "destination:siig_geo_pl_arco_3");
        areas.put(0.25, "destination:siig_geo_pl_arco_4");
        areas.put(100.0, "destination:siig_geo_pl_arco_5");
        //18.225263943
    }

    private static final NavigableMap<Double, Integer> levels = new TreeMap<>();
    // Max values
    static {
        levels.put(0.00025, 1);
        levels.put(0.001, 2);
        levels.put(0.004, 3);
        levels.put(0.25, 4);
        levels.put(100.0, 5);
        //18.225263943
    }



    public ValutazioneDannoWPSRequest(

            final FeatureCollection _features,
            final String scenarios,
            final String accidents,
            final String entity
    ){

        this.featureCollection = _features;

        this.parameters.put(KEY_BATCH, VALUE_BATCH);
        this.parameters.put(KEY_ENTITIES, entity);
        this.parameters.put(KEY_EXTENDEDSCHEMA, VALUE_EXTENDED_SCHEMA);
        this.parameters.put(KEY_FORMULA, VALUE_FORMULA);
        this.parameters.put(KEY_FP, VALUE_FP);
        this.parameters.put(KEY_KEMLER, scenarios);
        this.parameters.put(KEY_MATERIALS, scenarios);
        this.parameters.put(KEY_MOBILE, VALUE_MOBILE);
        this.parameters.put(KEY_PRECISION, VALUE_PRECISION);
        this.parameters.put(KEY_PROCESSING, VALUE_PROCESSING);
        this.parameters.put(KEY_SCENARIOS, accidents);
        this.parameters.put(KEY_SEVERENESS, VALUE_SEVERENESS);
        this.parameters.put(KEY_STORE, VALUE_STORE);
        this.parameters.put(KEY_TARGET, VALUE_TARGET);


    }

    public ValutazioneDannoWPSRequest(final FeatureCollection _features, final HashMap<String,Object> _parameters){

        this.featureCollection = _features;

        for(Map.Entry<String,Object> entry : _parameters.entrySet()){
            this.parameters.put(entry.getKey(),entry.getValue());
        }
    }



    /**
     * Creates a hardcoded gs:RiskCalculator WPS Request
     * It inserts the  gemoetry as linear ring and adds dynamically all available attributes
     * @return the created XML as String
     * @throws IllegalArgumentException if no bounding box is available
     */
    public String createWPSCallFromText(final Context context) throws IllegalArgumentException {

        final StringBuilder builder = new StringBuilder();

        final String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n";

        if (this.featureCollection == null ||
                this.featureCollection.features == null ||
                this.featureCollection.features.size() < 1 ||
                this.featureCollection.features.get(0).geometry == null) {
            throw new IllegalArgumentException("no (bounding box) available");
        }


        final Feature f = this.featureCollection.features.get(0);
        final Envelope env = f.geometry.getEnvelopeInternal();

        try {
            this.parameters.put(KEY_LEVEL, levels.ceilingEntry(env.getArea()).getValue());
        }catch(ClassCastException cce){
            if(BuildConfig.DEBUG){
                Log.e(TAG, "Conversion error: "+ cce.getLocalizedMessage());
            }
            throw new IllegalArgumentException("Bad Area Value");
        }catch(NullPointerException npe){
            if(BuildConfig.DEBUG){
                Log.e(TAG, "Area in NULL: "+ npe.getLocalizedMessage());
            }
            throw new IllegalArgumentException("Area is empty");
        }

        final String minX = String.format(Locale.US, "%f", env.getMinX());
        final String minY = String.format(Locale.US, "%f", env.getMinY());
        final String maxX = String.format(Locale.US, "%f", env.getMaxX());
        final String maxY = String.format(Locale.US, "%f", env.getMaxY());

        final String wktPolygon = String.format(Locale.US,"POLYGON ((%s %s, %s %s, %s %s, %s %s, %s %s))",
                minX,minY,
                maxX,minY,
                maxX,maxY,
                minX,maxY,
                minX,minY);

        final String convertedPolygon = SpatialiteUtils.convertWGS84PolygonTo(context, wktPolygon, DAMAGE_AREA_EPSG);

        this.parameters.put(KEY_DAMAGE_AREA,convertedPolygon);

        final String lowerLeftCorner = String.format(Locale.US, "%f %f", env.getMinX(), env.getMinY());
        final String upperRightCorner = String.format(Locale.US, "%f %f", env.getMaxX(), env.getMaxY());

        final String geometry = String.format(Locale.US,
                "<ows:Identifier>gs:RiskCalculator</ows:Identifier>\n" +
                "<wps:DataInputs>\n" +
                "\t<wps:Input>\n" +
                "\t\t<ows:Identifier>features</ows:Identifier>\n" +
                "\t\t<wps:Reference mimeType=\"text/xml\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n" +
                "\t\t\t<wps:Body>\n" +
                "\t\t\t\t<wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\" xmlns:destination=\"http://destination.geo-solutions.it\">\n" +
                // TODO: the ceiling value can be null
                "\t\t\t\t\t<wfs:Query typeName=\""+ areas.ceilingEntry(env.getArea()).getValue()+"\">\n" +
                "\t\t\t\t\t\t<ogc:Filter>\n" +
                "\t\t\t\t\t\t\t<ogc:BBOX>\n" +
                "\t\t\t\t\t\t\t\t<ogc:PropertyName>geometria</ogc:PropertyName>\n" +
                "\t\t\t\t\t\t\t\t<gml:Envelope srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n" +
                "\t\t\t\t\t\t\t\t\t<gml:lowerCorner>%s</gml:lowerCorner>\n" +
                "\t\t\t\t\t\t\t\t\t<gml:upperCorner>%s</gml:upperCorner>\n" +
                "\t\t\t\t\t\t\t\t</gml:Envelope>\n" +
                "\t\t\t\t\t\t\t</ogc:BBOX>\n" +
                "\t\t\t\t\t\t</ogc:Filter>\n" +
                "\t\t\t\t\t</wfs:Query>\n" +
                "\t\t\t\t</wfs:GetFeature>\n" +
                "\t\t\t</wps:Body>\n" +
                "\t\t</wps:Reference>\n" +
                "\t</wps:Input>\n",lowerLeftCorner, upperRightCorner);

        final String footer =
                 "</wps:DataInputs>\n" +
                 "\t<wps:ResponseForm>\n" +
                 "\t\t<wps:RawDataOutput mimeType=\"application/json\">\n" +
                 "\t\t\t<ows:Identifier>result</ows:Identifier>\n" +
                 "\t\t</wps:RawDataOutput>\n" +
                 "\t</wps:ResponseForm>\n" +
                 "</wps:Execute>";

        builder.append(header);

        builder.append(geometry);

        for (Map.Entry<String,Object> entry : this.getParameters().entrySet()){

            if(entry.getValue() instanceof  String){

                builder.append(createAttributeString(entry.getKey(), (String) entry.getValue()));

            }else if(entry.getValue() instanceof Integer){

                builder.append(createAttributeString(entry.getKey(), Integer.toString((Integer) entry.getValue())));

            }else if(entry.getValue() instanceof  Boolean){

                builder.append(createAttributeString(entry.getKey(), Boolean.toString((Boolean) entry.getValue())));

            }else{

                Log.w(getClass().getSimpleName(), "unexpected attribute data type , using its toString() method to build the WPS call, check if that is correct");

                builder.append(createAttributeString(entry.getKey(), entry.getValue().toString()));
            }
        }

        builder.append(footer);

        return builder.toString();

    }



    /**
     * Creates an attribute String for a WPS request
     * @param id the id to insert
     * @param data the data to insert as String Literal
     * @return the formatted XML snippet
     */
    public static String createAttributeString(final String id, final String data){

        return String.format(Locale.US,
                "\t<wps:Input>\n" +
                "\t\t<ows:Identifier>%s</ows:Identifier>\n" +
                "\t\t<wps:Data>\n" +
                "\t\t\t<wps:LiteralData>%s</wps:LiteralData>\n" +
                "\t\t</wps:Data>\n" +
                "\t</wps:Input>\n", id, data);

    }


}
