package com.github.chemikadze.pumploader;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

public class PumpLoaderAccountAuthenticator extends AbstractAccountAuthenticator {

    private Context context = null;

    public PumpLoaderAccountAuthenticator(Context newContext) {
        super(newContext);
        context = newContext;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String s) {
        // TODO stub
        return new Bundle();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        final Intent intent = new Intent(context, NewAccountActivity.class);
        intent.putExtra(NewAccountActivity.KEY_EXTRA_TOKEN_TYPE, accountType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle keyBundle = new Bundle();
        if (options != null) {
            keyBundle.putAll(options);
        }
        keyBundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return keyBundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle options) throws NetworkErrorException {
        final Bundle keyBundle = new Bundle();
        // TODO stub
        if (options != null) {
            keyBundle.putAll(options);
        }
        keyBundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return keyBundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        final Bundle result = new Bundle();
        final AccountManager am = AccountManager.get(context.getApplicationContext());
        String authToken = am.peekAuthToken(account, authTokenType);
        if (!TextUtils.isEmpty(authToken)) {
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
        } else {
            final Intent intent = new Intent(context, NewAccountActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            intent.putExtra(NewAccountActivity.KEY_EXTRA_TOKEN_TYPE, authTokenType);
            final Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        }
        return result;
    }

    @Override
    public String getAuthTokenLabel(String s) {
        // TODO stub
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) throws NetworkErrorException {
        // TODO stub
        final Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        return result;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] strings) throws NetworkErrorException {
        // TODO stub
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }
}
