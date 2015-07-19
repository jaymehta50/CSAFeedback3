package uk.co.jaymehta.csafeedback;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;


public class FeedbackActivity extends Activity implements FeedbackActivityFragment.OnEventSelectedListener {
    private static final String FRAGMENT_LIST = "listFragment";
    private static final String FRAGMENT_PAGE = "pageFragment";
    private String mComment;

    private static final IntentFilter syncIntentFilter = new IntentFilter(DatabaseConstants.SYNC_FINISH);

    private BroadcastReceiver syncBroadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            //Switch to the main app page
            Log.d("Jay","done frag reload");
            Fragment g = getFragmentManager().findFragmentByTag(FRAGMENT_LIST);
            Fragment f = getFragmentManager().findFragmentById(R.id.container);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, FeedbackActivityFragment.newInstance(), FRAGMENT_LIST)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            unregisterReceiver(syncBroadcastReceiver);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, FeedbackActivityFragment.newInstance(), FRAGMENT_LIST)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }

        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);

        if(getIntent().hasExtra(NotificationLocal.EVENT_ID_TAG)) {
            Long eventid = getIntent().getLongExtra(NotificationLocal.EVENT_ID_TAG, 1);
            onEventSelected(eventid);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_feedback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.sync_now) {
            AccountManager mAccountManager = AccountManager.get(this);
            Account[] arrayAccounts = mAccountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE);

            //Double check - there should still only be one CSA account
            if (arrayAccounts.length==1) {
                registerReceiver(syncBroadcastReceiver, syncIntentFilter);
                Toast.makeText(getBaseContext(), "Getting your data\nPlease wait...", Toast.LENGTH_LONG).show();
                // Force syncadapter to run now so that user has all the information downloaded for first use
                Bundle settingsBundle = new Bundle();
                settingsBundle.putBoolean(
                        ContentResolver.SYNC_EXTRAS_MANUAL, true);
                settingsBundle.putBoolean(
                        ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

                getContentResolver().requestSync(arrayAccounts[0], DatabaseConstants.PROVIDER_NAME, settingsBundle);
                return true;
            }
            else {
                //Must have been an error (more/less than one CSA account) so show error page
                Intent intent = new Intent(getApplicationContext(), ErrorActivity.class);
                startActivity(intent);
                return true;
            }
        }
        if (id == R.id.action_settings) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new SettingsFragment(), FRAGMENT_PAGE)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();
        Log.d("Jay", "Back stack count = "+count);
        if(count > 1) {
            super.onBackPressed();
        }
        else {
            this.moveTaskToBack(true);
        }
    }

    public void onEventSelected(long id) {
        getFragmentManager().beginTransaction()
                .replace(R.id.container, FeedbackActivityFragment2.newInstance(id), FRAGMENT_PAGE)
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d("Jay", "Setting = " + sharedPref.getBoolean(this.getString(R.string.pref_event_notify), true));
    }

    public void saveDataButtonClicked(Boolean commentAdd, String comment) {
        mComment = comment;
        new saveData().execute(commentAdd);
    }

    private class saveData extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            Log.d("Jay", params[0].toString());
            if(params[0]) {
                Log.d("Jay", "String from class: "+FeedbackActivityFragment2.comment);
                Log.d("Jay", "String from mComment: "+mComment);

                ContentValues toupdate = new ContentValues();
                toupdate.put(DatabaseConstants.fd_feedback.COLUMN_NAME_COMMENT, mComment);
                toupdate.put(DatabaseConstants.fd_feedback.COLUMN_NAME_SYNC, 0);

                Integer answer = getApplicationContext().getContentResolver().update(
                        Uri.withAppendedPath(DatabaseConstants.CONTENT_URI, "feedback"),
                        toupdate,
                        DatabaseConstants.fd_feedback.COLUMN_NAME_EVENTID + "=?",
                        new String[]{FeedbackActivityFragment2.selection.toString()}
                );

                Log.d("Jay", "Tried updating comment, answer=" + answer);
                Log.d("Jay", toupdate.toString());
            }
            else {
                ContentValues toinsert = new ContentValues();
                toinsert.put(DatabaseConstants.fd_feedback.COLUMN_NAME_EVENTID, FeedbackActivityFragment2.selection);
                toinsert.put(DatabaseConstants.fd_feedback.COLUMN_NAME_SCORE, FeedbackActivityFragment2.score);
                toinsert.put(DatabaseConstants.fd_feedback.COLUMN_NAME_TIMESTAMP, Math.round(System.currentTimeMillis() / 1000));
                toinsert.put(DatabaseConstants.fd_feedback.COLUMN_NAME_NOTIFY_RESP, FeedbackActivityFragment2.cbresp);
                toinsert.put(DatabaseConstants.fd_feedback.COLUMN_NAME_SYNC, 0);
                getApplicationContext().getContentResolver().insert(
                        Uri.withAppendedPath(DatabaseConstants.CONTENT_URI, "feedback"),
                        toinsert
                );

                Log.d("Jay", toinsert.toString());
            }

            //Get an updated list of accounts, which should include the recently created one
            Account[] arrayAccounts = AccountManager.get(getApplicationContext()).getAccountsByType(AccountConstants.ACCOUNT_TYPE);
            //If more/less than one account return, quit because thats weird...
            if(arrayAccounts.length!=1) return null;

            // Tell syncadapter to run so info is sent to server, but not expedited run
            Bundle settingsBundle = new Bundle();
            settingsBundle.putBoolean(
                    ContentResolver.SYNC_EXTRAS_MANUAL, true);
            settingsBundle.putBoolean(
                    ContentResolver.SYNC_EXTRAS_EXPEDITED, false);

            getContentResolver().requestSync(arrayAccounts[0], DatabaseConstants.PROVIDER_NAME, settingsBundle);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Fragment f = getFragmentManager().findFragmentByTag(FRAGMENT_PAGE);
            getFragmentManager().beginTransaction()
                    .detach(f)
                    .attach(f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
    }
}
