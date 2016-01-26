package it.geosolutions.android.siigmobile.wps;

import android.util.Log;
import android.util.Xml;

import com.vividsolutions.jts.geom.Envelope;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import it.geosolutions.android.map.wfs.geojson.feature.Feature;
import it.geosolutions.android.map.wfs.geojson.feature.FeatureCollection;
import it.geosolutions.android.siigmobile.BuildConfig;

/**
 * Models a gs:RiskCalculator request to GeoServer using WPS
 *
 * Compare http://destination.geo-solutions.it/geoserver_destination
 *
 * Created by Robert Oehler on 29.06.15.
 */

public class RiskWPSRequest extends WPSRequest{

    public static final String KEY_FEATURES = "features";
    public static final String KEY_WORKSPACE = "workspace";
    public static final String KEY_STORE = "store";
    public static final String KEY_BATCH = "batch";
    public static final String KEY_PRECISION = "precision";
    public static final String KEY_CONNECTION = "connection";
    public static final String KEY_PROCESSING = "processing";
    public static final String KEY_FORMULA = "formula";
    public static final String KEY_TARGET = "target";
    public static final String KEY_MATERIALS ="materials";
    public static final String KEY_SCENARIOS = "scenarios";
    public static final String KEY_ENTITIES = "entities";
    public static final String KEY_SEVERENESS = "severeness";
    public static final String KEY_LEVEL = "level";
    public static final String KEY_KEMLER = "kemler";
    public static final String KEY_FP = "fp";
    public static final String KEY_CHANGED_TARGETS = "changedTargets";
    public static final String KEY_CFF = "cff";
    public static final String KEY_PSC = "psc";
    public static final String KEY_PADR = "padr";
    public static final String KEY_PIS = "pis";
    public static final String KEY_DISTANCES = "distances";
    public static final String KEY_DAMAGEAREA = "damageArea";
    public static final String KEY_EXTENDEDSCHEMA = "extendedSchema";
    public static final String KEY_CRS = "crs";
    public static final String KEY_MOBILE = "mobile";

    private static String TAG = "RiskWPSRequest";

