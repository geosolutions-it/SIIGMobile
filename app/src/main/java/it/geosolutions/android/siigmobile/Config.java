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

    public static int SEARCHVIEW_CHARACTER_THRESHOLD = 3;
    public static int SUGGESTIONS_THRESHOLD = 5;
    public static byte PLACE_SELECTED_ZOOM_TO_LEVEL = 14;

    public final static String CURSOR_TITLE = "title";
    public final static String CURSOR_LAT = "lat";
    public final static String CURSOR_LON = "lon";

    public static String LAYERS_PREFIX = "v_elab_std";
    public static String[] STYLES_PREFIX_ARRAY = {"grafo", "ambientale", "sociale", "totale" };

    public static String BASE_PACKAGE_URL = "http://demo.geo-solutions.it/share/csi_piemonte/destination_plus/mobile/siig_mobile.zip";
    public static long REQUIRED_SPACE = 1150000000L;

}
