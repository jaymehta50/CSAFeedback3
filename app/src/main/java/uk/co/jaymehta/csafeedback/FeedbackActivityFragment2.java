package uk.co.jaymehta.csafeedback;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * A placeholder fragment containing a simple view.
 */
public class FeedbackActivityFragment2 extends Fragment {

    public static Long selection;
    public static Integer score;
    public static Integer cbresp = 1;
    public static String comment;
    public static Boolean commentAdded = false;

    private static final String ARG_PARAM1 = "arg1";
    private SeekBar seekBarScore;
    private TextView textScore;
    private CheckBox checkBoxResponse;
    private EditText editTextComment;

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
        commentAdded = false;
        textScore = (TextView) v.findViewById(R.id.textScore);
        seekBarScore = (SeekBar) v.findViewById(R.id.seekBarScore);
        checkBoxResponse = (CheckBox) v.findViewById(R.id.checkBoxRespond);
        checkBoxResponse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Log.d("Jay", "cbresp to 1");
                    cbresp = 1;
                } else {
                    Log.d("Jay", "cbresp to 0");
                    cbresp = 0;
                }
            }
        });

        seekBarScore.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                score = progressValue;
                textScore.setText(String.valueOf(progressValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        editTextComment = (EditText) v.findViewById(R.id.editTextComment);
        editTextComment.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                comment = editTextComment.getText().toString();
                commentAdded = true;
                Log.d("Jay", commentAdded.toString());
            }
        });

        Button saveButton = (Button) v.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Jay", "Button clicked - comment" + commentAdded.toString());
                ((FeedbackActivity)getActivity()).saveDataButtonClicked(commentAdded, editTextComment.getText().toString());
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
                    new String[] {
                            BaseColumns._ID,
                            DatabaseConstants.fd_feedback.COLUMN_NAME_SCORE,
                            DatabaseConstants.fd_feedback.COLUMN_NAME_NOTIFY_RESP,
                            DatabaseConstants.fd_feedback.COLUMN_NAME_COMMENT
                    },                        // The columns to return for each row
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
                            DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_NAME,
                            DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_TEXT,
                            DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_TIME
                    },                        // The columns to return for each row
                    null,                    // Selection criteria
                    null,                     // Selection criteria
                    null);

            return output;
        }

        protected void onPostExecute(Cursor... result) {
            if (result.length == 2) {
                Cursor sCursor = result[1];
                sCursor.moveToFirst();

                textScore.setText(sCursor.getString(sCursor.getColumnIndex(DatabaseConstants.fd_feedback.COLUMN_NAME_SCORE)));
                seekBarScore.setVisibility(View.GONE);
                checkBoxResponse.setVisibility(View.GONE);

                LinearLayout l = (LinearLayout) getActivity().findViewById(R.id.circle_layout);
                l.setBackgroundResource(R.drawable.score_circle_green);

                editTextComment.setVisibility(View.VISIBLE);

                if(!TextUtils.isEmpty(sCursor.getString(sCursor.getColumnIndex(DatabaseConstants.fd_feedback.COLUMN_NAME_COMMENT))) &&
                        !sCursor.getString(sCursor.getColumnIndex(DatabaseConstants.fd_feedback.COLUMN_NAME_COMMENT)).equals("") &&
                        !sCursor.getString(sCursor.getColumnIndex(DatabaseConstants.fd_feedback.COLUMN_NAME_COMMENT)).equals("null") &&
                        !sCursor.getString(sCursor.getColumnIndex(DatabaseConstants.fd_feedback.COLUMN_NAME_COMMENT)).equals("Null")) {
                    
                    TextView textComment = (TextView) getActivity().findViewById(R.id.textComment);
                    textComment.setText(sCursor.getString(sCursor.getColumnIndex(DatabaseConstants.fd_feedback.COLUMN_NAME_COMMENT)));
                    textComment.setVisibility(View.VISIBLE);
                    
                    editTextComment.setVisibility(View.GONE);

                    Button saveButton = (Button) getActivity().findViewById(R.id.saveButton);
                    saveButton.setVisibility(View.GONE);
                }

                sCursor.close();
            }

            Cursor mCursor = result[0];
            mCursor.moveToFirst();

            SimpleDateFormat sdf = new SimpleDateFormat("h:mma EEE d MMM yyyy", Locale.UK);

            if(!TextUtils.isEmpty(mCursor.getString(mCursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_TEXT))) &&
                    !mCursor.getString(mCursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_TEXT)).equals("") &&
                    !mCursor.getString(mCursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_TEXT)).equals("null") &&
                    !mCursor.getString(mCursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_TEXT)).equals("Null")) {

                LinearLayout respLayout = (LinearLayout) getActivity().findViewById(R.id.linearLayoutResponse);
                respLayout.setVisibility(View.VISIBLE);

                TextView respText = (TextView) getActivity().findViewById(R.id.textResponse);
                respText.setText(mCursor.getString(mCursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_TEXT)));

                TextView respTextFrom = (TextView) getActivity().findViewById(R.id.textResponseFrom);
                respTextFrom.setText("From " +
                    mCursor.getString(mCursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_NAME)) +
                    " at " +
                    sdf.format(mCursor.getLong(mCursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_RESPONSE_TIME)) * 1000)
                );
            }
            
            TextView textTitle = (TextView) getActivity().findViewById(R.id.textTitle);
            textTitle.setText(mCursor.getString(mCursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_NAME)));

            TextView textDesc = (TextView) getActivity().findViewById(R.id.textDesc);
            textDesc.setText(mCursor.getString(mCursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_DESC)));


            TextView textDateTime = (TextView) getActivity().findViewById(R.id.textDateTime);
            textDateTime.setText(sdf.format(mCursor.getLong(mCursor.getColumnIndex(DatabaseConstants.fd_events.COLUMN_NAME_STARTTIME)) * 1000));

            mCursor.close();
        }
    }




}
