package uk.co.jaymehta.csafeedback;

import uk.co.jaymehta.csafeedback.util.SystemUiHider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class SplashActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    
    private AccountManager mAccountManager;

    private Integer hours_between_renew_token = 3;

    private ContentResolver mResolver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        mResolver = getContentResolver();
        mAccountManager = AccountManager.get(this);
        new checkLoginStatus().execute();
    }

    private class checkLoginStatus extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... params) {

            Looper.prepare();
            Account[] arrayAccounts = mAccountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE);
            if (arrayAccounts.length==0) {
                return "1";
            }
            else if (arrayAccounts.length==1) {
                Account mAccount = arrayAccounts[0];
                AccountManagerFuture<Bundle> amf = mAccountManager.getAuthToken(mAccount, AccountConstants.AUTH_TOKEN_TYPE, null, new AccountLoginAct(), null, null);

                mResolver.setSyncAutomatically(mAccount, DatabaseConstants.PROVIDER_NAME, true);
                String authToken;

                try {
                    Bundle authTokenBundle = amf.getResult();
                    authToken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
                }
                catch (OperationCanceledException e) {
                    e.printStackTrace();
                    showMessage(e.getMessage());
                    return "0";
                }
                catch (IOException e) {
                    e.printStackTrace();
                    showMessage(e.getMessage());
                    return "0";
                }
                catch (AuthenticatorException e) {
                    e.printStackTrace();
                    showMessage(e.getMessage());
                    return "0";
                }

                String result;

                try {
                    String url_checkvalid = "http://jkm50.user.srcf.net/feedback/post/index.php";
                    ContentValues authtokenvalues = new ContentValues();
                    authtokenvalues.put("authtoken", authToken);
                    result = PostHelper.postRequest(url_checkvalid, authtokenvalues);

                    Log.d("Jay", result);

                    if (result.equals("invalid_user")) { return "0"; }

                    if (result.equals("expired_token")) {
                        Log.d("Jay", "Expired token");
                        mAccountManager.invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, authToken);
                        return "1";
                    }

                    if (result.equals("valid_user")) {
                        SharedPreferences prefs = getApplication().getSharedPreferences(getString(R.string.time_since_renew), Context.MODE_PRIVATE);
                        Date tokenRenewed = new Date(prefs.getLong("time", 0));

                        Log.d("Jay", tokenRenewed.toString());

                        if (tokenRenewed.before(new Date(System.currentTimeMillis()))) {
                            String url_renew_token = "http://jkm50.user.srcf.net/feedback/post/index.php/welcome/renew_token";
                            result = PostHelper.postRequest(url_renew_token, authtokenvalues);

                            mAccountManager.invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, authToken);
                            mAccountManager.setAuthToken(mAccount, AccountConstants.AUTH_TOKEN_TYPE, result);
                            authToken = result;

                            Date newDate = new Date(System.currentTimeMillis() + (hours_between_renew_token * 3600 * 1000));
                            Log.d("Jay", newDate.toString());
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putLong(getString(R.string.time_since_renew), newDate.getTime());
                            editor.apply();
                        }

                        return authToken;
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                    showMessage(e.getMessage());
                }

                return authToken;
            }
            else return "0";

        }

        protected void onPostExecute(String result) {
            if (result.equals("0")) {
                Intent intent = new Intent(getApplicationContext(), ErrorActivity.class);
                startActivity(intent);
                return;
            }
            if (result.equals("1")) {
                addNewAccount(AccountConstants.ACCOUNT_TYPE, AccountConstants.AUTH_TOKEN_TYPE);
                return;
            }
        }
    }

    private void addNewAccount(String accountType, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.addAccount(accountType, authTokenType, null, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bnd = future.getResult();
                    showMessage("Account was created");
                    Log.d("Jay", "AddNewAccount Bundle is " + bnd);

                    Account[] arrayAccounts2 = mAccountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE);

                    if (arrayAccounts2.length==1) {
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
                        mResolver.requestSync(arrayAccounts2[0], DatabaseConstants.PROVIDER_NAME, settingsBundle);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    showMessage(e.getMessage());
                }
            }
        }, null);
    }

    private void showMessage(final String msg) {
        if (TextUtils.isEmpty(msg))
            return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
