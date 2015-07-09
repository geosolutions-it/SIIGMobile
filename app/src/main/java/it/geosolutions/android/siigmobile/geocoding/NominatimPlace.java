package it.geosolutions.android.siigmobile.geocoding;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Robert Oehler on 08.07.15.
 *
 * Pojo object to parse the response of a NominatimGeocoder using Gson
 *
 */
public class NominatimPlace {

    @SerializedName("place_id")
    private String placeId;
    private String licence;
    @SerializedName("osm_type")
    private String osmType;
    @SerializedName("osm_id")
    private String osmId;
    private List<String> boundingbox = new ArrayList<>();
    private String lat;
    private String lon;
    @SerializedName("display_name")
    private String displayName;
    @SerializedName("class")
    private String _class;
    private String type;
    private Double importance;
    private String icon;

    /**
     *
     * @return
     * The placeId
     */
    public String getPlaceId() {
        return placeId;
    }

    /**
     *
     * @param placeId
     * The place_id
     */
    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    /**
     *
     * @return
     * The licence
     */
    public String getLicence() {
        return licence;
    }

    /**
     *
     * @param licence
     * The licence
     */
    public void setLicence(String licence) {
        this.licence = licence;
    }

    /**
     *
     * @return
     * The osmType
     */
    public String getOsmType() {
        return osmType;
    }

    /**
     *
     * @param osmType
     * The osm_type
     */
    public void setOsmType(String osmType) {
        this.osmType = osmType;
    }

    /**
     *
     * @return
     * The osmId
     */
    public String getOsmId() {
        return osmId;
    }

    /**
     *
     * @param osmId
     * The osm_id
     */
    public void setOsmId(String osmId) {
        this.osmId = osmId;
    }

    /**
     *
     * @return
     * The boundingbox
     */
    public List<String> getBoundingbox() {
        return boundingbox;
    }

    /**
     *
     * @param boundingbox
     * The boundingbox
     */
    public void setBoundingbox(List<String> boundingbox) {
        this.boundingbox = boundingbox;
    }

    /**
     *
     * @return
     * The lat
     */
    public String getLat() {
        return lat;
    }

    /**
     *
     * @param lat
     * The lat
     */
    public void setLat(String lat) {
        this.lat = lat;
    }

    /**
     *
     * @return
     * The lon
     */
    public String getLon() {
        return lon;
    }

    /**
     *
     * @param lon
     * The lon
     */
    public void setLon(String lon) {
        this.lon = lon;
    }

    /**
     *
     * @return
     * The displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     *
     * @param displayName
     * The display_name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     *
     * @return
     * The _class
     */
    public String getClass_() {
        return _class;
    }

    /**
     *
     * @param _class
     * The class
     */
    public void setClass_(String _class) {
        this._class = _class;
    }

    /**
     *
     * @return
     * The type
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @param type
     * The type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return
     * The importance
     */
    public Double getImportance() {
        return importance;
    }

    /**
     *
     * @param importance
     * The importance
     */
    public void setImportance(Double importance) {
        this.importance = importance;
    }

    /**
     *
     * @return
     * The icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     *
     * @param icon
     * The icon
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }
}
