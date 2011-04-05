package com.itog_lab.android.sample.twitter4j;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
	private Twitter twitter = null;
	private RequestToken requestToken = null;

	private boolean isConnected;
	private Context context;

	private Button connectButton;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		context = this;

		SharedPreferences pref = getSharedPreferences(Const.PREFERENCE_NAME, MODE_PRIVATE);
		isConnected = pref.getBoolean(Const.PREF_KEY_CONNECTED, false);

		connectButton = (Button)findViewById(R.id.tweetLogin);
		if (isConnected) {
			connectButton.setText(R.string.label_disconnect);
		} else {
			connectButton.setText(R.string.label_connect);
		}
		connectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (isConnected) {
					disconnectTwitter();
					connectButton.setText(R.string.label_connect);
				} else {
					new ConnectTask().execute();
				}
			}
		});
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			super.onActivityResult(requestCode, resultCode, intent);

			AccessToken accessToken = null;
			try {
				accessToken = twitter.getOAuthAccessToken(requestToken, intent
						.getExtras().getString(Const.IEXTRA_OAUTH_VERIFIER));

				SharedPreferences pref = getSharedPreferences(Const.PREFERENCE_NAME, MODE_PRIVATE);

				SharedPreferences.Editor editor = pref.edit();
				editor.putString(Const.PREF_KEY_TOKEN, accessToken.getToken());
				editor.putString(Const.PREF_KEY_SECRET, accessToken
						.getTokenSecret());
				editor.putBoolean(Const.PREF_KEY_CONNECTED, true);
				editor.commit();

				Toast.makeText(context, "authorized", Toast.LENGTH_SHORT).show();
				connectButton.setText(R.string.label_disconnect);
			} catch (TwitterException e) {
				e.printStackTrace();
			}
		}
	}

	private void connectTwitter() {
		ConfigurationBuilder confbuilder = new ConfigurationBuilder();
		Configuration conf = confbuilder
							.setOAuthConsumerKey(Const.CONSUMER_KEY)
							.setOAuthConsumerSecret(Const.CONSUMER_SECRET)
							.build();

		twitter = new TwitterFactory(conf).getInstance();
		twitter.setOAuthAccessToken(null);

		try {
			requestToken = twitter.getOAuthRequestToken(Const.CALLBACK_URL);

			Intent intent = new Intent(this, TwitterLogin.class);
			intent.putExtra(Const.IEXTRA_AUTH_URL, requestToken.getAuthorizationURL());

			this.startActivityForResult(intent, 0);
		} catch (TwitterException e) {
			Toast.makeText(context, "Errror : " + e.getStatusCode(), Toast.LENGTH_LONG).show();
		}
	}

	public void disconnectTwitter() {
		SharedPreferences pref = getSharedPreferences(Const.PREFERENCE_NAME, MODE_PRIVATE);

		SharedPreferences.Editor editor = pref.edit();
		editor.remove(Const.PREF_KEY_TOKEN);
		editor.remove(Const.PREF_KEY_SECRET);
		editor.remove(Const.PREF_KEY_CONNECTED);

		editor.commit();
	}

	private class ConnectTask extends AsyncTask<Void, Void, Void> {
		private ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = new ProgressDialog(context);
			progressDialog.setMessage("doing...");
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					cancel(false);
				}
			});
			progressDialog.show();
		}

		@Override
		protected Void doInBackground(Void... v) {
			connectTwitter();
			return (Void)null;
		}

		@Override
		protected void onProgressUpdate(Void... v) {
			//TODO show progress
		}

		@Override
		protected void onPostExecute(Void v) {
			connectButton.setText(R.string.label_disconnect);
			progressDialog.dismiss();
		}
	}
}
