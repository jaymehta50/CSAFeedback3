package uk.co.jaymehta.csafeedback;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by jkm50 on 03/07/2015.
 */
public class DatabaseProvider extends ContentProvider {

    SQLiteDatabase db;

    static final UriMatcher sUriMatcher;
    static{
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(DatabaseConstants.PROVIDER_NAME, "events", 1);
        sUriMatcher.addURI(DatabaseConstants.PROVIDER_NAME, "events/#", 2);
        sUriMatcher.addURI(DatabaseConstants.PROVIDER_NAME, "feedback", 3);
        sUriMatcher.addURI(DatabaseConstants.PROVIDER_NAME, "feedback/#", 4);
    }

    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper mOpenHelper = new DatabaseHelper(context);

        /**
         * Create a write able database which will trigger its
         * creation if it doesn't already exist.
         */
        db = mOpenHelper.getWritableDatabase();
        return (db != null);
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String tablename;

        switch (sUriMatcher.match(uri)) {
            // If the incoming URI was for all of problem_names
            case 1:
                tablename = DatabaseConstants.fd_events.TABLE_NAME;
                if (TextUtils.isEmpty(sortOrder)) sortOrder = DatabaseConstants.fd_events.COLUMN_NAME_STARTTIME + " DESC";
                break;

            case 2:
                tablename = DatabaseConstants.fd_events.TABLE_NAME;
                selection = BaseColumns._ID + " = ?";
                selectionArgs = new String[] {uri.getLastPathSegment()};
                break;

            // If the incoming URI was for a single row
            case 3:
                tablename = DatabaseConstants.fd_feedback.TABLE_NAME;
                if (TextUtils.isEmpty(sortOrder)) sortOrder = BaseColumns._ID + " ASC";
                break;

            case 4:
                tablename = DatabaseConstants.fd_feedback.TABLE_NAME;
                selection = BaseColumns._ID + " = ?";
                selectionArgs = new String[] {uri.getLastPathSegment()};
                break;

            default:
                throw new IllegalArgumentException("Not a valid URI");
        }

        Cursor c = db.query(
                tablename,  // The table to query
                projection,                               // The columns to return
                selection,              // The columns for the WHERE clause
                selectionArgs,      // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
        return c;
    }

    public String getType(Uri uri) {
        String toreturn;
        switch (sUriMatcher.match(uri)) {
            // If the incoming URI was for all of problem_names
            case 1:
                toreturn = "vnd.android.cursor.dir/vnd." + DatabaseConstants.PROVIDER_NAME + "." + DatabaseConstants.fd_events.TABLE_NAME;
                break;
            case 2:
                toreturn = "vnd.android.cursor.dir/vnd." + DatabaseConstants.PROVIDER_NAME + "." + DatabaseConstants.fd_events.TABLE_NAME;
                break;
            case 3:
                toreturn = "vnd.android.cursor.dir/vnd." + DatabaseConstants.PROVIDER_NAME + "." + DatabaseConstants.fd_feedback.TABLE_NAME;
                break;
            case 4:
                toreturn = "vnd.android.cursor.dir/vnd." + DatabaseConstants.PROVIDER_NAME + "." + DatabaseConstants.fd_feedback.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Not a valid URI");
        }
        return toreturn;
    }

    public Uri insert(Uri uri, ContentValues values) {
        String table;
        switch (sUriMatcher.match(uri)) {
            // If the incoming URI was for all of problem_names
            case 1:
                table = DatabaseConstants.fd_events.TABLE_NAME;
                break;
            case 2:
                table = DatabaseConstants.fd_events.TABLE_NAME;
                break;
            case 3:
                table = DatabaseConstants.fd_feedback.TABLE_NAME;
                break;
            case 4:
                table = DatabaseConstants.fd_feedback.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Not a valid URI");
        }

        long rowID = db.insert(table, "", values);

        if (rowID > 0)
        {
            Uri _uri = ContentUris.withAppendedId(DatabaseConstants.CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String table;
        switch (sUriMatcher.match(uri)) {
            // If the incoming URI was for all of problem_names
            case 1:
                table = DatabaseConstants.fd_events.TABLE_NAME;
                break;
            case 2:
                table = DatabaseConstants.fd_events.TABLE_NAME;
                selection = BaseColumns._ID + " = ?";
                selectionArgs = new String[] {uri.getLastPathSegment()};
                break;
            case 3:
                table = DatabaseConstants.fd_feedback.TABLE_NAME;
                break;
            case 4:
                table = DatabaseConstants.fd_feedback.TABLE_NAME;
                selection = BaseColumns._ID + " = ?";
                selectionArgs = new String[] {uri.getLastPathSegment()};
                break;
            default:
                throw new IllegalArgumentException("Not a valid URI");
        }

        Cursor c = db.query(
                table,  // The table to query
                new String[] { BaseColumns._ID },                               // The columns to return
                selection,              // The columns for the WHERE clause
                selectionArgs,      // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );

        Integer count = c.getCount();
        c.close();

        db.delete(table, selection, selectionArgs);
        return count;
    }

    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        String table;
        switch (sUriMatcher.match(uri)) {
            // If the incoming URI was for all of problem_names
            case 1:
                table = DatabaseConstants.fd_events.TABLE_NAME;
                break;
            case 2:
                table = DatabaseConstants.fd_events.TABLE_NAME;
                selection = BaseColumns._ID + " = ?";
                selectionArgs = new String[] {uri.getLastPathSegment()};
                break;
            case 3:
                table = DatabaseConstants.fd_feedback.TABLE_NAME;
                break;
            case 4:
                table = DatabaseConstants.fd_feedback.TABLE_NAME;
                selection = BaseColumns._ID + " = ?";
                selectionArgs = new String[] {uri.getLastPathSegment()};
                break;
            default:
                throw new IllegalArgumentException("Not a valid URI");
        }

        return db.update(
                table,
                values,
                selection,
                selectionArgs);
    }

}
