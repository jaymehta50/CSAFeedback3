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
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class CSASyncAdapter extends AbstractThreadedSyncAdapter {
    // Global variables
    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;

    AccountManager mAccountManager;

    /**
     * Set up the sync adapter
     */
    public CSASyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
        mAccountManager = AccountManager.get(context);
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
        mContentResolver = context.getContentResolver();
        mAccountManager = AccountManager.get(context);
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
                new String[] {"1"},
                null
        );

        //If there are any un-synced items, sync them up to server now
        Log.d("Jay", "If there are any un-synced items, sync them up to server now");
        if (c.getCount() > 1) {
            Log.d("Jay", "Syncing items up");
            JSONArray data = cur2Json(c);
            Log.d("Jay", data.toString());

            String url_sync_up = "http://jkm50.user.srcf.net/feedback/post/index.php/welcome/sync_up";
            ContentValues authtokenvalues = new ContentValues();
            authtokenvalues.put("authtoken", mAuthToken);
            authtokenvalues.put("fd_data", data.toString());
            try {
                PostHelper.postRequest(url_sync_up, authtokenvalues);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }


        //Sync down
        //Get data to store on device
        Log.d("Jay", "Sync down");
        String url_sync_up = "http://jkm50.user.srcf.net/feedback/post/index.php/welcome/sync_down";
        ContentValues authtokenvalues = new ContentValues();
        authtokenvalues.put("authtoken", mAuthToken);
        String result;
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

        cursor.close();
        return resultSet;

    }
}