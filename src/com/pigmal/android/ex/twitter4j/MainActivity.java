package com.pigmal.android.ex.twitter4j;

import twitter4j.DirectMessage;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStreamListener;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	private Context context;

	private Button connectButton;
	private Button getTweetButton;
	private TextView tweetText;
	private ScrollView scrollView;

	private Twitter twitter;
	private RequestToken requestToken;
	private TwitterStream twitterStream;
	private boolean running = false;

	/**
	 * check if the account is authorized
	 * @return
	 */
	private boolean isConnected() {
		SharedPreferences pref = getSharedPreferences(Const.PREFERENCE_NAME, MODE_PRIVATE);
		return pref.getBoolean(Const.PREF_KEY_CONNECTED, false);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		context = this;

		scrollView = (ScrollView)findViewById(R.id.scrollView);
		tweetText =(TextView)findViewById(R.id.tweetText);
		getTweetButton = (Button)findViewById(R.id.getTweet);
		getTweetButton.setOnClickListener(this);
		connectButton = (Button)findViewById(R.id.twitterLogin);
		if (isConnected()) {
			connectButton.setText(R.string.label_disconnect);
			getTweetButton.setEnabled(true);
		} else {
			connectButton.setText(R.string.label_connect);
		}
		connectButton.setOnClickListener(this);		
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
				editor.putString(Const.PREF_KEY_SECRET, accessToken.getTokenSecret());
				editor.putBoolean(Const.PREF_KEY_CONNECTED, true);
				editor.commit();

				Toast.makeText(context, "authorized", Toast.LENGTH_SHORT).show();
				connectButton.setText(R.string.label_disconnect);
				getTweetButton.setEnabled(true);
			} catch (TwitterException e) {
				e.printStackTrace();
			}
		}
	}


	protected void onResume() {
		super.onResume();
		if (isConnected()) {
			SharedPreferences pref = getSharedPreferences(Const.PREFERENCE_NAME, MODE_PRIVATE);
			String oauthAccessToken = pref.getString(Const.PREF_KEY_TOKEN, "");
			String oAuthAccessTokenSecret = pref.getString(Const.PREF_KEY_SECRET, "");
			ConfigurationBuilder confbuilder = new ConfigurationBuilder();
			Configuration conf = confbuilder
								.setOAuthConsumerKey(Const.CONSUMER_KEY)
								.setOAuthConsumerSecret(Const.CONSUMER_SECRET)
								.setOAuthAccessToken(oauthAccessToken)
								.setOAuthAccessTokenSecret(oAuthAccessTokenSecret)
								.build();
			twitterStream = new TwitterStreamFactory(conf).getInstance();
		}
	}
	
	private Boolean connectTwitter() {
		ConfigurationBuilder confbuilder = new ConfigurationBuilder();
		Configuration conf = confbuilder
							.setOAuthConsumerKey(Const.CONSUMER_KEY)
							.setOAuthConsumerSecret(Const.CONSUMER_SECRET)
							.build();

		twitter = new TwitterFactory(conf).getInstance();
		twitter.setOAuthAccessToken(null);

		boolean ret = false;
		try {
			requestToken = twitter.getOAuthRequestToken(Const.CALLBACK_URL);

			Intent intent = new Intent(this, TwitterLogin.class);
			intent.putExtra(Const.IEXTRA_AUTH_URL, requestToken.getAuthorizationURL());

			this.startActivityForResult(intent, 0);
			ret = true;
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public void disconnectTwitter() {
		SharedPreferences pref = getSharedPreferences(Const.PREFERENCE_NAME, MODE_PRIVATE);

		SharedPreferences.Editor editor = pref.edit();
		editor.remove(Const.PREF_KEY_TOKEN);
		editor.remove(Const.PREF_KEY_SECRET);
		editor.remove(Const.PREF_KEY_CONNECTED);

		editor.commit();
	}

	private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
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
		protected Boolean doInBackground(Void... v) {
			return connectTwitter();
		}

		@Override
		protected void onProgressUpdate(Void... v) {
			//TODO show progress
		}

		@Override
		protected void onPostExecute(Boolean isConnected) {
			if (isConnected()) {
				connectButton.setText(R.string.label_disconnect);
			} else {
				Toast.makeText(context, "Error to authorize", Toast.LENGTH_LONG).show();
			}
			progressDialog.dismiss();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.twitterLogin:
			if (isConnected()) {
				disconnectTwitter();
				connectButton.setText(R.string.label_connect);
			} else {
				new ConnectTask().execute();
			}
			break;
		case R.id.getTweet:
			if (running) {
				stopStreamingTimeline();
				running = false;
				getTweetButton.setText("start streaming");
			} else {
				startStreamingTimeline();
				running = true;
				getTweetButton.setText("stop streaming");
			}
			break;
		}
	}
	
	private void stopStreamingTimeline() {
		twitterStream.shutdown();
	}

	public void startStreamingTimeline() {

        UserStreamListener listener = new UserStreamListener() {

			@Override
			public void onDeletionNotice(StatusDeletionNotice arg0) {
				System.out.println("deletionnotice");
			}

			@Override
			public void onScrubGeo(long arg0, long arg1) {
				System.out.println("scrubget");
			}

			@Override
			public void onStatus(Status status) {
				final String tweet = "@" + status.getUser().getScreenName() + " : " + status.getText() + "\n"; 
				System.out.println(tweet);
				tweetText.post(new Runnable() {
					@Override
					public void run() {
						tweetText.append(tweet);
						scrollView.fullScroll(View.FOCUS_DOWN);
					}
				});
			}

			@Override
			public void onTrackLimitationNotice(int arg0) {
				System.out.println("trackLimitation");
			}

			@Override
			public void onException(Exception arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onBlock(User arg0, User arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onDeletionNotice(long arg0, long arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onDirectMessage(DirectMessage arg0) {
				// TODO Auto-generated method stub				
			}

			@Override
			public void onFavorite(User arg0, User arg1, Status arg2) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onFollow(User arg0, User arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onFriendList(long[] arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onRetweet(User arg0, User arg1, Status arg2) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onUnblock(User arg0, User arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onUnfavorite(User arg0, User arg1, Status arg2) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onUserListCreation(User arg0, UserList arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onUserListDeletion(User arg0, UserList arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onUserListMemberAddition(User arg0, User arg1, UserList arg2) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onUserListMemberDeletion(User arg0, User arg1,  UserList arg2) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onUserListSubscription(User arg0, User arg1, UserList arg2) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onUserListUnsubscription(User arg0, User arg1, UserList arg2) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onUserListUpdate(User arg0, UserList arg1) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onUserProfileUpdate(User arg0) {
				// TODO Auto-generated method stub
			}
        };
        twitterStream.addListener(listener);
        twitterStream.user();
	}
}
