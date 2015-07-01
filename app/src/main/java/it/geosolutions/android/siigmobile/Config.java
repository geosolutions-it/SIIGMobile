package it.geosolutions.android.siigmobile;

/**
 * Configuration and default values
 * Created by Lorenzo on 24/06/2015.
 */
public class Config {

    public static String BASE_DIR_NAME = "/siig_mobile";

    public static float INITIAL_LATITUDE = 45.42082f;
    public static float INITIAL_LONGITUDE = 9.73312f;
    public static int INITIAL_ZOOMLEVEL = 7;

    public static String GRAFO_LAYER = "v_elab_std_1";
    public static String LAYERS_PREFIX = "v_elab_std";
    public static final String RESULT_PREFIX = "resultTable_";
    public static final String[] STYLES_PREFIX_ARRAY = {"grafo", "ambientale", "sociale", "totale" };
    public static final String[] RESULT_STYLES = {"result_ambientale", "result_sociale", "result_totale" };
    public static final int DEFAULT_STYLE = 3;

    public static final String RESULT = "result";

    public static String BASE_PACKAGE_URL = "http://demo.geo-solutions.it/share/csi_piemonte/destination_plus/mobile/siig_mobile.zip";
    public static long REQUIRED_SPACE = 1150000000L;

    public static final String PARAM_LAST_UNSAVED_DELETION = "last_unsafed_deletion";
    public static final long  UNSAVED_DELETION_INTERVAL = 24 * 60 * 60 * 1000; //one day

}
