package it.geosolutions.android.map.model.query;

import android.os.Parcel;

import it.geosolutions.android.map.model.Layer;

/**
 * Created by robertoehler on 31.08.15.
 */
public class WMSGetFeatureInfoTaskQuery extends WMSGetFeatureInfoQuery implements FeatureInfoTaskQuery {


    public WMSGetFeatureInfoTaskQuery(Parcel source) {
        super(source);

    }
    public WMSGetFeatureInfoTaskQuery(WMSGetFeatureInfoQuery q){
        super(q);
    }

    private Layer layer;
    private Integer start;
    private Integer limit;

    /**
     * Return start value
     * @return
     */
    public Integer getStart() {
        return start;
    }

    /**
     * Set start value
     * @param start
     */
    public void setStart(Integer start) {
        this.start = start;
    }

    /**
     * Return limit value
     * @return
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * Set limit value
     * @param limit
     */
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }
}