    private static final NavigableMap<Double, String> areas = new TreeMap<>();
    // Max values
    static {
        areas.put(0.00025, "mobile:siig_geo_ln_arco_1");
        areas.put(0.001, "mobile:siig_geo_ln_arco_2");
        areas.put(0.004, "mobile:siig_geo_pl_arco_3");
        areas.put(0.25, "mobile:siig_geo_pl_arco_4");
        areas.put(100.0, "mobile:siig_geo_pl_arco_5");
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

    public RiskWPSRequest(final FeatureCollection _features){

        this.featureCollection = _features;
    }
    public RiskWPSRequest(final FeatureCollection _features, final HashMap<String,Object> _parameters){

        this.featureCollection = _features;

        for(Map.Entry<String,Object> entry : _parameters.entrySet()){
            this.parameters.put(entry.getKey(),entry.getValue());
        }
    }

    public RiskWPSRequest(

            final FeatureCollection _features,
            final String _store,
            final int _batch,
            final int _precision,
            final int _processing,
            final int _formula,
            final int _target,
            final int _level,
            final String _kemler,
            final String _materials,
            final String _scenarios,
            final String _entities,
            final String _severeness,
            final String _fp
    ){

        this.featureCollection = _features;

        this.parameters.put(KEY_WORKSPACE,"mobile");
        this.parameters.put(KEY_STORE,_store);
        this.parameters.put(KEY_BATCH,_batch);
        this.parameters.put(KEY_PRECISION,_precision);
        this.parameters.put(KEY_PROCESSING,_processing);
        this.parameters.put(KEY_FORMULA,_formula);
        this.parameters.put(KEY_TARGET,_target);
        this.parameters.put(KEY_MATERIALS,_materials);
        this.parameters.put(KEY_SCENARIOS,_scenarios);
        this.parameters.put(KEY_ENTITIES,_entities);
        this.parameters.put(KEY_SEVERENESS,_severeness);
        this.parameters.put(KEY_FP,_fp);
        this.parameters.put(KEY_LEVEL,_level);
        this.parameters.put(KEY_KEMLER,_kemler);

    }



    /**
     * Creates a hardcoded gs:RiskCalculator WPS Request
     * It inserts the bounding box of the geometry and adds dynamically all available attributes
     * @param request the request to parse
     * @return the created XML as String
     * @throws IllegalArgumentException if no boundingbox is provided in the request
     */
    public static String createWPSCallFromText(final WPSRequest request) throws IllegalArgumentException {

        final StringBuilder builder = new StringBuilder();

        final String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n";

        if (request.featureCollection == null ||
                request.featureCollection.features == null ||
                request.featureCollection.features.size() < 1 ||
                request.featureCollection.features.get(0).geometry == null) {
            throw new IllegalArgumentException("no (boundingbox) feature provided");
        }


        final Feature f = request.featureCollection.features.get(0);
        final Envelope env = f.geometry.getEnvelopeInternal();

        try {
            request.parameters.put(KEY_LEVEL, levels.ceilingEntry(env.getArea()).getValue());
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


        //local US necessary for dot formatting -> 12.34 instead of 12,34
        final String lowerLeftCorner = String.format(Locale.US, "%f %f", env.getMinX(), env.getMinY());
        final String upperRightCorner = String.format(Locale.US, "%f %f", env.getMaxX(), env.getMaxY());

        Log.v(TAG, "Area selected: "+ String.format(Locale.US, "%f", env.getArea()));

        final String geometry = String.format(Locale.US,
                "\t<ows:Identifier>gs:RiskCalculator</ows:Identifier>\n" +
                "\t<wps:DataInputs>\n" +
                "\t\t<wps:Input>\n" +
                "\t\t\t<ows:Identifier>features</ows:Identifier>\n" +
                "\t\t\t<wps:Reference mimeType=\"text/xml\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n" +
                "\t\t\t\t<wps:Body>\n" +
                "\t\t\t\t\t<wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\" xmlns:mobile=\"http://mobile.csi.it\">\n" +
                        // TODO: the ceiling value can be null
                "\t\t\t\t\t\t<wfs:Query typeName=\""+ areas.ceilingEntry(env.getArea()).getValue()+"\">\n" +
                "\t\t\t\t\t\t\t<ogc:Filter>\n" +
                "\t\t\t\t\t\t\t\t<ogc:BBOX>\n" +
                "\t\t\t\t\t\t\t\t\t<ogc:PropertyName>geometria</ogc:PropertyName>\n" +
                "\t\t\t\t\t\t\t\t\t<gml:Envelope srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n" +
                "\t\t\t\t\t\t\t\t\t\t<gml:lowerCorner>%s</gml:lowerCorner>\n" +
                "\t\t\t\t\t\t\t\t\t\t<gml:upperCorner>%s</gml:upperCorner>\n" +
                "\t\t\t\t\t\t\t\t\t</gml:Envelope>\n" +
                "\t\t\t\t\t\t\t\t</ogc:BBOX>\n" +
                "\t\t\t\t\t\t\t</ogc:Filter>\n" +
                "\t\t\t\t\t\t</wfs:Query>\n" +
                "\t\t\t\t\t</wfs:GetFeature>\n" +
                "\t\t\t\t</wps:Body>\n" +
                "\t\t\t</wps:Reference>\n" +
                "\t\t</wps:Input>\n", lowerLeftCorner, upperRightCorner);

        final String footer =
                "\t</wps:DataInputs>\n" +
                "\t<wps:ResponseForm>\n" +
                "\t\t<wps:RawDataOutput mimeType=\"application/json\">\n" +
                "\t\t\t<ows:Identifier>result</ows:Identifier>\n" +
                "\t\t</wps:RawDataOutput>\n" +
                "\t</wps:ResponseForm>\n" +
                "</wps:Execute>";

        builder.append(header);

        builder.append(geometry);

        for (Map.Entry<String,Object> entry : request.getParameters().entrySet()){

            if(entry.getValue() instanceof  String){

                builder.append(createAttributeString(entry.getKey(), (String) entry.getValue()));

            }else if(entry.getValue() instanceof Integer){

                builder.append(createAttributeString(entry.getKey(), Integer.toString((Integer) entry.getValue())));

            }else if(entry.getValue() instanceof  Boolean){

                builder.append(createAttributeString(entry.getKey(), Boolean.toString((Boolean) entry.getValue())));

            }else{

                Log.w(SIIGRetrofitClient.class.getSimpleName(), "unexpected attribute data type , using its toString() method to build the WPS call, check if that is correct");

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
    private static String createAttributeString(final String id, final String data){

        return String.format(Locale.US,
                "\t\t<wps:Input>\n" +
                "\t\t\t<ows:Identifier>%s</ows:Identifier>\n" +
                "\t\t\t<wps:Data>\n" +
                "\t\t\t\t<wps:LiteralData>%s</wps:LiteralData>\n" +
                "\t\t\t</wps:Data>\n" +
                "\t\t</wps:Input>\n", id, data);

    }

    /**
     * //TODO for more flexibility, this could be done using a XMLSerializer
     * //For now this causes issues due to the various namespaces a WPS call includes
     */

    public static String createWPSCallWithSerializer(final WPSRequest request) throws IOException {

        if(request.featureCollection == null ||
                request.featureCollection.features == null ||
                request.featureCollection.features.size() < 1 ||
                request.featureCollection.features.get(0).geometry == null){
            throw new IllegalArgumentException("no (bounding box) feature provided");
        }

        final Feature f = request.featureCollection.features.get(0);
        final Envelope env = f.geometry.getEnvelopeInternal();
        //local US necessary for dot formatting -> 12.34 instead of 12,34
        final String lowerLeftCorner = String.format(Locale.US,"%f %f",env.getMinX(),env.getMinY());
        final String upperRightCorner = String.format(Locale.US,"%f %f",env.getMaxX(),env.getMaxY());

        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();


        serializer.setOutput(writer);
        serializer.startDocument("UTF-8", true);

        serializer.setPrefix("wps", "wps");
        serializer.startTag("wps", "Execute");
        //header
        createHeader(serializer);

        serializer.setPrefix("ows", "ows");
        serializer.startTag("ows", "Identifier");
        serializer.text("gs:RiskCalculator");
        serializer.endTag("ows", "Identifier");

        //Begin with data
        serializer.setPrefix("wps", "wps");
        serializer.startTag("wps", "DataInputs");
        serializer.startTag("wps", "Input");

        serializer.setPrefix("ows", "ows");
        serializer.startTag("ows", "Identifier");
        serializer.text("features");
        serializer.endTag("ows", "Identifier");

        serializer.setPrefix("wps", "wps");
        serializer.startTag("wps", "Reference");
        serializer.attribute("", "mimeType", "text/xml");
        serializer.attribute("", "xlink:href", "http://geoserver/wfs");
        serializer.attribute("", "method", "POST");

        serializer.startTag("wps", "Body");
        serializer.setPrefix("wfs", "wfs");
        serializer.startTag("wfs", "GetFeature");
        serializer.attribute("", "service", "WFS");
        serializer.attribute("", "version", "1.0.0");
        serializer.attribute("", "outputFormat", "GML2");
        serializer.attribute("", "xlmns:mobile", "http://mobile.csi.it");

        serializer.startTag("wfs", "Query");
        serializer.attribute("", "typeName", "mobile:rischio_1");

        serializer.setPrefix("ogc", "ogc");
        serializer.startTag("ogc", "Filter");
        serializer.startTag("ogc", "BBOX");
        serializer.startTag("ogc", "PropertyName");
        serializer.text("geometria");
        serializer.endTag("ogc", "PropertyName");
        serializer.setPrefix("gml", "gml");
        serializer.startTag("gml", "Envelope");
        serializer.attribute("", "srsName", "http://www.opengis.net/gml/srs/epsg.xml#3857");
        serializer.startTag("gml", "lowerCorner");
        serializer.text(lowerLeftCorner);
        serializer.endTag("gml", "lowerCorner");
        serializer.startTag("gml", "upperCorner");
        serializer.text(upperRightCorner);
        serializer.endTag("gml", "upperCorner");
        serializer.endTag("gml", "Envelope");
        serializer.endTag("ogc", "BBOX");
        serializer.endTag("ogc", "Filter");
        serializer.endTag("wfs", "Query");
        serializer.endTag("wfs", "GetFeature");
        serializer.endTag("wps","Body");
        serializer.endTag("wps","Reference");
        serializer.endTag("wps","Input");

        for (Map.Entry<String,Object> entry : request.getParameters().entrySet()){

            if(entry.getValue() instanceof  String){

                addAttribute(serializer,entry.getKey(), (String) entry.getValue());

            }else if(entry.getValue() instanceof Integer){

                addAttribute(serializer,entry.getKey(), Integer.toString((Integer) entry.getValue()));

            }else if(entry.getValue() instanceof  Boolean){

                addAttribute(serializer,entry.getKey(), Boolean.toString((Boolean) entry.getValue()));

            }else{

                Log.w(SIIGRetrofitClient.class.getSimpleName(), "unexpected attribute data type , using its toString() method to build the WPS call, check if that is correct");

                addAttribute(serializer, entry.getKey(), entry.getValue().toString());
            }
        }

        serializer.endTag("wps", "DataInputs");

        serializer.setPrefix("wps", "wps");
        serializer.startTag("wps", "ResponseForm");
        serializer.startTag("wps", "RawDataOutput");
        serializer.attribute("", "mimeType", "text/xml; subtype=wfs-collection/1.0");
        serializer.setPrefix("ows", "ows");
        serializer.startTag("ows", "Identifier");
        serializer.text("result");
        serializer.endTag("ows", "Identifier");
        serializer.endTag("wps", "RawDataOutput");
        serializer.endTag("wps", "ResponseForm");

        serializer.endTag("wps", "Execute");

        serializer.endDocument();

        return writer.toString();


    }

    public static void addAttribute(final XmlSerializer serializer, final String id, final String data) throws IOException {
        serializer.setPrefix("wps", "wps");
        serializer.startTag("wps", "Input");
        serializer.setPrefix("ows", "ows");
        serializer.startTag("ows", "Identifier");
        serializer.text(id);
        serializer.endTag("ows", "Identifier");
        serializer.setPrefix("wps", "wps");
        serializer.startTag("wps", "Data");

        serializer.startTag("wps","LiteralData");
        serializer.text(data);
        serializer.endTag("wps", "LiteralData");

        serializer.endTag("wps","Data");

        serializer.endTag("wps", "Input");
    }

    public static void createHeader(final XmlSerializer serializer) throws IOException {
        serializer.attribute("", "version", "1.0.0");
        serializer.attribute("", "service", "WPS");
        serializer.attribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        serializer.attribute("", "xmlns", "http://www.opengis.net/wps/1.0.0");
        serializer.attribute("", "xmlns:wfs", "http://www.opengis.net/wfs");
//        serializer.attribute("", "xmlns:wps", "http://www.opengis.net/wps/1.0.0");
        serializer.attribute("", "xmlns:ows", "http://www.opengis.net/ows/1.1");
        serializer.attribute("", "xmlns:gml", "http://www.opengis.net/gml");
        serializer.attribute("", "xmlns:ogc", "http://www.opengis.net/ogc");
        serializer.attribute("", "xmlns:wcs", "http://www.opengis.net/wcs/1.1.1");
        serializer.attribute("", "xmlns:xlink", "http://www.w3.org/1999/xlink");
        serializer.attribute("", "xsi:schemaLocation", "http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd");

    }


}
