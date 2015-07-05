package uk.co.jaymehta.csafeedback;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by jkm50 on 03/07/2015.
 */
public final class DatabaseConstants {

    public DatabaseConstants() {
    }

    static final String PROVIDER_NAME = "uk.co.jaymehta.csafeedback.DatabaseProvider";
    static final String URL = "content://" + PROVIDER_NAME + "/";
    static final Uri CONTENT_URI = Uri.parse(URL);

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String NOT_NULL = " NOT NULL";
    private static final String NULL = " NULL";
    private static final String COMMA_SEP = ",";

    public static abstract class fd_events implements BaseColumns {
        public static final String TABLE_NAME = "fd_events";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DESC = "desc";
        public static final String COLUMN_NAME_STARTTIME = "starttime";
        public static final String COLUMN_NAME_ENDTIME = "endtime";
        public static final String COLUMN_NAME_RESP = "responsible_person";
        public static final String COLUMN_NAME_RESPONSE_USER = "response_user";
        public static final String COLUMN_NAME_RESPONSE_NAME = "response_name";
        public static final String COLUMN_NAME_RESPONSE_TEXT = "response_text";
        public static final String COLUMN_NAME_RESPONSE_TIME = "response_time";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_DESC + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_STARTTIME + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_ENDTIME + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_RESP + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_RESPONSE_USER + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_RESPONSE_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_RESPONSE_TEXT + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_RESPONSE_TIME + TEXT_TYPE + NOT_NULL +
                ")";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }


    public static abstract class fd_feedback implements BaseColumns {
        public static final String TABLE_NAME = "fd_feedback";
        public static final String COLUMN_NAME_EVENTID = "event_id";
        public static final String COLUMN_NAME_SCORE = "score";
        public static final String COLUMN_NAME_COMMENT = "comment";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_NOTIFY_RESP = "notify_responses";
        public static final String COLUMN_NAME_SYNC = "sync_status";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_EVENTID + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_SCORE + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_COMMENT + TEXT_TYPE + NULL + COMMA_SEP +
                        COLUMN_NAME_TIMESTAMP + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_NOTIFY_RESP + INTEGER_TYPE + NOT_NULL + COMMA_SEP +
                        COLUMN_NAME_SYNC + INTEGER_TYPE + NOT_NULL +
                        ")";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }




}
