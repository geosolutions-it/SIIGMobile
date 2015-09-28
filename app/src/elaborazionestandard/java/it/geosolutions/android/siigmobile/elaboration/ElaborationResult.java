package it.geosolutions.android.siigmobile.elaboration;

import java.io.Serializable;

/**
 *
 * Class which contains the properties of an elaboration
 *
 * Created by Robert Oehler on 31.07.15.
 */
public class ElaborationResult  implements Serializable{

    private String userEditedName;
    private String userEditedDescription;

    private String riskTableName;
    private String streetTableName;


    public ElaborationResult(String userEditedName,String userEditedDescription,String riskTableName,String streetTableName) {
        this.userEditedName = userEditedName;
        this.userEditedDescription = userEditedDescription;
        this.riskTableName = riskTableName;
        this.streetTableName = streetTableName;
    }


    public String getStreetTableName() {
        return streetTableName;
    }

    public void setStreetTableName(String streetTableName) {
        this.streetTableName = streetTableName;
    }

    public String getRiskTableName() {
        return riskTableName;
    }

    public void setRiskTableName(String riskTableName) {
        this.riskTableName = riskTableName;
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
}
