package uk.co.jaymehta.csafeedback;

import android.app.Activity;
import android.app.Fragment;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;


/**
 * A placeholder fragment containing a simple view.
 */
public class FeedbackActivityFragment2 extends Fragment {

    private static final String ARG_PARAM1 = "arg1";
    private SeekBar seekBarScore;
    private TextView textScore;
    private Long selection;

    public FeedbackActivityFragment2() {
    }

    public static FeedbackActivityFragment2 newInstance(Long id) {
        FeedbackActivityFragment2 fragment = new FeedbackActivityFragment2();
        Bundle args = new Bundle();
        args.putLong(ARG_PARAM1, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selection = getArguments().getLong(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_feedback_2, container, false);
        textScore = (TextView) v.findViewById(R.id.textScore);
        seekBarScore = (SeekBar) v.findViewById(R.id.seekBarScore);

        seekBarScore.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                textScore.setText(String.valueOf(progressValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        new CompleteUI().execute(selection);

    return v;
    }

    private class CompleteUI extends AsyncTask<Long, Void, Cursor[]> {
        protected Cursor[] doInBackground(Long... ids) {
            Cursor[] output;
            Cursor c = getActivity().getContentResolver().query(
                    Uri.withAppendedPath(DatabaseConstants.CONTENT_URI, "feedback"),   // The content URI of the words table
                    new String[] { BaseColumns._ID },                        // The columns to return for each row
                    DatabaseConstants.fd_feedback.COLUMN_NAME_EVENTID + "=?",                    // Selection criteria
                    new String[] { String.valueOf(ids[0]) },                     // Selection criteria
                    null);

            if (c.getCount() != 0) {
                output = new Cursor[2];
                output[1] = c;
            }
            else {
                output = new Cursor[1];
            }

            output[0] = getActivity().getContentResolver().query(
                    Uri.withAppendedPath(DatabaseConstants.CONTENT_URI, "events/" + String.valueOf(ids[0])),   // The content URI of the words table
                    new String[]{
                            BaseColumns._ID,
                            DatabaseConstants.fd_events.COLUMN_NAME_NAME,
                            DatabaseConstants.fd_events.COLUMN_NAME_DESC,
                            DatabaseConstants.fd_events.COLUMN_NAME_STARTTIME,
                    },                        // The columns to return for each row
                    null,                    // Selection criteria
                    null,                     // Selection criteria
                    null);

            return output;
        }

        protected void onPostExecute(Cursor... result) {
            if (result.length == 2) {
                //TODO Code to handle user has already left feedback
            }

            Cursor mCursor = result[0];
            mCursor.moveToFirst();

            TextView textTitle = (TextView) getActivity().findViewById(R.id.textTitle);
            textTitle.setText(mCursor.getString(1));

            TextView textDesc = (TextView) getActivity().findViewById(R.id.textDesc);
            textDesc.setText(mCursor.getString(2));

            TextView textDateTime = (TextView) getActivity().findViewById(R.id.textDateTime);
            textDateTime.setText(new Date(Long.valueOf(mCursor.getString(3)) * 1000).toString());
        }
    }


}
