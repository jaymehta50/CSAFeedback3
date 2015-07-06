package uk.co.jaymehta.csafeedback;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


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

    public static FeedbackActivityFragment newInstance() {
        FeedbackActivityFragment fragment = new FeedbackActivityFragment();
        return fragment;
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
            throw new ClassCastException(activity.toString()
                    + " must implement OnEventSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // For the cursor adapter, specify which columns go into which views
        String[] fromColumns = {DatabaseConstants.fd_events.COLUMN_NAME_NAME, DatabaseConstants.fd_events.COLUMN_NAME_STARTTIME};
        int[] toViews = {android.R.id.text1, android.R.id.text2}; // The TextView in simple_list_item_1

        // Create an empty adapter we will use to display the loaded data.
        // We pass null for the cursor, then update it in onLoadFinished()
        mAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_2, null, fromColumns, toViews, 0);
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
                DatabaseConstants.fd_events.COLUMN_NAME_STARTTIME
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
}
