package uk.co.jaymehta.csafeedback;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class FeedbackActivity extends Activity implements FeedbackActivityFragment.OnEventSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, FeedbackActivityFragment.newInstance())
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onEventSelected(long id) {
        getFragmentManager().beginTransaction()
                .replace(R.id.container, FeedbackActivityFragment2.newInstance(id))
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    public void saveButtonClicked(View view) {
        ContentValues toinsert = new ContentValues();
        toinsert.put(DatabaseConstants.fd_feedback.COLUMN_NAME_EVENTID, FeedbackActivityFragment2.selection);
        toinsert.put(DatabaseConstants.fd_feedback.COLUMN_NAME_SCORE, FeedbackActivityFragment2.score);
        toinsert.put(DatabaseConstants.fd_feedback.COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());
        toinsert.put(DatabaseConstants.fd_feedback.COLUMN_NAME_NOTIFY_RESP, FeedbackActivityFragment2.cbresp);
        toinsert.put(DatabaseConstants.fd_feedback.COLUMN_NAME_SYNC, 0);
        this.getContentResolver().insert(
                Uri.withAppendedPath(DatabaseConstants.CONTENT_URI, "feedback"),
                toinsert
        );

        getFragmentManager().beginTransaction()
                .replace(R.id.container, FeedbackActivityFragment2.newInstance(FeedbackActivityFragment2.selection))
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }
}
