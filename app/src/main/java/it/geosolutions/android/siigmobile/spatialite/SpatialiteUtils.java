package it.geosolutions.android.siigmobile.spatialite;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import com.newrelic.agent.android.instrumentation.Trace;
import com.newrelic.agent.android.instrumentation.MetricCategory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

import org.mapsforge.core.model.BoundingBox;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import it.geosolutions.android.map.wfs.geojson.feature.Feature;
import it.geosolutions.android.siigmobile.BuildConfig;
import it.geosolutions.android.siigmobile.Config;
import it.geosolutions.android.siigmobile.wps.CRSFeatureCollection;
import jsqlite.*;
import jsqlite.Exception;

/**
 * Created by Robert Oehler on 29.06.15.
 *
 */
public class SpatialiteUtils {

    public final static String TAG = SpatialiteUtils.class.getSimpleName();

    public final static String DB_NAME = "local_data.sqlite";
    public final static String APP_PATH = "siig_mobile";

    public final static String NAMES_TABLE = "names_table";

    private final static String CREATED_TABLE_NAME = "CREATED_TABLE_NAME";
    private final static String USER_RESULT_NAME = "USER_RESULT_NAME";
    private final static String USER_RESULT_DESCRIPTION = "USER_RESULT_DESCRIPTION";

    private final static String RESULT_ID = "RESULT_ID";
    private final static String CRS = "CRS";
    private final static String GEOMETRY = "GEOMETRY";
    private final static String ID_GEO_ARCO = "id_geo_arco";
    private final static String RISCHIO1 = "rischio1";
    private final static String RISCHIO2 = "rischio2";
    private final static String ISPIS = "ispis";
    private final static String ID = "id";


    /**
     * Convenience Method for opening the default local database
     * containing the results of the WPS Call operations
     * @param context context to open the DB
     * @return the database reference
     */
    public static Database openSpatialiteDB(final Context context){
        return SpatialiteUtils.openSpatialiteDB(context, APP_PATH + "/" + DB_NAME);
    }

    /**
     * Opens or creates if not present the database at the path provided
     * A reference to it or null is returned if an invalid context or databasePath were passed
     * @param context the context to use
     * @param databasePath the path to the database file (may not exist yet)
     * @return the database reference or null
     */
    public static Database openSpatialiteDB(final Context context, final String databasePath){

        if(	context == null
                || databasePath == null
                || databasePath.isEmpty()){
            Log.w(TAG, "Cannot open Database, invalid parameters.");
            return null;

        }

        Database spatialiteDatabase = null;

        try {

            File sdcardDir = Environment.getExternalStorageDirectory();
            File spatialDbFile = new File(sdcardDir, databasePath);

            boolean existed = spatialDbFile.exists();

            spatialiteDatabase = new jsqlite.Database();
            spatialiteDatabase.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                    | jsqlite.Constants.SQLITE_OPEN_CREATE);

            if(!existed){
                //did not exist yet, create spatial data tables
                spatialiteDatabase.exec("SELECT InitSpatialMetaData();", null);
                //create result names table
                if(!tableExists(spatialiteDatabase,NAMES_TABLE)){
                    createNamesTable(spatialiteDatabase);
                }
            }

            if(BuildConfig.DEBUG) {
                Log.v(TAG, spatialiteDatabase.dbversion());
            }
        } catch (Exception e) {
            Log.e(TAG, "error opening db",e);
        }

