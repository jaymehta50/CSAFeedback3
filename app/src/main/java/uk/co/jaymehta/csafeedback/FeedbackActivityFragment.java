package uk.co.jaymehta.csafeedback;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.provider.BaseColumns;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.text.SimpleDateFormat;
import java.util.Locale;


/**
 * A placeholder fragment containing a simple view.
 */
public class FeedbackActivityFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter mAdapter;
    private OnEventSelectedListener mCallback;

    // Container Activity must implement this interface
    public interface OnEventSelectedListener {
        public void onEventSelected(long position);
    }

    //Get and return a new instance of this fragment
    public static FeedbackActivityFragment newInstance() {
        return new FeedbackActivityFragment();
    }

    public FeedbackActivityFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnEventSelectedListener) activity;
        } catch (ClassCastException e) {
            Crashlytics.getInstance().core.logException(e);
            throw new ClassCastException(activity.toString()
                    + " must implement OnEventSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // For the cursor adapter, specify which columns go into which views
        String[] fromColumns = {DatabaseConstants.fd_events.COLUMN_NAME_NAME, DatabaseConstants.fd_events.COLUMN_NAME_STARTTIME};
        int[] toViews = {R.id.list_name, R.id.list_datetime}; // The TextView in simple_list_item_2

        // Create an empty adapter we will use to display the loaded data.
        // We pass null for the cursor, then update it in onLoadFinished()
        mAdapter = new CustomCursorAdapter(getActivity(), R.layout.list_item_feedback, null, fromColumns, toViews, 0);
        setListAdapter(mAdapter);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);

        return inflater.inflate(R.layout.fragment_feedback, container, false);
    }

    // Called when a new Loader needs to be created
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        final String[] PROJECTION = new String[] {
                BaseColumns._ID,
                DatabaseConstants.fd_events.COLUMN_NAME_NAME,
                DatabaseConstants.fd_events.COLUMN_NAME_STARTTIME,
                DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_TEXT
        };
        // This is the select criteria
        final Uri sUri = Uri.withAppendedPath(DatabaseConstants.CONTENT_URI, "events");
        final String selection = DatabaseConstants.fd_events.COLUMN_NAME_STARTTIME + " <= ?";
        final String[] selectionArgs = { String.valueOf(System.currentTimeMillis() / 1000) };

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        CursorLoader c = new CursorLoader(getActivity(), sUri, PROJECTION, selection, selectionArgs, null);
        Log.d("Jay", "Created CursorLoader");
        return c;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        mCallback.onEventSelected(id);
    }

    private class CustomCursorAdapter extends SimpleCursorAdapter {

        public CustomCursorAdapter(Context context, Integer l, Cursor c, String[] from, int[] to, int flags) {
            super(context, l, c, from, to, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            TextView listName=(TextView) view.findViewById(R.id.list_name);
            TextView listDateTime=(TextView) view.findViewById(R.id.list_datetime);
            ImageView ic_done = (ImageView) view.findViewById(R.id.done_image);
            ImageView ic_comment = (ImageView) view.findViewById(R.id.comment_image);
            
            String responseText = cursor.getString(cursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_TEXT));

            SimpleDateFormat sdf = new SimpleDateFormat("h:mma EEE d MMM yy", Locale.UK);

            listName.setText(cursor.getString(cursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_NAME)));
            listDateTime.setText(sdf.format(cursor.getLong(cursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_STARTTIME)) * 1000));

            Cursor c = getActivity().getContentResolver().query(
                    Uri.withAppendedPath(DatabaseConstants.CONTENT_URI, "feedback"),   // The content URI of the words table
                    new String[] {BaseColumns._ID},                        // The columns to return for each row
                    DatabaseConstants.fd_feedback.COLUMN_NAME_EVENTID + "=?",                    // Selection criteria
                    new String[] { cursor.getString(cursor.getColumnIndex(BaseColumns._ID)) },                     // Selection criteria
                    null);

            Log.d("Jay", cursor.getString(cursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_NAME)) + " " + c.getCount());
            if (c.getCount() != 0) {
                ic_done.setVisibility(View.VISIBLE);
            }

            if(!TextUtils.isEmpty(responseText) &&
                    !responseText.equals("") &&
                    !responseText.equals("null") &&
                    !responseText.equals("Null")) {
                ic_comment.setVisibility(View.VISIBLE);
            }
        }

    }
}
