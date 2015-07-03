package uk.co.jaymehta.csafeedback;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by jkm50 on 02/07/2015.
 */
public class CSAAuthenticator extends AbstractAccountAuthenticator {
    private final String TAG = "CSAAuthenticator";
    private Context mContext;

    public CSAAuthenticator(Context context) {
        super(context);

        this.mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Log.d("Jay", TAG + "> Add Account");
        Log.d("Jay", accountType);

        final Intent intent = new Intent(mContext, AccountLoginAct.class);
            intent.putExtra(AccountLoginAct.ARG_ACCOUNT_TYPE, accountType);
            intent.putExtra(AccountLoginAct.ARG_AUTH_TYPE, authTokenType);
            intent.putExtra(AccountLoginAct.ARG_IS_ADDING_NEW_ACCOUNT, true);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d("Jay", TAG + "> getAuthToken");

        if (!authTokenType.equals(AccountConstants.AUTH_TOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);

        String authToken = am.peekAuthToken(account, authTokenType);

        Log.d("Jay", TAG + "> peekAuthToken returned - " + authToken);

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AccountLoginAct.
        final Intent intent = new Intent(mContext, AccountLoginAct.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AccountLoginAct.ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(AccountLoginAct.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(AccountLoginAct.ARG_ACCOUNT_NAME, account.name);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (AccountConstants.AUTH_TOKEN_TYPE.equals(authTokenType))
            return AccountConstants.AUTH_TOKEN_TYPE_LABEL;
        else
            return authTokenType + " (Label)";
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }


}