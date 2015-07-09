package it.geosolutions.android.siigmobile.legend;

/**
 * Created by Robert Oehler on 09.07.15.
 *
 */
public class LegendItem
{
    public String title;
    public int color;

    public LegendItem(final String pTitle, final int pColor){

        this.title = pTitle;
        this.color = pColor;
    }
}