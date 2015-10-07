package it.geosolutions.android.siigmobile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

public class InfoDisplayActivity extends AppCompatActivity {

    public static String EXTRA_TEXT_INDEX = "EXTRA_TEXT_INDEX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_display);

        int textToDisplayIdx = 0;
        if(getIntent() != null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                textToDisplayIdx = 0;
            } else {
                textToDisplayIdx = extras.getInt(EXTRA_TEXT_INDEX, 0);
            }
        }
        String info_text =  getString(R.string.siig_info_page_text);
        String [] texts = getResources().getStringArray(R.array.info_texts);
        if(texts != null){
            if(textToDisplayIdx >= 0 && textToDisplayIdx < texts.length){
                info_text = texts[textToDisplayIdx];
            }
        }

        setTitle(R.string.siig_info_page_title);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.m_toolbar);
        setSupportActionBar(mToolbar);

        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        WebView webView = (WebView) findViewById(R.id.siig_info_page_text);
        webView.loadDataWithBaseURL(null, info_text, "text/html", "utf-8", null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.out_to_down);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
