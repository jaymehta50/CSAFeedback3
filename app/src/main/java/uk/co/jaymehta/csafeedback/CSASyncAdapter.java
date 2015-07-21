package uk.co.jaymehta.csafeedback;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import io.fabric.sdk.android.Fabric;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class CSASyncAdapter extends AbstractThreadedSyncAdapter {
    // Class variables
    private ContentResolver mContentResolver;
    private Context mContext;
    private AccountManager mAccountManager;

    /**
     * Set up the sync adapter
     */
    public CSASyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mAccountManager = AccountManager.get(mContext);
        Fabric.with(mContext, new Crashlytics());
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public CSASyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mAccountManager = AccountManager.get(mContext);
    }

    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {

        SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);

        //Get the authtoken
        String mAuthToken;
        try {
            mAuthToken = mAccountManager.blockingGetAuthToken(account, AccountConstants.AUTH_TOKEN_TYPE, true);
            Log.d("Jay", "SyncAdapter > " + mAuthToken);

            //Check that authtoken has not already been invalidated e.g. by a previous run of this adapter, if yes then quit
            if(TextUtils.isEmpty(mAuthToken)) { return; }
        }
        catch (OperationCanceledException e) {
            Crashlytics.getInstance().core.logException(e);
            e.printStackTrace();
            Log.d("Jay", "SyncAdapter > " + e.getMessage());
            return;
        }
        catch (IOException e) {
            Crashlytics.getInstance().core.logException(e);
            e.printStackTrace();
            Log.d("Jay", "SyncAdapter > " + e.getMessage());
            return;
        }
        catch (AuthenticatorException e) {
            Crashlytics.getInstance().core.logException(e);
            e.printStackTrace();
            Log.d("Jay", "SyncAdapter > " + e.getMessage());
            return;
        }

        //Is this authtoken for a valid user?
        String url_checkvalid = AccountConstants.BASE_URL + "post/index.php";
        ContentValues authtokenvalues = new ContentValues();
        authtokenvalues.put("authtoken", mAuthToken);
        String result;
        try {
            result = PostHelper.postRequest(url_checkvalid, authtokenvalues);
        }
        catch (IOException e) {
            Crashlytics.getInstance().core.logException(e);
            e.printStackTrace();
            Log.d("Jay", "SyncAdapter > " + e.getMessage());
            return;
        }

        //Result shows that this user should not have access to this system... How odd... Remove their account, maybe a fresh login will help
        if (result.equals("invalid_user")) {
            Log.d("Jay", "SyncAdapter > " + "Invalid user");
            mAccountManager.invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, mAuthToken);
            mAccountManager.removeAccountExplicitly(account);
            return;
        }

        //Result shows that user's token has expired (probably hasn't logged in for 3 months), invalidate token and user can re-logon on next app usage
        if (result.equals("expired_token")) {
            Log.d("Jay", "SyncAdapter > " + "Expired token");
            mAccountManager.invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, mAuthToken);
            return;
        }

        //Server gives any other response than this is a valid user - quit
        if (!result.equals("valid_user")) {
            return;
        }


        //Anything after this line - we assume we have received a valid user response from server

        //Has the syncadapter been told to renew the auth token?
        if(prefs.getBoolean(mContext.getString(R.string.run_renewal_bool), false)) {
            //If yes - renew auth token
            Long tokenRenewed = prefs.getLong(mContext.getString(R.string.time_since_renew), 0);
            Log.d("Jay", "SyncAdapter > " + new Date(tokenRenewed).toString());

            //Has it been more than the number of hours specified in AccountConstants since the last authtoken renewal?
            if (tokenRenewed <= System.currentTimeMillis()) {
                //Yes - get and store new authtoken and invalidate prior one
                String url_renew_token = AccountConstants.BASE_URL + "post/index.php/welcome/renew_token";
                Log.d("Jay", "SyncAdapter > " + "Renewing token");
                try {
                    result = PostHelper.postRequest(url_renew_token, authtokenvalues);
                    Log.d("Jay", "SyncAdapter > " + result);
                } catch (IOException e) {
                    Crashlytics.getInstance().core.logException(e);
                    e.printStackTrace();
                    Log.d("Jay", "SyncAdapter > " + e.getMessage());
                    return;
                }

                mAccountManager.invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, mAuthToken);
                mAccountManager.setAuthToken(account, AccountConstants.AUTH_TOKEN_TYPE, result);
                mAuthToken = result;

                //Update datetime after which next renewal can take place
                Long newDate = System.currentTimeMillis() + (AccountConstants.HOURS_BETWEEN_RENEW_TOKEN * 3600 * 1000);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(mContext.getString(R.string.time_since_renew), newDate);
                editor.apply();
            }
        }


        //Get any unsynced items ready to sync up to server
        Log.d("Jay", "SyncAdapter > " + "Get any unsynced items ready to sync up to server");
        Cursor c = mContentResolver.query(
                Uri.parse(DatabaseConstants.URL + "feedback"),
                new String[] {
                        DatabaseConstants.fd_feedback.COLUMN_NAME_EVENTID,
                        DatabaseConstants.fd_feedback.COLUMN_NAME_SCORE,
                        DatabaseConstants.fd_feedback.COLUMN_NAME_COMMENT,
                        DatabaseConstants.fd_feedback.COLUMN_NAME_TIMESTAMP,
                        DatabaseConstants.fd_feedback.COLUMN_NAME_NOTIFY_RESP
                },
                DatabaseConstants.fd_feedback.COLUMN_NAME_SYNC + "=?",
                new String[] {"0"},
                null
        );

        //If there are any un-synced items, sync them up to server now
        Log.d("Jay", "SyncAdapter > " + "If there are any un-synced items, sync them up to server now");
        if (c.getCount() > 1) {
            Log.d("Jay", "SyncAdapter > " + "Syncing items up");
            JSONArray data = cur2Json(c);
            c.close();

            Cursor d = mContentResolver.query(
                    Uri.parse(DatabaseConstants.URL + "feedback"),
                    new String[] { BaseColumns._ID },
                    DatabaseConstants.fd_feedback.COLUMN_NAME_SYNC + "=?",
                    new String[] {"0"},
                    null
            );

            String url_sync_up = AccountConstants.BASE_URL + "post/index.php/welcome/sync_up";
            authtokenvalues = new ContentValues();
            authtokenvalues.put("authtoken", mAuthToken);
            authtokenvalues.put("fd_data", data.toString());
            try {
                String postresponse = PostHelper.postRequest(url_sync_up, authtokenvalues);
                setItemsAsSynced(d);
                d.close();
            }
            catch (IOException e) {
                Crashlytics.getInstance().core.logException(e);
                e.printStackTrace();
                Log.d("Jay", "SyncAdapter > " + e.getMessage());
            }
        }


        //Sync down
        //Get data to store on device
        Log.d("Jay", "SyncAdapter > " + "Sync down");
        String url_sync_up = AccountConstants.BASE_URL + "post/index.php/welcome/sync_down";
        authtokenvalues = new ContentValues();
        authtokenvalues.put("authtoken", mAuthToken);
        try {
            result = PostHelper.postRequest(url_sync_up, authtokenvalues);
        }
        catch (IOException e) {
            Crashlytics.getInstance().core.logException(e);
            e.printStackTrace();
            return;
        }

        //Convert string retrieved into JSON array
        JSONArray obj;
        JSONArray fd_obj;
        try {
            JSONArray result_json_array = new JSONArray(result);
            obj = result_json_array.getJSONArray(0);
            fd_obj = result_json_array.getJSONArray(1);
        } catch (Throwable t) {
            Crashlytics.getInstance().core.logException(t);
            t.printStackTrace();
            Log.d("Jay", "SyncAdapter > " + "Could not parse malformed JSON: \"" + result + "\"");
            return;
        }

        //Convert JSONArray into ContentValues, delete existing entries, and insert each newly downloaded one
        for (int i = 0, size = obj.length(); i < size; i++)
        {
            ContentValues toinsert = new ContentValues();
            JSONObject objectInArray;
            try {
                objectInArray = obj.getJSONObject(i);
            }
            catch (JSONException e) {
                Crashlytics.getInstance().core.logException(e);
                e.printStackTrace();
                return;
            }

            JSONArray elementNames = objectInArray.names();
            for (int j = 0, size2 = elementNames.length(); j < size2; j++)
            {
                try {
                    //Convert JSONArray to ContentValues
                    toinsert.put(elementNames.getString(j), objectInArray.getString(elementNames.getString(j)));
                }
                catch (JSONException e) {
                    Crashlytics.getInstance().core.logException(e);
                    e.printStackTrace();
                    Log.d("Jay", "SyncAdapter > " + e.getMessage());
                    return;
                }
            }

            if((toinsert.getAsLong("endtime")*1000) >= System.currentTimeMillis()) {
                //Schedule notifications for each event
                scheduleNotification(toinsert.getAsLong("endtime"), toinsert);
            }

            Cursor olddata = mContentResolver.query(
                    Uri.parse(DatabaseConstants.URL + "events"),
                    new String [] {
                            DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_USER,
                            DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_NAME
                    },
                    BaseColumns._ID + "=?",
                    new String[] {toinsert.getAsString(BaseColumns._ID)},
                    null
            );

            if(olddata.moveToFirst()) {
                mContentResolver.delete(
                        Uri.parse(DatabaseConstants.URL + "events"),
                        BaseColumns._ID + "=?",
                        new String[]{toinsert.getAsString(BaseColumns._ID)}
                );

                if(sharedPref.getBoolean(mContext.getString(R.string.pref_resp_notify), true)) {
                    String resp_user = olddata.getString(olddata.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_USER));
                    if (TextUtils.isEmpty(resp_user) || resp_user.equals("") || resp_user.equals("null") || resp_user.equals("Null")) {
                        String new_resp_user = toinsert.getAsString(DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_USER);
                        if (!TextUtils.isEmpty(new_resp_user) && !new_resp_user.equals("") && !new_resp_user.equals("null") && !new_resp_user.equals("Null")) {
                            NotificationLocal.doNotification(
                                    mContext,
                                    "New response to feedback",
                                    toinsert.getAsString(DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_NAME) + " responded to the feedback on the " + toinsert.getAsString(DatabaseConstants.fd_events.COLUMN_NAME_NAME),
                                    toinsert.getAsInteger(BaseColumns._ID),
                                    toinsert.getAsLong(BaseColumns._ID));
                        }
                    }
                }
            }
            olddata.close();

            //Insert current row
            mContentResolver.insert(
                    Uri.parse(DatabaseConstants.URL + "events"),
                    toinsert
            );
            toinsert.clear();
        }

        //Convert JSONArray into ContentValues, delete existing entries, and insert each newly downloaded one
        Boolean first = true;
        for (int i = 0, size = fd_obj.length(); i < size; i++)
        {
            ContentValues toinsert = new ContentValues();
            JSONObject objectInArray;
            try {
                objectInArray = fd_obj.getJSONObject(i);
            }
            catch (JSONException e) {
                Crashlytics.getInstance().core.logException(e);
                e.printStackTrace();
                return;
            }

            JSONArray elementNames = objectInArray.names();
            for (int j = 0, size2 = elementNames.length(); j < size2; j++)
            {
                try {
                    //Convert JSONArray to ContentValues
                    toinsert.put(elementNames.getString(j), objectInArray.getString(elementNames.getString(j)));
                }
                catch (JSONException e) {
                    Crashlytics.getInstance().core.logException(e);
                    e.printStackTrace();
                    Log.d("Jay", "SyncAdapter > " + e.getMessage());
                    return;
                }
            }

            //If first time this has run, delete existing entries
            if (first) {
                Log.d("Jay", "SyncAdapter > " + "Delete existing entries");
                mContentResolver.delete(
                        Uri.parse(DatabaseConstants.URL + "feedback"),
                        DatabaseConstants.fd_feedback.COLUMN_NAME_SYNC + "=?",
                        new String[] { "1" }
                );
            }
            first = false;

            //Insert current row
            mContentResolver.insert(
                    Uri.parse(DatabaseConstants.URL + "feedback"),
                    toinsert
            );
            toinsert.clear();
        }

        mContext.sendBroadcast(new Intent(DatabaseConstants.SYNC_FINISH));
    }

    //Converts a cursor into a JSONArray
    public JSONArray cur2Json(Cursor cursor) {

        JSONArray resultSet = new JSONArray();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();
            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        rowObject.put(cursor.getColumnName(i),
                                cursor.getString(i));
                    } catch (Exception e) {
                        Crashlytics.getInstance().core.logException(e);
                        Log.d("Jay", "SyncAdapter > " + e.getMessage());
                    }
                }
            }
            resultSet.put(rowObject);
            cursor.moveToNext();
        }

        return resultSet;
    }

    public void setItemsAsSynced(Cursor cursor) {
        cursor.moveToFirst();
        ContentValues values = new ContentValues();
        values.put(DatabaseConstants.fd_feedback.COLUMN_NAME_SYNC, 1);
        while (!cursor.isAfterLast()) {
            mContentResolver.update(
                    Uri.withAppendedPath(DatabaseConstants.CONTENT_URI, "feedback/" + cursor.getString(cursor.getColumnIndex(BaseColumns._ID))),
                    values,
                    null,
                    null
            );
            cursor.moveToNext();
        }
    }

    private void scheduleNotification(long endtime, ContentValues values) {
        Intent notificationIntent = new Intent(mContext, NotificationLocal.class);
        notificationIntent.putExtra(NotificationLocal.NOTIFICATION, values);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, (endtime * 1000), pendingIntent);
        Log.d("Jay", "Notification set for " + endtime);
    }
}