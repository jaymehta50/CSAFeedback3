package uk.co.jaymehta.csafeedback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

/**
 * Created by Jay on 19/07/2015.
 */
public class NotificationLocal extends BroadcastReceiver {

    public static final String NOTIFICATION_ID = "notification-id";
    public static final String NOTIFICATION = "notification";
    public static final String EVENT_ID_TAG = "event_id";

    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        if(sharedPref.getBoolean(context.getString(R.string.pref_event_notify), true)) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            ContentValues values = intent.getParcelableExtra(NOTIFICATION);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setContentTitle("Leave feedback now:");
            builder.setContentText(values.getAsString("name"));
            builder.setSmallIcon(R.mipmap.ic_csafeedback);

            Intent resultIntent = new Intent(context, FeedbackActivity.class);
            resultIntent.putExtra(NotificationLocal.EVENT_ID_TAG, values.getAsLong(BaseColumns._ID));

            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(resultPendingIntent);

            notificationManager.notify(values.getAsInteger(BaseColumns._ID), builder.build());
        }
    }
}
