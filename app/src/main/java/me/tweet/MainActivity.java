package me.tweet;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class MainActivity extends AppCompatActivity {

    static String TWITTER_CONSUMER_KEY = "Lv89KWd4oS2kaItDu5lMc5srA";
    static String TWITTER_CONSUMER_SECRET = "otMY446NC947Za1hcbss7tvuuwZiWo13dBcEj7UJ7fJxqSi2QR";

    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";

    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";

    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";

    Button login, updateStatus, logout;
    EditText etUpdate;
    TextView tvUserName, tvUpdate;
    ProgressDialog pDialog;

    private static Twitter twitter;
    private static RequestToken requestToken;
    private static SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        CheckConnection conn = new CheckConnection(getApplicationContext());

        AlertDialog alert = new AlertDialog.Builder(this).create();
        alert.setTitle("Failed");
        alert.setMessage("Could not connect to the Internet");
        alert.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });


        if (!conn.isConnecting()){
            alert.show();
        }

        login = (Button) findViewById(R.id.btnLogin);
        updateStatus = (Button) findViewById(R.id.btnUpdateStatus);
        logout = (Button) findViewById(R.id.btnLogout);
        etUpdate = (EditText) findViewById(R.id.etUpdateStatus);
        tvUpdate = (TextView) findViewById(R.id.tvUpdate);
        tvUserName = (TextView) findViewById(R.id.tvUserName);

        prefs = getApplicationContext().getSharedPreferences("MyPref", 0);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logIn();
            }
        });

        updateStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String status = etUpdate.getText().toString();

                if (status.trim().length() > 0) {
                    new updateTwitterStatus().execute(status);
                }
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOut();
            }
        });

        if (!isTwitterLoggedInAlready()){
            Uri uri = getIntent().getData();
            if (uri !=null && uri.toString().startsWith(TWITTER_CALLBACK_URL)){
                String verifier = uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
                try {
                    AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                    editor.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
                    editor.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                    editor.apply();

                    login.setVisibility(View.GONE);

                    tvUpdate.setVisibility(View.VISIBLE);
                    etUpdate.setVisibility(View.VISIBLE);
                    updateStatus.setVisibility(View.VISIBLE);
                    logout.setVisibility(View.VISIBLE);

                    long userID = accessToken.getUserId();
                    User user = twitter.showUser(userID);
                    String username = user.getName();

                    tvUserName.setText(Html.fromHtml("<b>Hi " + username + "</b>"));
                } catch (Exception e) {
                    Log.e("Twitter Login Error", "> " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private boolean isTwitterLoggedInAlready() {
        return prefs.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    private void logIn() {
        if (!isTwitterLoggedInAlready()){
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
            builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
            Configuration configuration = builder.build();

            TwitterFactory factory = new TwitterFactory(configuration);
            twitter = factory.getInstance();

            try {
                requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
                this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
            } catch (TwitterException e){
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(),
                    "Already Logged into twitter", Toast.LENGTH_LONG).show();
        }
    }


    private void logOut() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREF_KEY_OAUTH_TOKEN);
        editor.remove(PREF_KEY_OAUTH_SECRET);
        editor.remove(PREF_KEY_TWITTER_LOGIN);
        editor.apply();

        logout.setVisibility(View.GONE);
        updateStatus.setVisibility(View.GONE);
        etUpdate.setVisibility(View.GONE);
        tvUpdate.setText("");
        tvUserName.setVisibility(View.GONE);

        login.setVisibility(View.VISIBLE);

    }

    class updateTwitterStatus extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Updating twitter status...");
            pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String status = params[0];
            try {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
                builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);

                String access_token = prefs.getString(PREF_KEY_OAUTH_TOKEN, "");
                String access_token_secret = prefs.getString(PREF_KEY_OAUTH_SECRET, "");

                AccessToken accessToken = new AccessToken(access_token, access_token_secret);
                Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

                twitter4j.Status response = twitter.updateStatus(status);
            } catch (TwitterException e){
                Log.d("Error Updating", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            pDialog.dismiss();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Tweet Sent", Toast.LENGTH_SHORT).show();
                    etUpdate.setText("");
                }
            });
        }
    }
}


