package it.geosolutions.android.siigmobile.elaboration;

import org.mapsforge.core.model.GeoPoint;

import java.io.Serializable;

/**
 *
 * Class which contains the properties of an elaboration for a valutazione danno calculation
 * It contains only one "created" table which contains the data
 * but additionally location and radius of the calculation
 *
 * Created by Robert Oehler on 31.07.15.
 */
public class ElaborationResult  implements Serializable{

    private String userEditedName;
    private String userEditedDescription;

    private String resultTableName;

    private GeoPoint location;
    private int radius;


    public ElaborationResult(final String userEditedName,
                             final String userEditedDescription,
                             final String resultTableName,
                             final GeoPoint center,
                             final int radius) {
        this.userEditedName = userEditedName;
        this.userEditedDescription = userEditedDescription;
        this.resultTableName = resultTableName;
        this.location = center;
        this.radius = radius;
    }

    public String getResultTableName() {
        return resultTableName;
    }

    public void setResultTableName(String resultTableName) {
        this.resultTableName= resultTableName;
    }

    public String getUserEditedDescription() {
        return userEditedDescription;
    }

    public void setUserEditedDescription(String userEditedDescription) {
        this.userEditedDescription = userEditedDescription;
    }

    public String getUserEditedName() {
        return userEditedName;
    }

    public void setUserEditedName(String userEditedName) {
        this.userEditedName = userEditedName;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }
}
