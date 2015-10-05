package it.geosolutions.android.map.spatialite.renderer;

import android.util.Log;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

import java.util.Iterator;

import eu.geopaparazzi.spatialite.database.spatial.core.GeometryIterator;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

/**
 * Created by Robert Oehler on 25.09.15.
 *
 * This a copy of the original GeometryIterator class which additionally
 * extracts a second column out of the database stmt (if available)
 * it is added to the Geometry object as "userdata"
 *
 */
public class LabelGeometryIterator extends GeometryIterator implements Iterator<Geometry>  {

    protected WKBReader wkbReader = new WKBReader();
    protected Stmt stmt;

    public LabelGeometryIterator( Database database, String query ) {
        super(database,query);
        try {
            stmt = database.prepare(query);
        } catch (jsqlite.Exception e) {
            Log.e(getClass().getSimpleName(), "exception prepare stmt", e);
        }
    }

    @Override
    public boolean hasNext() {
        if (stmt == null)
            return false;
        try {
            return stmt.step();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(),"exception stmt step",e);
            return false;
        }
    }

    @Override
    public Geometry next() {
        if (stmt == null)
            return null;
        try {
            byte[] geomBytes = stmt.column_bytes(0);
            Geometry geometry = wkbReader.read(geomBytes);
            if(stmt.column_count() > 1) {
                Object o = stmt.column(1);
                if(o != null){
                    geometry.setUserData(o);
                }
            }
            return geometry;
        } catch (java.lang.Exception e) {
            Log.e(getClass().getSimpleName(),"exception next",e);
        }
        return null;
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
