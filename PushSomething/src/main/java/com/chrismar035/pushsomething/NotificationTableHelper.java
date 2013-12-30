package com.chrismar035.pushsomething;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by chrismar035 on 10/26/13.
 */
public class NotificationTableHelper extends SQLiteOpenHelper {
    final static String TAG = "PushSomething:NotificationTableHelper";

    public static final String TABLE_NOTIFICATIONS = "notifications";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SERVER_ID = "server_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_RECEIVED_AT = "received_at";

    private static final String DATABASE_NAME = "notifications.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_NOTIFICATIONS + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_SERVER_ID + " integer not null, "
            + COLUMN_TITLE + " text not null, "
            + COLUMN_BODY + " text not null, "
            + COLUMN_RECEIVED_AT + " integer not null"
            + ");";

    public NotificationTableHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        Log.i(TAG, "Notification table created: " + DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(NotificationTableHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
        Log.i(TAG, "Notifications table dropped.");
        onCreate(db);
    }
}