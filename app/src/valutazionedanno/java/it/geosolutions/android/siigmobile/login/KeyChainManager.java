package it.geosolutions.android.siigmobile.login;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509ExtendedKeyManager;

/**
 * Simple activity based test that exercises the KeyChain API
 */

public class KeyChainManager extends X509ExtendedKeyManager {

	private Activity activity;
	private final Object aliasLock = new Object();
	private String alias = null;

	public KeyChainManager(Activity activity){
		this.activity = activity;
	}
	@Override
	public String chooseClientAlias(String[] keyTypes,
			Principal[] issuers,
			Socket socket) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

		if (prefs.getString("DefaultAlias", "").equals("")){
			KeyChain.choosePrivateKeyAlias(activity, new AliasResponse(), keyTypes, issuers, socket.getInetAddress().getHostName(), socket.getPort(), "My Test Certificate");
			String a = null;
			synchronized (aliasLock) {
				while (alias == null) {
					try {
						aliasLock.wait();
					} catch (InterruptedException ignored) {
					}
				}
				a = alias;
			}
			prefs.edit().putString("DefaultAlias", a).commit();
		}
		
		return prefs.getString("DefaultAlias", "");
	}
	@Override
	public String chooseServerAlias(String keyType,
			Principal[] issuers,
			Socket socket) {
		// not a client SSLSocket callback
		throw new UnsupportedOperationException();
	}
	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		try {
			X509Certificate[] certificateChain = KeyChain.getCertificateChain(activity, alias);
			return certificateChain;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (KeyChainException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		// not a client SSLSocket callback
		throw new UnsupportedOperationException();
	}
	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		// not a client SSLSocket callback
		throw new UnsupportedOperationException();
	}
	@Override
	public PrivateKey getPrivateKey(String alias) {
		try {
			PrivateKey privateKey = KeyChain.getPrivateKey(activity, alias);
			return privateKey;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (KeyChainException e) {
			throw new RuntimeException(e);
		}
	}

	private class AliasResponse implements KeyChainAliasCallback {
		@Override
		public void alias(String alias) {
			if (alias == null) {
				return;
			}
			synchronized (aliasLock) {
				KeyChainManager.this.alias = alias;
				aliasLock.notifyAll();
			}
		}
	}
}


