package it.geosolutions.android.siigmobile.geocoding;

import android.content.Context;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;

import it.geosolutions.android.siigmobile.Config;
import it.geosolutions.android.siigmobile.R;

/**
 * Created by Robert Oehler on 09.07.15.
 *
 * Extends android.support.v7.widget.SearchView to have all the listener methods
 * at on place and to pipe the callback into two methods
 *
 * doQuery(query) begin a query using this String
 *
 * suggestionClick(position) evaluate the connected adapter to get the data at this position
 *
 */
public class GeoCodingSearchView extends SearchView {

    private GeoCodingCallback mCallback;

    public GeoCodingSearchView(Context context) {
        super(context);
        this.setQueryHint(context.getString(R.string.search_hint));
        setup();
    }

    public GeoCodingSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setQueryHint(context.getString(R.string.search_hint));
        setup();
    }

    public GeoCodingSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.setQueryHint(context.getString(R.string.search_hint));
        setup();
    }

    /**
     * setup this search view
     */
    public void setup(){

        this.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {

                if (query.length() >= Config.SEARCHVIEW_CHARACTER_THRESHOLD) {
                    if (mCallback != null) {
                        mCallback.doQuery(query);
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {

                if (newText.length() >= Config.SEARCHVIEW_CHARACTER_THRESHOLD) {
                    if (mCallback != null) {
                        mCallback.doQuery(newText);
                    }
                    return true;
                }

                return false;
            }
        });

        this.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionClick(int position) {

                if(mCallback != null) {
                    mCallback.suggestionSelected(position);
                }
                return true;
            }

            @Override
            public boolean onSuggestionSelect(int position) {

                if (mCallback != null) {
                    mCallback.suggestionSelected(position);
                }
                return true;
            }
        });

    }

    public void setCallback(GeoCodingCallback mCallback) {
        this.mCallback = mCallback;
    }

    public interface GeoCodingCallback
    {
        void doQuery(final String query);

        void suggestionSelected(final int position);
    }
}
