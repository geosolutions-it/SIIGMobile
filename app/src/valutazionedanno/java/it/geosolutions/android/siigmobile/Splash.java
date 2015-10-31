package it.geosolutions.android.siigmobile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import it.geosolutions.android.siigmobile.login.LoginActivity;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final Intent intent = new Intent(this, LoginActivity.class);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final int sleep = BuildConfig.DEBUG ? 500 : 5000;
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    if(BuildConfig.DEBUG){
                        Log.e(this.getClass().getSimpleName(), "Splash screen interrupted", e);
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(intent);
                        finish();
                    }
                });
            }
        }).start();

    }

}