        return spatialiteDatabase;
    }

    /**
     * checks if the table exists in this database
     * @param db the database to check
     * @param tableName the table to check
     * @return if this table is present in the database
     */
    public static boolean tableExists(final Database db, final String tableName) {


        if (db != null){
            String query = "SELECT name FROM sqlite_master WHERE type='table' AND name='"+tableName+"'";

            boolean found = false;
            try {

                Stmt stmt = db.prepare(query);
                if( stmt.step() ) {
                    String nomeStr = stmt.column_string(0);
                    found = true;
                    if(BuildConfig.DEBUG) {
                        Log.v(TAG, "Found table: " + nomeStr);
                    }
                }

                stmt.close();
                return found;

            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
                return false;
            }
        }
        return false;

    }

    /**
     * creates the names table, containing created and user edited names
     * of the result tables of a WPS call
     * @param db the db to create the table in
     * @return if this operation was successful, i.e. without exceptions
     */
    public static boolean createNamesTable(final Database db){

        return exec(db,"CREATE TABLE '"+ NAMES_TABLE + "'(" +
                "'" + RESULT_ID + "' INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "'" + CREATED_TABLE_NAME + "' TEXT, " +
                "'" + USER_RESULT_NAME + "' TEXT, " +
                "'" + USER_RESULT_DESCRIPTION + "' TEXT,"+
                "'" + ISPIS + "' BOOLEAN);");

    }

    /**
     * Inserts a created table name into the names table
     * @param db the db to insert
     * @param createdTableName the name of the table
     * @return if this operation was successful, i.e. without exceptions
     */
    public static boolean insertIntoNamesTable(final Database db, final String createdTableName, boolean ispis){

        return exec(db,"INSERT INTO " + NAMES_TABLE+
                "("+RESULT_ID+","+CREATED_TABLE_NAME+","+USER_RESULT_NAME+","+USER_RESULT_DESCRIPTION+","+ISPIS+")"+
                " VALUES (NULL,'"+createdTableName+"',NULL,NULL,"+(ispis?"1":"0")+")");

    }

    /**
     * update the names table entry with user created name
     * @param db the db to update
     * @param createdTableName the created name
     * @param userResultName the user edited name
     * @param userResultDescription the user edited description
     * @return if this operation was successful, i.e. without exceptions
     */
    public static boolean updateNameTableWithUserData(final Database db, final String createdTableName, final String userResultName, final String userResultDescription){

        return exec(db,"UPDATE "+ NAMES_TABLE + " SET "+USER_RESULT_NAME+" = '"+userResultName+"' , "+USER_RESULT_DESCRIPTION+" = '"+userResultDescription+"' WHERE "+ CREATED_TABLE_NAME + " = '" + createdTableName+"';");
    }

    /**
     * deletes an entry in the names tables
     * @param db the db to work on
     * @param createdTableName the name of the entry to delete
     * @return if this operation was successful, i.e. without exceptions
     */
    public static boolean deleteNamesTableEntry(final Database db, final String createdTableName){

        return exec(db,"DELETE FROM "+NAMES_TABLE+" WHERE "+CREATED_TABLE_NAME+"="+createdTableName);
    }

    /**
     * executes a sql statement
     * @param db the db to work on
     * @param sql the statement to execute
     * @return if the execution was successful, i.e. without exceptions
     */
    private static boolean exec(final Database db, final String sql){
        try{
            final Stmt stmt = db.prepare(sql);
            stmt.step();
            stmt.close();
            return true;
        } catch (jsqlite.Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }
    }

    /**
     * Returns all "saved" rows of the names table
     * as String[]{createdTableName,userCreatedName,userEditedDescription}
     * "saved" means the usercreatedname is not null
     * note that the desciption can be null
     * @param db the db to work on
     * @return a list of pairs
     */
    @Trace(category = MetricCategory.DATABASE)
    public static ArrayList<String[]> getSavedResultTableNames(final Database db){

        try{
            ArrayList<String[]> names = new ArrayList<>();
            Stmt stmt = db.prepare("SELECT "+CREATED_TABLE_NAME+","+USER_RESULT_NAME+","+USER_RESULT_DESCRIPTION+","+ISPIS+" FROM "+ NAMES_TABLE +" WHERE "+USER_RESULT_NAME+" IS NOT NULL;");
            while(stmt.step()){

                names.add(new String[]{stmt.column_string(0),stmt.column_string(1),stmt.column_string(2)});
            }

            return names;

        } catch (jsqlite.Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }
    /**
     * Returns all "unsaved" rows of the names table
     *
     * "unsaved" means the usercreatedname is null
     * @param db the db to work on
     * @return a list of pairs
     */
    public static ArrayList<String> getUnSavedResultTableNames(final Database db){

        try{
            ArrayList<String> names = new ArrayList<>();
            Stmt stmt = db.prepare("SELECT "+CREATED_TABLE_NAME+" FROM "+ NAMES_TABLE +" WHERE "+USER_RESULT_NAME+" IS NULL;");
            while(stmt.step()){

                names.add(stmt.column_string(0));
            }
            return names;

        } catch (jsqlite.Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    /**
     * retrieves the user edited name and description for a table
     * @param db the db to work on
     * @param createdTableName the internal name of the table
     * @return a Pair<String,String> containing the user edited name and description of the table
     */
    public static Pair<String,String> getTableUserNameAndDescription(final Database db, final String createdTableName){

        final String sql = "SELECT "+USER_RESULT_NAME+" , "+USER_RESULT_DESCRIPTION+" FROM "+NAMES_TABLE+" WHERE "+CREATED_TABLE_NAME+"='"+createdTableName+"';";


        try{
            final Stmt stmt = db.prepare(sql);

            if (stmt.step()) {
                final String name = stmt.column_string(0);
                final String desc = stmt.column_string(1);
                stmt.close();

                return new Pair<>(name, desc);
            }

        } catch (jsqlite.Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return null;
    }

    /**
     * creates the table containing the features of a result in the database
     *
     * @param db the database to create the table in
     * @param tableName the name of this table
     * @return if this operation was successful, i.e. without exceptions
     */
    public static boolean createResultTable(final Database db, final String tableName, final int crs, final String geometryType) {

        final String create_resultTable_stmt = "CREATE TABLE '" + tableName + "' (" +
                "'" + RESULT_ID + "' INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "'" + ID + "' INTEGER, "+
                "'" + ID_GEO_ARCO + "' INTEGER, "+
                "'" + RISCHIO1 + "' DOUBLE, "+
                "'" + RISCHIO2 + "' DOUBLE, "+
                "'" + CRS + "' INTEGER);";


        try {
            Stmt stmt01 = db.prepare(create_resultTable_stmt);
            if (stmt01.step()) {
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "Result Table created");
                }
            }

            final String add_geom_stmt = "SELECT AddGeometryColumn('" + tableName + "', '" + GEOMETRY + "', " + crs + " , '" + geometryType + "', 'XY');";

            stmt01 = db.prepare(add_geom_stmt);
            if (stmt01.step()) {
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "Geometry Column Added " + stmt01.column_string(0));
                }
            }

            final String create_idx_stmt = "SELECT CreateSpatialIndex('" + tableName + "', '" + GEOMETRY + "');";

            stmt01 = db.prepare(create_idx_stmt);
            if (stmt01.step()) {
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "Index Created");
                }
            }

            stmt01.close();

            return true;

        } catch (jsqlite.Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }

    }

    /**
     * saves the result of a WPS query to the local database
     * The table name is randomly created and returned
     * this could take an notable amount of time and should be done in a background thread
     *
     * @param db the database to write to
     * @param result the data to write
     * @return  a Pair containing if the operation was successful, i.e.without exceptions and the name of the created table in case of success
     */
    public static Pair<Boolean,String> saveResult(final Database db, final CRSFeatureCollection result, final String geomType, boolean ispis) {

        String tableName;

        Random random = new Random();

        do {
            int n = Math.abs(random.nextInt(10000));

            tableName = Config.RESULT_PREFIX + n;

        }while(SpatialiteUtils.tableExists(db, tableName));

        String crs = (String) result.crs.properties.get("name");
        crs = crs.substring(crs.lastIndexOf(":") + 1);
        int crs_id = 0;
        try{
            crs_id = Integer.parseInt(crs);
        }catch(NumberFormatException e){
            Log.e(TAG, "exception parsing crs id",e);
        }

        //save tableName
        if(!insertIntoNamesTable(db,tableName, ispis)){
            Log.w(TAG, "could not save table name "+tableName);
            return new Pair<>(false, "error inserting "+tableName+" into names table");
        }

        if(!createResultTable(db,tableName, crs_id, geomType)){
            Log.w(TAG, "could not create table "+tableName);
            deleteNamesTableEntry(db,tableName);
            return new Pair<>(false,"error creating table "+ tableName);
        }

        Stmt stmt = null;
        try {

            int id, arc_id, total_inserted = 0;
            double risk1, risk2;

            //save feature to table
            for (Feature feature : result.features) {

                id = Integer.parseInt(feature.id);
                arc_id = (int) Math.rint((Double) feature.properties.get(ID_GEO_ARCO)); //is parsed by GSON as double, why ?
                risk1 = (Double) feature.properties.get(RISCHIO1);
                risk2 = (Double) feature.properties.get(RISCHIO2);

                if(feature.geometry == null){
                    Log.e(TAG, "geometry object null");
                    return new Pair<>(false,"geometry object null");
                }
                stmt = db.prepare("INSERT INTO " + tableName +
                        " (" + RESULT_ID + ", " + ID + ", " + ID_GEO_ARCO + ", " + RISCHIO1 + ", " + RISCHIO2 + ", " + CRS + ", " + GEOMETRY + ")" +
                        " VALUES (NULL, " + id + " , " + arc_id + " , " + risk1 + " , " + risk2 + " , " + crs_id + ", " +
                        "ST_GeomFromText('" + feature.geometry.toString() + "', " + crs_id + "));");

                stmt.step();

                total_inserted+=1;
                if(total_inserted % 100 == 0){
                    Log.d(TAG, "Inserted "+total_inserted);
                }
            }
            if(stmt != null) {
                stmt.close();
            }

            return new Pair<>(true,tableName);


        } catch (jsqlite.Exception e) {

            Log.e(TAG, "exception closing stmt", e);
            return new Pair<>(false,"exception inserting values into table "+tableName);

        }
    }

    /**
     * Loads a single WPS risk call result from the database
     * @param db the db to query
     * @param tableName the internal name of the table
     * @return the result as CRSFeatureCollection
     */
    @Trace(category = MetricCategory.DATABASE)
    public static CRSFeatureCollection loadResult(final Database db, final String tableName){

        try {

            final CRSFeatureCollection result = new CRSFeatureCollection();

            final WKBReader wkbReader = new WKBReader();

            result.features = new ArrayList<>();

            result.tableName = tableName;

            Stmt stmt = db.prepare("SELECT "+RESULT_ID+" , "+ ID+" , "+ID_GEO_ARCO+" , "+RISCHIO1+" , "+RISCHIO2+" , "+CRS+" , ST_AsBinary(CastToXY("+GEOMETRY+")) as "+GEOMETRY+" FROM "+tableName+" asc;");

            int featureCount = 0;

            while (stmt.step()) {
                //feature

                Feature feature = new Feature();

                feature.properties = new HashMap<>();

                final int colCount = stmt.column_count();

                for (int colPos = 0; colPos < colCount; colPos++) {

                    String columnName = stmt.column_name(colPos);

                    if (columnName.equalsIgnoreCase("GEOMETRY")) {

                        byte[] geomBytes = stmt.column_bytes(colPos);

                        if (geomBytes != null) {

                            try {

                                feature.geometry = wkbReader.read(geomBytes);

                            } catch (com.vividsolutions.jts.io.ParseException e) {
                                Log.e(TAG, "Error reading geometry");
                            }
                        }else{
                            Log.w(TAG, "geometry for result "+ tableName + " is null");
                        }
                    } else if (columnName.equalsIgnoreCase(ID) || columnName.equalsIgnoreCase(ID_GEO_ARCO) || columnName.equalsIgnoreCase(RESULT_ID)) {

                        feature.properties.put(columnName, stmt.column_int(colPos));

                    } else if (columnName.equalsIgnoreCase(RISCHIO1) || columnName.equalsIgnoreCase(RISCHIO2)) {

                        feature.properties.put(columnName, stmt.column_double(colPos));

                    }else if(columnName.equals(CRS) && result.crs == null){

                        result.crs = new CRSFeatureCollection.Crs(stmt.column_int(colPos));
                    }
                }

                result.features.add(feature);

                featureCount++;
            }

            result.totalFeatures = featureCount;

            stmt.close();

            return result;

        } catch (jsqlite.Exception e) {
            Log.e(TAG, "exception loading result " + tableName + " from local db", e);
        }

        return null;
    }

    /**
     * deletes a result table and its spatial index
     * according to http://www.gaia-gis.it/gaia-sins/spatialite-tutorial-2.3.1.html
     * @param db the db to use
     * @param createdTableName the table to delete
     * @return if this operation was successfull, i.e. without errors
     */
    public static boolean deleteResult(final Database db, final String createdTableName){

        //statements to execute, don't change the order deliberately
        String[] sqls = new String[]{

                "SELECT DisableSpatialIndex('" + createdTableName + "', '" + GEOMETRY + "');",
                "SELECT DiscardGeometryColumn('" + createdTableName + "', '" + GEOMETRY + "');",
                //TODO deleting the spatial index table currently fails
                //"DROP TABLE  idx_"+createdTableName+"_"+ GEOMETRY,
                "DROP TABLE " + createdTableName
                //"VACUUM" //TODO recommended by spatialite, fails also
        };

        boolean success = true;

        for (String sql : sqls) {
            try {

                //using db.exec() instead of stmt.prepare()/step()
                //allows execute the drop result table con success
                //TODO after upgrading sqlite/spatialite consider switching back to Stmt
                //TODO as it gives feedback about the execution result
                db.exec(sql, null);

                if(BuildConfig.DEBUG) {
                    Log.i(TAG, "success executing " + sql);
                }

            } catch (jsqlite.Exception  e) {
                Log.e(TAG, "exception executing" + sql , e);
                success = false;
            }
        }

        return success;

    }
    public static boolean deleteResultFromNamesTable(final Database db, final String createdTableName){

        final String sql = "DELETE from "+NAMES_TABLE+" WHERE "+CREATED_TABLE_NAME+" ='"+createdTableName+"';";

        return exec(db,sql);

    }

    /**
     * Calculates a bounding box in lat/lon coordinates for the features of a table
     * The feature data is transformed to 4326 to achieve a suitable Mapsforge BoundingBox
     * @param context a context to open a database
     * @param tableName the table to operate om
     * @return the BoundingBox or null if an exception occurred
     */
    public static BoundingBox getBoundingBoxForSpatialiteTable(final Context context, final String tableName){

        try {
            final Database db = SpatialiteUtils.openSpatialiteDB(context);

            final Stmt stmt = db.prepare("SELECT ST_AsBinary(CastToXY(ST_Transform(GEOMETRY, 4326))) FROM " + tableName);

            final WKBReader wkbReader = new WKBReader();

            stmt.step();

            byte[] geomBytes = stmt.column_bytes(0);

            final Geometry geometry = wkbReader.read(geomBytes);

            //create a boundingbox
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

            for (Coordinate coord : geometry.getCoordinates()) {

                if (coord.x > maxX) {
                    maxX = coord.x;
                }
                if (coord.x < minX) {
                    minX = coord.x;
                }
                if (coord.y > maxY) {
                    maxY = coord.y;
                }
                if (coord.y < minY) {
                    minY = coord.y;
                }
            }
            if(BuildConfig.DEBUG) {
                Log.i(TAG, String.format("bbox : \nminX %f\nmaxX %f\nminY %f\nmaxY %f", minX, maxX, minY, maxY));
            }

            final BoundingBox bb = new BoundingBox(minY,minX,maxY,maxX);

            stmt.close();

            db.close();

            return bb;

        }catch(jsqlite.Exception | ParseException e){
            Log.e(TAG, "error querying bounding box for table "+ tableName,e);

            return null;
        }

    }
}