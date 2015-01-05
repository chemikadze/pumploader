package com.github.chemikadze.pumploader;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.jstrava.authenticator.AuthResponse;
import org.jstrava.authenticator.StravaAuthenticator;

public class NewAccountActivity extends AccountAuthenticatorActivity {

    public static final String LOG_TAG = NewAccountActivity.class.getSimpleName();
    public static final int RESULT_FAILED = RESULT_FIRST_USER;

    private StravaAuthenticator authenticator;

    public static final String KEY_EXTRA_TOKEN_TYPE = "x-token-type";
    public static final String REDIRECT_URL = "http://localhost";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        authenticator =
                new StravaAuthenticator(
                        getResources().getInteger(R.integer.auth_client_id),
                        getString(R.string.auth_redirect_url),
                        getString(R.string.auth_client_secret));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_account_activity);
        WebView wv = (WebView)findViewById(R.id.login_web_view);
        wv.setWebViewClient(new StravaAuthClient());
        wv.loadUrl(authenticator.getRequestAccessUrl("force", false, false, null) + "&scope=write");
    }

    class StravaAuthClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith(REDIRECT_URL)) {
                try {
                    Uri parsed = Uri.parse(url);
                    String authcode = parsed.getQueryParameter("code");
                    onCodeReceived(authcode);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to parse URL", e);
                }
                return true;
            } else {
                return false;
            }
        }

        private void onCodeReceived(final String authcode) {
            new AsyncTask<Void, Void, Intent>() {
                @Override
                protected Intent doInBackground(Void... params) {
                    final Intent res = new Intent();
                    try {
                        AuthResponse parsedResponse = authenticator.getToken(authcode);
                        if (parsedResponse != null && parsedResponse.getAthlete() != null) {
                            String name = parsedResponse.getAthlete().getFirstname();
                            if (TextUtils.isEmpty(name)) {
                                name = getApplicationContext().getString(R.string.unnamed_athlete) +
                                        String.valueOf(parsedResponse.getAthlete().getId());
                            };

                            res.putExtra(AccountManager.KEY_ACCOUNT_NAME, name);
                            res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
                            res.putExtra(AccountManager.KEY_AUTHTOKEN, parsedResponse.getAccess_token());
                        } else {
                            // TODO: correctly handle error from strava!
                            res.putExtra(AccountManager.KEY_ERROR_MESSAGE, getString(R.string.auth_failed));
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                        res.putExtra(AccountManager.KEY_ERROR_MESSAGE, R.string.login_unexpected_error);
                    }
                    return res;
                }

                @Override
                protected void onPostExecute(Intent intent) {
                    String errorMsg = intent.getStringExtra(AccountManager.KEY_ERROR_MESSAGE);
                    if (errorMsg != null) {
                        setResult(RESULT_FAILED, intent);
                        finish();
                    } else {
                        finishLogin(intent);
                    }
                }
            }.execute();
        }
    }

    public void finishLogin(Intent intent) {
        final AccountManager am = AccountManager.get(this);
        final Bundle result = new Bundle();
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
        Account account = new Account(accountName, accountType);
        String token = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        result.putString(AccountManager.KEY_AUTHTOKEN, token);
        if (am.addAccountExplicitly(account, null, new Bundle(result))) {
            am.setAuthToken(account, account.type, token);

        } else {
            result.clear();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "already exists");
        }
        setAccountAuthenticatorResult(result);
        setResult(RESULT_OK);
        finish();
    }
}
