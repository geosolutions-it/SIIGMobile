package it.geosolutions.android.siigmobile;

/**
 * Configuration and default values
 * Created by Lorenzo on 24/06/2015.
 */
public class Config {

    public static final String DESTINATION_WMS_URL = "http://tst-destinationpa.territorio.csi.it/geoserver/mobile/wms";
    public static final String DESTINATION_AUTHORIZATION = "Basic c3VwZXI6c3VwZXI=";
    public static final int WPS_MAX_FEATURES = 500;
    public static final int FORMULA_RISK = 141;
    public static final int FORMULA_STREET = 22;
    public static String BASE_DIR_NAME = "/siig_mobile";

    public static float INITIAL_LATITUDE = 45.42082f;
    public static float INITIAL_LONGITUDE = 9.73312f;
    public static int INITIAL_ZOOMLEVEL = 7;

    public static String GRAFO_LAYER = "v_elab_std_1";
    public static int SEARCHVIEW_CHARACTER_THRESHOLD = 3;
    public static int SUGGESTIONS_THRESHOLD = 5;
    public static byte PLACE_SELECTED_ZOOM_TO_LEVEL = 14;

    public final static String CURSOR_TITLE = "title";
    public final static String CURSOR_LAT = "lat";
    public final static String CURSOR_LON = "lon";

    public static String LAYERS_PREFIX = "v_elab_std";
    public static final String RESULT_PREFIX = "resultTable_";
    public static final String[] STYLES_PREFIX_ARRAY = {"grafo", "ambientale", "sociale", "totale" };
    public static final String[] RESULT_STYLES = {"result_ambientale", "result_sociale", "result_totale", "result_pis" };
    public static final int DEFAULT_STYLE = 3;

    public static final String[] WMS_LAYERS = {"grafo_stradale", "rischio_totale_ambientale", "rischio_totale_sociale", "rischio_totale", "pis" };
    public static final String[] WMS_ENV = {
            "false",
            "low:5.994576e-9;medium:0.015552002997288;max:0.031104",
            "low:1.53216e-11;medium:0.0002868048076608;max:0.0005736096",
            "lowsociale:1.53216e-11;mediumsociale:0.0002868048076608;maxsociale:0.0005736096;lowambientale:5.994576e-9;mediumambientale:0.015552002997288;maxambientale:0.031104",
            "low:0.005;medium:2.0;max:4.0"};
    public static final String[] WMS_RISKPANEL = {"true", "true", "true", "true" };
    public static final String[] WMS_DEFAULTENV = {
            "false",
            "low:5.994576e-9;medium:0.015552002997288;max:0.031104",
            "low:1.53216e-11;medium:0.0002868048076608;max:0.0005736096",
            "lowsociale:1.53216e-11;mediumsociale:0.0002868048076608;maxsociale:0.0005736096;lowambientale:5.994576e-9;mediumambientale:0.015552002997288;maxambientale:0.031104" };

    public static final String RESULT_ITEM = "result_item";
    //public static final String RESULT_TABLENAME = "result_tableName";
    //public static final String RESULT_FORMULA = "result_formula";

    public static String BASE_PACKAGE_URL = "http://demo.geo-solutions.it/share/csi_piemonte/destination_plus/mobile/siig_mobile_2.zip";
    public static long REQUIRED_SPACE = 290_000_000L;

    public static final String PARAM_LAST_UNSAVED_DELETION = "last_unsaved_deletion";
    public static final long  UNSAVED_DELETION_INTERVAL = 24 * 60 * 60 * 1000; //one day

    /**
     * Maximum possible latitude coordinate for this project.
     */
    public static final double LATITUDE_MAX = 47.1205788012735;

    /**
     * Minimum possible latitude coordinate for this project.
     */
    public static final double LATITUDE_MIN = 44.034474283395;

    /**
     * Maximum possible longitude coordinate for this project.
     */
    public static final double LONGITUDE_MAX = 12.5041443854095;

    /**
     * Minimum possible longitude coordinate for this project.
     */
    public static final double LONGITUDE_MIN = 6.59220922465439;


    public static final int WMS_INFO_FEATURE_COUNT = 10;

    public static final String[] DESTINATION_SUPPORTED_LANGUAGES = { "en","it","de","fr"};

    public final static String WMS_GETINFO_VERSION = "1.1.1";
    public final static String WMS_GETINFO_FORMAT = "image/png8";
    public final static String WMS_GETINFO_STYLES = "";
    public final static int    WMS_GETINFO_EPSG = 3857;

    public final static String WMS_GETINFO_PARAMETER_ENV = "ENV";
    public final static String WMS_GETINFO_PARAMETER_FEATURE_COUNT = "FEATURE_COUNT";
    public final static String WMS_GETINFO_PARAMETER_FORMAT = "FORMAT";

    public static final float MAP_SCALE = 1.6f;

    public static int RESULT_MIN_ZOOMLEVEL = 15;

}
