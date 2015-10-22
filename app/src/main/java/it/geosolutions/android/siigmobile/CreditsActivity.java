package it.geosolutions.android.siigmobile;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * Activity to display the Credits
 */
public class CreditsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_display);

        setTitle(R.string.siig_credits_page_title);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.m_toolbar);
        setSupportActionBar(mToolbar);

        // Enable the Back Arrow in the Toolbar
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Se HTML formatted text in the page
        WebView webView = (WebView) findViewById(R.id.siig_info_page_text);
        webView.loadDataWithBaseURL("file:///android_asset/", getString(R.string.siig_credits_page_text), "text/html", "utf-8", null);
    }

    /**
     * Animate the close
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.out_to_down);
    }

    /**
     * React to the Back Arrow press as the back button press
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
