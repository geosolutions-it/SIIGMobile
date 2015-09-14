package it.geosolutions.android.map.model.query;

import android.os.Parcel;
import android.os.Parcelable;

import org.mapsforge.core.model.BoundingBox;

import java.util.HashMap;
import java.util.Map;

import it.geosolutions.android.map.wms.GetFeatureInfoConfiguration;

/**
 * Created by Robert Oehler on 31.08.15.
 *
 */
public class WMSGetFeatureInfoQuery extends BaseFeatureInfoQuery{

    private double lat, lon, radius;
    private String[] layers;
    private String[] queryLayers;
    private long[] mapSize;
    private BoundingBox bbox;
    private String version;
    private String crs;
    private String locale;
    private String authToken;
    private String styles;
    private String endPoint;
    private double pixel_X,pixel_Y;
    private GetFeatureInfoConfiguration.Locale localeConfig;
    private Map<String,String> additionalParams;

    /**
     * Method that return x coordinate of center
     * @return double
     */
    public double getLon() {
        return lon;
    }
    /**
     * Method that set x coordinate of center
     */
    public void setLon(double lon) {
        this.lon = lon;
    }
    /**
     * Method that return y coordinate of center
     * @return double
     */
    public double getLat() {
        return lat;
    }
    /**
     * Method that set y coordinate of center
     */
    public void setLat(double lat) {
        this.lat = lat;
    }
    /**
     * Method that return radius of circle
     * @return double
     */
    public double getRadius() {
        return radius;
    }
    /**
     * Method that set radius of circle
     */
    public void setRadius(double radius) {
        this.radius = radius;
    }

    public long[] getMapSize() {
        return mapSize;
    }

    public void setMapSize(long[] mapSize) {
        this.mapSize = mapSize;
    }

    public BoundingBox getBbox() {
        return bbox;
    }

    public void setBbox(BoundingBox bbox) {
        this.bbox = bbox;
    }

    public String[] getLayers() {
        return layers;
    }

    public void setLayers(String[] layers) {
        this.layers = layers;
    }

    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public double getPixel_Y() {
        return pixel_Y;
    }

    public void setPixel_Y(double pixel_Y) {
        this.pixel_Y = pixel_Y;
    }

    public double getPixel_X() {
        return pixel_X;
    }

    public void setPixel_X(double pixel_X) {
        this.pixel_X = pixel_X;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String[] getQueryLayers() {
        return queryLayers;
    }

    public void setQueryLayers(String[] queryLayers) {
        this.queryLayers = queryLayers;
    }

    public String getStyles() {
        return styles;
    }

    public void setStyles(String styles) {
        this.styles = styles;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public GetFeatureInfoConfiguration.Locale getLocaleConfig() {
        return localeConfig;
    }

    public void setLocaleConfig(GetFeatureInfoConfiguration.Locale config) {
        this.localeConfig = config;
    }

    /* (non-Javadoc)
 * @see android.os.Parcelable#describeContents()
 */
    @Override
    public int describeContents() {
        return 0;
    }
    /* (non-Javadoc)
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(lat);
        dest.writeDouble(lon);
        dest.writeDouble(radius);
        dest.writeInt(getMapSize().length);
        dest.writeLongArray(getMapSize());
        dest.writeMap(getAdditionalParams());
        dest.writeString(getAuthToken());
        dest.writeDouble(getPixel_X());
        dest.writeDouble(getPixel_Y());
        dest.writeSerializable(getBbox());
        dest.writeString(getStyles());
        dest.writeString(getVersion());
        dest.writeInt(getQueryLayers().length);
        dest.writeStringArray(getQueryLayers());
        dest.writeInt(getLayers().length);
        dest.writeStringArray(getLayers());
        dest.writeString(getLocale());
        dest.writeString(getCrs());
        dest.writeString(getEndPoint());
        dest.writeSerializable(getLocaleConfig());
    }

    /**
     * Constructor for class FeatureCircleQuery.
     * @param p
     */
    public WMSGetFeatureInfoQuery(Parcel p){
        super(p);
        lat = p.readDouble();
        lon = p.readDouble();
        radius = p.readDouble();

        int mapLength = p.readInt();
        mapSize = new long[mapLength];
        p.readLongArray(mapSize);

        additionalParams = new HashMap<>();
        p.readMap(additionalParams, this.getClass().getClassLoader());


        authToken = p.readString();
        pixel_X = p.readDouble();
        pixel_Y = p.readDouble();
        bbox = (BoundingBox) p.readSerializable();
        styles = p.readString();
        version = p.readString();

        int qLLength = p.readInt();
        queryLayers = new String[qLLength];
        p.readStringArray(queryLayers);

        int lLength = p.readInt();
        layers = new String[lLength];
        p.readStringArray(layers);

        locale  = p.readString();
        crs     = p.readString();
        endPoint= p.readString();

        localeConfig = (GetFeatureInfoConfiguration.Locale) p.readSerializable();
    }

    public WMSGetFeatureInfoQuery(){
    }

    public WMSGetFeatureInfoQuery(WMSGetFeatureInfoQuery q){
        super(q);
        setLat(q.getLat());
        setLon(q.getLon());
        setRadius(q.getRadius());
        setSrid(q.getSrid());
        setZoomLevel(q.getZoomLevel());
        setMapSize(q.getMapSize());
        setAdditionalParams(q.getAdditionalParams());
        setAuthToken(q.getAuthToken());
        setPixel_Y(q.getPixel_Y());
        setPixel_X(q.getPixel_X());
        setBbox(q.getBbox());
        setStyles(q.getStyles());
        setVersion(q.getVersion());
        setQueryLayers(q.getQueryLayers());
        setLayers(q.getLayers());
        setLocale(q.getLocale());
        setCrs(q.getCrs());
        setEndPoint(q.getEndPoint());
        setLocaleConfig(q.getLocaleConfig());
    }


    public static final Parcelable.Creator<WMSGetFeatureInfoQuery> CREATOR = new Parcelable.Creator<WMSGetFeatureInfoQuery>() {
        public WMSGetFeatureInfoQuery createFromParcel(Parcel in) {
            return new WMSGetFeatureInfoQuery(in);
        }

        public WMSGetFeatureInfoQuery[] newArray(int size) {
            return new WMSGetFeatureInfoQuery[size];
        }
    };


}
