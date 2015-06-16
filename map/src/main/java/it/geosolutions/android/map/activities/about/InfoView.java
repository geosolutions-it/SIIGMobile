package it.geosolutions.android.map.activities.about;

import it.geosolutions.android.map.R;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

/**
 * Simple activity to display the info web page from the assets folder.
 */
public class InfoView extends AppCompatActivity {
        
        WebView mWebView;
        
        @SuppressLint("SetJavaScriptEnabled")
		@Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.map_about);
                setTitle(R.string.about_mapstore);
                mWebView = (WebView) findViewById(R.id.webview);
                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.loadUrl("file:///android_asset/about/info.html");
                
        }
        

        @Override
        protected void onResume() {
                super.onResume();
                
        }
}