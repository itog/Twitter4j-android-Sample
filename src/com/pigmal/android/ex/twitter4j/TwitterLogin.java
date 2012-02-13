package com.pigmal.android.ex.twitter4j;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class TwitterLogin extends Activity {

	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.twitter_login);

		WebView webView = (WebView) findViewById(R.id.twitterLoginWeb);
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {

			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);

				if (url != null && url.startsWith(Const.CALLBACK_URL)) {
					String[] urlParameters = url.split("\\?")[1].split("&");

					String oauthToken = "";
					String oauthVerifier = "";

					if (urlParameters[0].startsWith("oauth_token")) {
						oauthToken = urlParameters[0].split("=")[1];
					} else if (urlParameters[1].startsWith("oauth_token")) {
						oauthToken = urlParameters[1].split("=")[1];
					}

					if (urlParameters[0].startsWith("oauth_verifier")) {
						oauthVerifier = urlParameters[0].split("=")[1];
					} else if (urlParameters[1].startsWith("oauth_verifier")) {
						oauthVerifier = urlParameters[1].split("=")[1];
					}

					Intent intent = getIntent();
					intent.putExtra(Const.IEXTRA_OAUTH_TOKEN, oauthToken);
					intent.putExtra(Const.IEXTRA_OAUTH_VERIFIER, oauthVerifier);

					setResult(Activity.RESULT_OK, intent);
					finish();
				}
			}
		});

		webView.loadUrl(this.getIntent().getExtras().getString("auth_url"));

	}
}
