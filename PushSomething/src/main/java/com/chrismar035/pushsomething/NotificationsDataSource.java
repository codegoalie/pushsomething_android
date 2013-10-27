package com.chrismar035.pushsomething;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.sql.SQLException;

/**
 * Created by chrismar035 on 10/26/13.
 */
public class NotificationsDataSource {
    private final static String TAG = "PushSomething.NotificationsDataSource";
    private SQLiteDatabase database;
    private NotificationTableHelper dbHelper;
    private String[] allColumns = { NotificationTableHelper.COLUMN_ID,
                                    NotificationTableHelper.COLUMN_TITLE,
                                    NotificationTableHelper.COLUMN_BODY,
                                    NotificationTableHelper.COLUMN_RECEIVED_AT };

    public NotificationsDataSource(Context context) {
        dbHelper = new NotificationTableHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Notification createNotification(String title, String body) {
        return this.createNotification(title, body, System.currentTimeMillis()/1000);
    }

    public Notification createNotification(String title, String body, long receivedAt) {
        ContentValues values = new ContentValues();
        values.put(NotificationTableHelper.COLUMN_TITLE, title);
        values.put(NotificationTableHelper.COLUMN_BODY, body);
        values.put(NotificationTableHelper.COLUMN_RECEIVED_AT, receivedAt);

        long insertId = database.insert(NotificationTableHelper.TABLE_NOTIFICATIONS, null, values);
        Cursor cursor = database.query(NotificationTableHelper.TABLE_NOTIFICATIONS,
                                       allColumns,
                                       NotificationTableHelper.COLUMN_ID + " = " + insertId,
                                       null,
                                       null,
                                       null,
                                       null);

        cursor.moveToFirst();
        Notification newNotification = cursorToNotification(cursor);
        cursor.close();
        return newNotification;
    }

    public void deleteNotification(Notification notification) {
        long id = notification.getId();
        deleteNotificationById(id);
    }

    public void deleteNotification(Cursor cursor) {
        long id = cursor.getLong(0);
        deleteNotificationById(id);
    }

    public Cursor getAllNotifications() {
        Cursor cursor = database.query(NotificationTableHelper.TABLE_NOTIFICATIONS,
                                       allColumns,
                                       null,
                                       null,
                                       null,
                                       null,
                                       NotificationTableHelper.COLUMN_RECEIVED_AT + " DESC");

        cursor.moveToFirst();
        return cursor;
    }

    private void deleteNotificationById(long id) {
        Log.i(TAG, "Notification " + id + " deleted");
        database.delete(NotificationTableHelper.TABLE_NOTIFICATIONS,
                NotificationTableHelper.COLUMN_ID + " = " + id,
                null);
    }

    private Notification cursorToNotification(Cursor cursor) {
        Notification notification = new Notification();
        notification.setId(cursor.getLong(0));
        notification.setTitle(cursor.getString(1));
        notification.setBody(cursor.getString(2));
        notification.setCreatedAt(cursor.getLong(3));
        return notification;
    }
}
