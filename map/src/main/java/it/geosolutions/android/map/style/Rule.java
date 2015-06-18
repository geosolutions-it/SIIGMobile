package it.geosolutions.android.map.style;

/**
 * Created by Lorenzo on 18/06/2015.
 */
public class Rule {

    Filter filter;
    Symbolizer symbolizer;


    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Symbolizer getSymbolizer() {
        return symbolizer;
    }

    public void setSymbolizer(Symbolizer symbolizer) {
        this.symbolizer = symbolizer;
    }
}
