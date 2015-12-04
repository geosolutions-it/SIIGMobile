package it.geosolutions.android.siigmobile.login;

import javax.net.ssl.SSLContext;

import cz.msebera.android.httpclient.conn.ssl.SSLSocketFactory;

/**
 * Created by Lorenzo on 01/12/2015.
 */
public class DestinationSSLSocketFactory extends SSLSocketFactory{
    public DestinationSSLSocketFactory(SSLContext sslContext) {
        super(sslContext);
    }
}
