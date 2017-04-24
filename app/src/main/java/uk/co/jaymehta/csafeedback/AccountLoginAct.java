package uk.co.jaymehta.csafeedback;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.iid.InstanceID;


public class AccountLoginAct extends AccountAuthenticatorActivity {

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";

    public final static String PARAM_USER_PASS = "USER_PASS";

    private final int REQ_SIGNUP = 1;

    private final String TAG = this.getClass().getSimpleName();

    private AccountManager mAccountManager;
    private String mAuthTokenType;
    private String mAccountType;

    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_login);
        mAccountManager = AccountManager.get(getBaseContext());

        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
        mAuthTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
        if (mAuthTokenType == null)
            mAuthTokenType = AccountConstants.AUTH_TOKEN_TYPE;
        mAccountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

    public void launchRavenLogin(View v) {
        //Launch Raven login in webview
        webview = new WebView(this);
        webview.clearCache(true);
        try {
            setContentView(webview);

            webview.setWebViewClient(new MyWebViewClient());

            webview.getSettings().setJavaScriptEnabled(true);
            webview.addJavascriptInterface(new JSInterfaceClass(), "Android");

            webview.loadUrl(AccountConstants.BASE_URL + "login/index.php/csafeedback/get/"+AccountConstants.CLIENT_SECRET+"/"+ InstanceID.getInstance(this).getId());
        } catch (Exception e) {
            e.printStackTrace();
            setContentView(R.layout.activity_account_login);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), "Please make sure you are connected to the internet", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("Jay", Uri.parse(url).getHost());
            if (Uri.parse(url).getHost().equals(AccountConstants.BASE_HOST) || Uri.parse(url).getHost().equals(AccountConstants.BASE_RAVEN)) {
                // This is my web site, so do not override; let my WebView load the page
                return false;
            }
            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.d("Jay", "WebView error code: "+errorCode);
            Log.d("Jay", "WebView error desc.: "+description);
            setContentView(R.layout.activity_account_login);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), "Please make sure you are connected to the internet", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private class JSInterfaceClass {

        /** Instantiate the interface and set the context */
        JSInterfaceClass() {}

        @JavascriptInterface
        public void returnAuthToken(String token, String username) {
            storeAndCloseWebview(token, username);
        }
    }

    public void storeAndCloseWebview(String token, String username) {
        Log.d("Jay", TAG + "> storeAndCloseWebview");
        Log.d("Jay", token);
        Log.d("Jay", username);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webview.loadUrl("about:blank");
                webview.clearCache(true);
                webview.destroy();
            }
        });

        Bundle data = new Bundle();

        data.putString(AccountManager.KEY_ACCOUNT_NAME, username);
        data.putString(AccountManager.KEY_ACCOUNT_TYPE, getIntent().getStringExtra(ARG_ACCOUNT_TYPE));
        data.putString(AccountManager.KEY_AUTHTOKEN, token);


        final Intent res = new Intent();
        res.putExtras(data);

        finishLogin(res);
    }

    private void finishLogin(Intent intent) {
        Log.d("Jay", TAG + "> finishLogin");

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        Log.d("Jay", accountName);
        Log.d("Jay", mAccountType);
        final Account account = new Account(accountName, mAccountType);

        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        /*
         * Request the sync for the default account, authority, and
         * manual sync settings
         */
        ContentResolver.requestSync(account, DatabaseConstants.PROVIDER_NAME, settingsBundle);

        Log.d("Jay", TAG + "> finishLogin > addAccountExplicitly");
        String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String authtokenType = mAuthTokenType;

        // Creating the account on the device and setting the auth token we got
        // (Not setting the auth token will cause another call to the server to authenticate the user)
        mAccountManager.addAccountExplicitly(account, null, null);
        mAccountManager.setAuthToken(account, authtokenType, authtoken);

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }
}
