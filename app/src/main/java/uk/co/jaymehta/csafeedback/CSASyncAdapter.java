package uk.co.jaymehta.csafeedback;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
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
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

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

        //Get the authtoken
        String mAuthToken;
        try {
            mAuthToken = mAccountManager.blockingGetAuthToken(account, AccountConstants.AUTH_TOKEN_TYPE, true);

            //Check that authtoken has not already been invalidated e.g. by a previous run of this adapter, if yes then quit
            if(TextUtils.isEmpty(mAuthToken)) { return; }
        }
        catch (OperationCanceledException e) {
            e.printStackTrace();
            Log.d("Jay", e.getMessage());
            return;
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.d("Jay", e.getMessage());
            return;
        }
        catch (AuthenticatorException e) {
            e.printStackTrace();
            Log.d("Jay", e.getMessage());
            return;
        }

        //Is this authtoken for a valid user?
        String url_checkvalid = "http://jkm50.user.srcf.net/feedback/post/index.php";
        ContentValues authtokenvalues = new ContentValues();
        authtokenvalues.put("authtoken", mAuthToken);
        String result;
        try {
            result = PostHelper.postRequest(url_checkvalid, authtokenvalues);
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.d("Jay", e.getMessage());
            return;
        }
        Log.d("Jay", result);

        //Result shows that this user should not have access to this system... How odd... Remove their account, maybe a fresh login will help
        if (result.equals("invalid_user")) {
            Log.d("Jay", "Invalid user");
            mAccountManager.invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, mAuthToken);
            mAccountManager.removeAccountExplicitly(account);
            return;
        }

        //Result shows that user's token has expired (probably hasn't logged in for 3 months), invalidate token and user can re-logon on next app usage
        if (result.equals("expired_token")) {
            Log.d("Jay", "Expired token");
            mAccountManager.invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, mAuthToken);
            return;
        }

        //Server gives any other response than this is a valid user - quit
        if (!result.equals("valid_user")) {
            return;
        }


        //Anything after this line - we assume we have received a valid user response from server

        //Has the syncadapter been told to renew the auth token?
        SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        if(prefs.getBoolean(mContext.getString(R.string.run_renewal_bool), false)) {
            //If yes - renew auth token
            Long tokenRenewed = prefs.getLong(mContext.getString(R.string.time_since_renew), 0);
            Log.d("Jay", tokenRenewed.toString());

            //Has it been more than the number of hours specified in AccountConstants since the last authtoken renewal?
            if (tokenRenewed <= System.currentTimeMillis()) {
                //Yes - get and store new authtoken and invalidate prior one
                String url_renew_token = "http://jkm50.user.srcf.net/feedback/post/index.php/welcome/renew_token";
                try {
                    result = PostHelper.postRequest(url_renew_token, authtokenvalues);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("Jay", e.getMessage());
                    return;
                }

                mAccountManager.invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, mAuthToken);
                mAccountManager.setAuthToken(account, AccountConstants.AUTH_TOKEN_TYPE, result);
                mAuthToken = result;

                //Update datetime after which next renewal can take place
                Long newDate = System.currentTimeMillis() + (AccountConstants.HOURS_BETWEEN_RENEW_TOKEN * 3600 * 1000);
                Log.d("Jay", newDate.toString());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(mContext.getString(R.string.time_since_renew), newDate);
                editor.apply();
            }
        }


        //Get any unsynced items ready to sync up to server
        Log.d("Jay", "Get any unsynced items ready to sync up to server");
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
        Log.d("Jay", "If there are any un-synced items, sync them up to server now");
        if (c.getCount() > 1) {
            Log.d("Jay", "Syncing items up");
            JSONArray data = cur2Json(c);
            c.close();
            Log.d("Jay", data.toString());

            Cursor d = mContentResolver.query(
                    Uri.parse(DatabaseConstants.URL + "feedback"),
                    new String[] { BaseColumns._ID },
                    DatabaseConstants.fd_feedback.COLUMN_NAME_SYNC + "=?",
                    new String[] {"0"},
                    null
            );

            String url_sync_up = "http://jkm50.user.srcf.net/feedback/post/index.php/welcome/sync_up";
            authtokenvalues = new ContentValues();
            authtokenvalues.put("authtoken", mAuthToken);
            authtokenvalues.put("fd_data", data.toString());
            try {
                PostHelper.postRequest(url_sync_up, authtokenvalues);
                setItemsAsSynced(d);
                d.close();
            }
            catch (IOException e) {
                e.printStackTrace();
                Log.d("Jay", e.getMessage());
            }
        }


        //Sync down
        //Get data to store on device
        Log.d("Jay", "Sync down");
        String url_sync_up = "http://jkm50.user.srcf.net/feedback/post/index.php/welcome/sync_down";
        authtokenvalues = new ContentValues();
        authtokenvalues.put("authtoken", mAuthToken);
        try {
            result = PostHelper.postRequest(url_sync_up, authtokenvalues);
            Log.d("Jay", result);
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }

        //Convert string retrieved into JSON array
        JSONArray obj;
        try {
            obj = new JSONArray(result);
            Log.d("Jay", obj.toString(1));
        } catch (Throwable t) {
            t.printStackTrace();
            Log.d("Jay", "Could not parse malformed JSON: \"" + result + "\"");
            return;
        }

        //Convert JSONArray into ContentValues, delete existing entries, and insert each newly downloaded one
        Boolean first = true;
        for (int i = 0, size = obj.length(); i < size; i++)
        {
            ContentValues toinsert = new ContentValues();
            JSONObject objectInArray;
            try {
                objectInArray = obj.getJSONObject(i);
            }
            catch (JSONException e) {
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
                    e.printStackTrace();
                    Log.d("Jay", e.getMessage());
                    return;
                }
            }

            //If first time this has run, delete existing entries
            if (first) {
                Log.d("Jay", "Delete existing entries");
                mContentResolver.delete(
                        Uri.parse(DatabaseConstants.URL + "events"),
                        null,
                        null
                );
            }
            first = false;

            //Insert current row
            Log.d("Jay", "Insert current row");
            mContentResolver.insert(
                    Uri.parse(DatabaseConstants.URL + "events"),
                    toinsert
            );
            toinsert.clear();
        }

        getContext().sendBroadcast(new Intent(mContext, FeedbackActivity.class));
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
                        Log.d("Jay", e.getMessage());
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
}