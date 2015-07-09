package uk.co.jaymehta.csafeedback;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import uk.co.jaymehta.csafeedback.util.SystemUiHider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.io.IOException;


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

    private ContentResolver mResolver;

    private static final IntentFilter syncIntentFilter = new IntentFilter(DatabaseConstants.SYNC_FINISH);

    private BroadcastReceiver syncBroadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            //Switch to the main app page
            Intent i = new Intent(context, FeedbackActivity.class);
            startActivity(i);
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

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

        //NO UI ELEMENTS TO INTERACT WITH
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        //Prep account and resolver variables
        mResolver = getContentResolver();
        mAccountManager = AccountManager.get(this);
        registerReceiver(syncBroadcastReceiver, syncIntentFilter);

        //Check for a valid account in an asynctask
        new checkLoginStatus().execute();
    }

    private class checkLoginStatus extends AsyncTask<Void, Void, Integer> {

        protected Integer doInBackground(Void... params) {

            Looper.prepare();
            //Search for CSA Accounts
            Account[] arrayAccounts = mAccountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE);

            if (arrayAccounts.length==0) {
                //No CSA account found - send the user to create an account
                return 1;
            }
            else if (arrayAccounts.length==1) {
                //One CSA account found - get its auth token
                Account mAccount = arrayAccounts[0];
                Crashlytics.getInstance().core.setUserIdentifier(mAccount.toString());
                AccountManagerFuture<Bundle> amf = mAccountManager.getAuthToken(mAccount, AccountConstants.AUTH_TOKEN_TYPE, null, new AccountLoginAct(), null, null);

                //Set the sync adapter to run automatically
                mResolver.setSyncAutomatically(mAccount, DatabaseConstants.PROVIDER_NAME, true);

                try {
                    Bundle authTokenBundle = amf.getResult();
                    //If authtoken has expired, prompt user to re-login (we don't store users' password so can't do this ourselves)
                    if (authTokenBundle.containsKey(AccountManager.KEY_INTENT)) {
                        return 1;
                    }
                }
                catch (OperationCanceledException e) {
                    Crashlytics.getInstance().core.logException(e);
                    e.printStackTrace();
                    showMessage(e.getMessage());
                    return 0;
                }
                catch (IOException e) {
                    Crashlytics.getInstance().core.logException(e);
                    e.printStackTrace();
                    showMessage(e.getMessage());
                    return 0;
                }
                catch (AuthenticatorException e) {
                    Crashlytics.getInstance().core.logException(e);
                    e.printStackTrace();
                    showMessage(e.getMessage());
                    return 0;
                }

                //Store a value that tells the syncadapter to renew this users authtoken the next time it runs
                SharedPreferences prefs = getSharedPreferences(getString(R.string.prefs_name), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(getString(R.string.run_renewal_bool), true);
                editor.apply();

                return 2;
            }
            else return 0;

        }

        protected void onPostExecute(Integer result) {
            if (result==1) {
                //Prompt from above that we need to add a new account or user's authtoken is invalid
                addNewAccount(AccountConstants.ACCOUNT_TYPE, AccountConstants.AUTH_TOKEN_TYPE);
                return;
            }
            if (result==2) {
                //Everything was fine, start app
                Intent intent = new Intent(getApplicationContext(), FeedbackActivity.class);
                startActivity(intent);
                return;
            }
            //Anything else - error page
            Intent intent = new Intent(getApplicationContext(), ErrorActivity.class);
            startActivity(intent);
        }
    }

    private void addNewAccount(String accountType, String authTokenType) {
        //Add new account
        final AccountManagerFuture<Bundle> future = mAccountManager.addAccount(accountType, authTokenType, null, null, this, new AccountManagerCallback<Bundle>() {
            //Function to run afterwards
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bnd = future.getResult();
                    showMessage("Account was created");
                    Log.d("Jay", "AddNewAccount Bundle is " + bnd);

                    //Get an updated list of accounts, which should include the recently created one
                    Account[] arrayAccounts2 = mAccountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE);

                    //Double check - there should still only be one CSA account
                    if (arrayAccounts2.length==1) {
                        Crashlytics.getInstance().core.setUserIdentifier(arrayAccounts2[0].toString());
                        // Force syncadapter to run now so that user has all the information downloaded for first use
                        Bundle settingsBundle = new Bundle();
                        settingsBundle.putBoolean(
                                ContentResolver.SYNC_EXTRAS_MANUAL, true);
                        settingsBundle.putBoolean(
                                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

                        mResolver.requestSync(arrayAccounts2[0], DatabaseConstants.PROVIDER_NAME, settingsBundle);
                    }
                    else {
                        //Must have been an error (more/less than one CSA account) so show error page
                        Intent intent = new Intent(getApplicationContext(), ErrorActivity.class);
                        startActivity(intent);
                    }

                } catch (Exception e) {
                    Crashlytics.getInstance().core.logException(e);
                    e.printStackTrace();
                    showMessage(e.getMessage());
                }
            }
        }, null);
    }

    //Quick function to simplify android toasts
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

    @Override
    protected void onResume() {
        super.onResume();
        // register for sync
        registerReceiver(syncBroadcastReceiver, syncIntentFilter);
        // do your resuming magic
    }

    @Override
    protected void onPause() {
        unregisterReceiver(syncBroadcastReceiver);
        super.onPause();
    };
}
