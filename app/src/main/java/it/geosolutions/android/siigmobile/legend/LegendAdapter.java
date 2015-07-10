package it.geosolutions.android.siigmobile.legend;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import it.geosolutions.android.map.style.AdvancedStyle;
import it.geosolutions.android.map.style.Rule;
import it.geosolutions.android.siigmobile.R;

/**
 * Created by Robert Oehler on 09.07.15.
 *
 * Adapter for legend items which reside
 *
 * as title-color combination inside the legends listview
 *
 */
public class LegendAdapter extends ArrayAdapter<LegendItem>{

    private Context context;
    private AdvancedStyle mCurrentStyle;

    public LegendAdapter(Context context) {
        super(context, R.layout.legend_item, new ArrayList<LegendItem>());

        this.context = context;
    }

    /**
     * Applies a new style
     *
     * Uses the fiels "label" and "strokecolor" of the symbolizer of the rules of a style
     *
     * @param style the style to apply
     */
    public void applyStlye(AdvancedStyle style){

        //avoid multiple invalidation of the same style
        if(mCurrentStyle != null && style.equals(mCurrentStyle)){
            return;
        }

        mCurrentStyle = style;

        this.clear();

        if(style.rules != null) {

            for (Rule rule : style.rules) {

                //ensure label is available or use a default label
                String label = rule.getSymbolizer().title == null ? "no label found" : rule.getSymbolizer().title;

                //assuming stroke color is always not null and a hex color
                this.add(new LegendItem(label, Color.parseColor(rule.getSymbolizer().strokecolor)));

            }
        }else{
            //no rules ->> this is grafo style
            //TODO add rule to grafo too
            this.add(new LegendItem(context.getResources().getStringArray(R.array.grafo)[0],context.getResources().getColor(R.color.grafo)));
        }

        this.notifyDataSetChanged();

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        RelativeLayout view;

        if(convertView == null){

            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = (RelativeLayout) li.inflate(R.layout.legend_item, parent, false);

        }else{

            view = (RelativeLayout) convertView;

        }
        final TextView title_tv  = (TextView)  view.findViewById(R.id.legend_title_tv);
        final ImageView color_iv = (ImageView) view.findViewById(R.id.legend_color_iv);

        final LegendItem item = getItem(position);

        title_tv.setText(item.title);

        color_iv.setBackgroundColor(item.color);

        return view;
    }
}
